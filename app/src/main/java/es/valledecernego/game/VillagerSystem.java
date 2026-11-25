package es.valledecernego.game;

/**
 * Three village residents with assignable jobs, skill progression and
 * game-time production. Production is collected at the village notice board.
 */
public final class VillagerSystem {
    public static final int IDLE = 0;
    public static final int FARMER = 1;
    public static final int RANCHER = 2;
    public static final int WOODCUTTER = 3;

    public static final long FARMER_INTERVAL_MS = 90_000L;
    public static final long RANCHER_INTERVAL_MS = 60_000L;
    public static final long WOODCUTTER_INTERVAL_MS = 75_000L;
    public static final int XP_PER_LEVEL = 5;
    public static final int MAX_LEVEL = 20;
    public static final int MAX_PENDING = 99;

    private final WorkerState[] workers = {
        new WorkerState("Lúa"), new WorkerState("Brais"), new WorkerState("Xela")
    };
    private int pendingWood;
    private int pendingForage;
    private int pendingHarvest;

    public synchronized boolean assignJob(int workerIndex, int job) {
        if (workerIndex < 0 || workerIndex >= workers.length || !validJob(job)) return false;
        WorkerState worker = workers[workerIndex];
        if (worker.job != job) {
            worker.job = job;
            worker.workMs = 0L;
        }
        return true;
    }

    public synchronized void update(long deltaMs) {
        if (deltaMs <= 0L) return;
        for (WorkerState worker : workers) {
            if (worker.job == IDLE) continue;
            long interval = intervalFor(worker.job);
            if (Long.MAX_VALUE - worker.workMs < deltaMs) worker.workMs = Long.MAX_VALUE;
            else worker.workMs += deltaMs;

            while (worker.workMs >= interval) {
                int output = 2 + Math.max(0, (worker.level - 1) / 2);
                int accepted = addPending(worker.job, output);
                if (accepted <= 0) {
                    // Keep one completed work cycle ready; never build an
                    // unbounded offline backlog while the storage is full.
                    worker.workMs = interval;
                    break;
                }
                worker.workMs -= interval;
                worker.xp++;
                worker.level = Math.min(MAX_LEVEL, 1 + worker.xp / XP_PER_LEVEL);
            }
        }
    }

    private int addPending(int job, int amount) {
        if (job == FARMER) {
            int accepted = Math.min(amount, MAX_PENDING - pendingHarvest);
            pendingHarvest += accepted;
            return accepted;
        }
        if (job == RANCHER) {
            int accepted = Math.min(amount, MAX_PENDING - pendingForage);
            pendingForage += accepted;
            return accepted;
        }
        if (job == WOODCUTTER) {
            int accepted = Math.min(amount, MAX_PENDING - pendingWood);
            pendingWood += accepted;
            return accepted;
        }
        return 0;
    }

    /**
     * Collects only what fits in the player's inventories. Uncollected goods
     * remain on the board instead of silently disappearing.
     */
    public synchronized Production claimProduction(int woodSpace, int forageSpace, int harvestSpace) {
        int wood = takeUpTo(pendingWood, woodSpace);
        int forage = takeUpTo(pendingForage, forageSpace);
        int harvest = takeUpTo(pendingHarvest, harvestSpace);
        pendingWood -= wood;
        pendingForage -= forage;
        pendingHarvest -= harvest;
        return new Production(wood, forage, harvest);
    }

    private int takeUpTo(int available, int capacity) {
        return Math.min(available, Math.max(0, capacity));
    }

    public synchronized Worker[] snapshot() {
        Worker[] copy = new Worker[workers.length];
        for (int i = 0; i < workers.length; i++) {
            WorkerState w = workers[i];
            copy[i] = new Worker(w.name, w.job, w.level, w.xp, w.workMs, intervalFor(w.job));
        }
        return copy;
    }

    public synchronized int getPendingWood() { return pendingWood; }
    public synchronized int getPendingForage() { return pendingForage; }
    public synchronized int getPendingHarvest() { return pendingHarvest; }
    public synchronized boolean hasPendingProduction() {
        return pendingWood > 0 || pendingForage > 0 || pendingHarvest > 0;
    }

    public synchronized String getPendingSummary() {
        return "Madera " + pendingWood + " · Forraje " + pendingForage + " · Cosecha " + pendingHarvest;
    }

    public synchronized String getSummary() {
        int working = 0;
        for (WorkerState worker : workers) if (worker.job != IDLE) working++;
        return "Aldeanos " + workers.length + " · Trabajando " + working + "/3";
    }

    public synchronized String saveState() {
        StringBuilder out = new StringBuilder();
        out.append("1|").append(pendingWood).append('|')
            .append(pendingForage).append('|').append(pendingHarvest).append('|');
        for (int i = 0; i < workers.length; i++) {
            if (i > 0) out.append(';');
            WorkerState w = workers[i];
            out.append(w.job).append(',').append(w.level).append(',')
                .append(w.xp).append(',').append(w.workMs);
        }
        return out.toString();
    }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] parts = saved.split("\\|", -1);
        if (parts.length != 5 || !"1".equals(parts[0])) return;
        try {
            int loadedWood = Integer.parseInt(parts[1]);
            int loadedForage = Integer.parseInt(parts[2]);
            int loadedHarvest = Integer.parseInt(parts[3]);
            if (loadedWood < 0 || loadedWood > MAX_PENDING
                || loadedForage < 0 || loadedForage > MAX_PENDING
                || loadedHarvest < 0 || loadedHarvest > MAX_PENDING) return;

            String[] savedWorkers = parts[4].split(";", -1);
            if (savedWorkers.length != workers.length) return;
            int[] jobs = new int[workers.length];
            int[] levels = new int[workers.length];
            int[] xpValues = new int[workers.length];
            long[] work = new long[workers.length];
            for (int i = 0; i < workers.length; i++) {
                String[] values = savedWorkers[i].split(",", -1);
                if (values.length != 4) return;
                jobs[i] = Integer.parseInt(values[0]);
                levels[i] = Integer.parseInt(values[1]);
                xpValues[i] = Integer.parseInt(values[2]);
                work[i] = Long.parseLong(values[3]);
                if (!validJob(jobs[i]) || levels[i] < 1 || levels[i] > MAX_LEVEL
                    || xpValues[i] < 0 || xpValues[i] > 100_000
                    || levels[i] != Math.min(MAX_LEVEL, 1 + xpValues[i] / XP_PER_LEVEL)
                    || work[i] < 0 || work[i] > intervalFor(jobs[i])
                    || (jobs[i] == IDLE && work[i] != 0L)) return;
            }

            pendingWood = loadedWood;
            pendingForage = loadedForage;
            pendingHarvest = loadedHarvest;
            for (int i = 0; i < workers.length; i++) {
                workers[i].job = jobs[i];
                workers[i].level = levels[i];
                workers[i].xp = xpValues[i];
                workers[i].workMs = work[i];
            }
        } catch (NumberFormatException ignored) {
            // Invalid save data is ignored without partially changing the village.
        }
    }

    public static String jobName(int job) {
        switch (job) {
            case FARMER: return "Agricultor";
            case RANCHER: return "Ganadero";
            case WOODCUTTER: return "Leñador";
            default: return "Sin trabajo";
        }
    }

    public static long intervalFor(int job) {
        switch (job) {
            case FARMER: return FARMER_INTERVAL_MS;
            case RANCHER: return RANCHER_INTERVAL_MS;
            case WOODCUTTER: return WOODCUTTER_INTERVAL_MS;
            default: return Long.MAX_VALUE;
        }
    }

    private static boolean validJob(int job) {
        return job >= IDLE && job <= WOODCUTTER;
    }

    private static final class WorkerState {
        final String name;
        int job = IDLE;
        int level = 1;
        int xp = 0;
        long workMs = 0L;
        WorkerState(String name) { this.name = name; }
    }

    public static final class Worker {
        private final String name;
        private final int job;
        private final int level;
        private final int xp;
        private final long workMs;
        private final long intervalMs;
        private Worker(String name, int job, int level, int xp, long workMs, long intervalMs) {
            this.name = name; this.job = job; this.level = level; this.xp = xp;
            this.workMs = workMs; this.intervalMs = intervalMs;
        }
        public String name() { return name; }
        public int job() { return job; }
        public String jobName() { return VillagerSystem.jobName(job); }
        public int level() { return level; }
        public int xp() { return xp; }
        public long workMs() { return workMs; }
        public long intervalMs() { return intervalMs; }
    }

    public static final class Production {
        private final int wood;
        private final int forage;
        private final int harvest;
        private Production(int wood, int forage, int harvest) {
            this.wood = wood; this.forage = forage; this.harvest = harvest;
        }
        public int wood() { return wood; }
        public int forage() { return forage; }
        public int harvest() { return harvest; }
        public int total() { return wood + forage + harvest; }
    }
}
