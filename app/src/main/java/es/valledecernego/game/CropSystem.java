package es.valledecernego.game;

import java.util.ArrayList;

/**
 * Small deterministic farming loop used by the first Android world.
 * Growth advances only while the app is running; current progress is saved
 * so leaving the game does not reset a planted field.
 */
public final class CropSystem {
    public static final int EMPTY = 0;
    public static final int PLANTED = 1;
    public static final int GROWING = 2;
    public static final int READY = 3;

    public static final int NO_PLOT = 0;
    public static final int PLANTED_RESULT = 1;
    public static final int WATERED = 2;
    public static final int GROWING_RESULT = 3;
    public static final int HARVESTED = 4;
    public static final int OUT_OF_SEEDS = 5;

    public static final long GROWTH_DURATION_MS = 90_000L;
    public static final int MAX_HARVEST = 999;
    private static final float INTERACTION_DISTANCE = 2.4f;

    private final ArrayList<Plot> plots = new ArrayList<>();
    private static final int MAX_SEEDS = 99;
    private int seeds = 6;
    private int harvest = 0;

    public CropSystem() {
        plots.add(new Plot(-4.0f, 1.0f));
        plots.add(new Plot(-1.6f, 1.0f));
        plots.add(new Plot(0.8f, 1.0f));
        plots.add(new Plot(3.2f, 1.0f));
    }

    public synchronized int interact(float x, float z) {
        Plot plot = nearest(x, z);
        if (plot == null || distance(plot, x, z) > INTERACTION_DISTANCE) return NO_PLOT;

        if (plot.stage == EMPTY) {
            if (seeds <= 0) return OUT_OF_SEEDS;
            seeds--;
            plot.stage = PLANTED;
            plot.growthMs = 0;
            plot.watered = false;
            return PLANTED_RESULT;
        }
        if (plot.stage == PLANTED) {
            plot.watered = true;
            plot.stage = GROWING;
            return WATERED;
        }
        if (plot.stage == GROWING) return GROWING_RESULT;

        harvest = Math.min(MAX_HARVEST, harvest + 3);
        seeds++;
        plot.stage = EMPTY;
        plot.growthMs = 0;
        plot.watered = false;
        return HARVESTED;
    }

    public synchronized void update(long deltaMs) {
        update(deltaMs, SeasonSystem.SPRING, SeasonSystem.CLOUDY);
    }

    /** Rain waters planted plots; seasonal temperature changes crop growth speed. */
    public synchronized void update(long deltaMs, int season, int weather) {
        if (deltaMs <= 0) return;
        float growthRate = season == SeasonSystem.WINTER ? 0.45f
            : season == SeasonSystem.SUMMER ? 1.15f
            : season == SeasonSystem.AUTUMN ? 0.90f : 1.0f;
        if (weather == SeasonSystem.RAIN) {
            growthRate *= 1.5f;
            for (Plot plot : plots) {
                if (plot.stage == PLANTED) {
                    plot.watered = true;
                    plot.stage = GROWING;
                }
            }
        }
        long effectiveDelta = Math.max(1L, Math.round(deltaMs * growthRate));
        for (Plot plot : plots) {
            if (plot.stage != GROWING || !plot.watered) continue;
            plot.growthMs = Math.min(GROWTH_DURATION_MS, plot.growthMs + effectiveDelta);
            if (plot.growthMs >= GROWTH_DURATION_MS) plot.stage = READY;
        }
    }

    public synchronized float nearestDistance(float x, float z) {
        Plot plot = nearest(x, z);
        return plot == null ? Float.MAX_VALUE : distance(plot, x, z);
    }

    public synchronized int getSeeds() { return seeds; }
    public synchronized int getHarvest() { return harvest; }

    /** Adds seeds up to the inventory cap and returns the amount actually added. */
    public synchronized int addSeeds(int amount) {
        if (amount <= 0) return 0;
        int added = Math.min(amount, MAX_SEEDS - seeds);
        seeds += added;
        return added;
    }

    /** Adds delivered produce up to the inventory limit and returns the accepted amount. */
    public synchronized int addHarvest(int amount) {
        if (amount <= 0) return 0;
        int added = Math.min(amount, MAX_HARVEST - harvest);
        harvest += added;
        return added;
    }

    /** Removes and returns all stored produce for a market transaction. */
    public synchronized int takeAllHarvest() {
        int sold = harvest;
        harvest = 0;
        return sold;
    }

    public synchronized int getReadyCount() {
        int count = 0;
        for (Plot plot : plots) if (plot.stage == READY) count++;
        return count;
    }

    public synchronized Plot[] snapshot() {
        Plot[] copy = new Plot[plots.size()];
        for (int i = 0; i < plots.size(); i++) copy[i] = new Plot(plots.get(i));
        return copy;
    }

    synchronized Plot plotForTest(int index) {
        return new Plot(plots.get(index));
    }

    public synchronized String saveState() {
        StringBuilder out = new StringBuilder();
        out.append(seeds).append('|').append(harvest);
        for (Plot plot : plots) {
            out.append('|').append(plot.stage).append(',')
                .append(plot.growthMs).append(',')
                .append(plot.watered ? 1 : 0);
        }
        return out.toString();
    }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] parts = saved.split("\\|");
        if (parts.length != plots.size() + 2) return;
        try {
            int loadedSeeds = Math.max(0, Integer.parseInt(parts[0]));
            int loadedHarvest = Math.max(0, Math.min(MAX_HARVEST, Integer.parseInt(parts[1])));
            int[] stages = new int[plots.size()];
            long[] growth = new long[plots.size()];
            boolean[] watered = new boolean[plots.size()];
            for (int i = 0; i < plots.size(); i++) {
                String[] state = parts[i + 2].split(",");
                if (state.length != 3) return;
                stages[i] = Integer.parseInt(state[0]);
                growth[i] = Long.parseLong(state[1]);
                watered[i] = "1".equals(state[2]);
                if (stages[i] < EMPTY || stages[i] > READY || growth[i] < 0) return;
                if (stages[i] == READY) growth[i] = GROWTH_DURATION_MS;
                if (stages[i] == EMPTY) { growth[i] = 0; watered[i] = false; }
            }
            seeds = loadedSeeds;
            harvest = loadedHarvest;
            for (int i = 0; i < plots.size(); i++) {
                Plot plot = plots.get(i);
                plot.stage = stages[i];
                plot.growthMs = Math.min(GROWTH_DURATION_MS, growth[i]);
                plot.watered = watered[i];
            }
        } catch (NumberFormatException ignored) {
            // Invalid save data is ignored; the default farm remains playable.
        }
    }

    private Plot nearest(float x, float z) {
        Plot best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Plot plot : plots) {
            float d = distance(plot, x, z);
            if (d < bestDistance) { bestDistance = d; best = plot; }
        }
        return best;
    }

    private float distance(Plot plot, float x, float z) {
        return (float)Math.hypot(x - plot.x, z - plot.z);
    }

    public static final class Plot {
        private final float x;
        private final float z;
        private int stage;
        private long growthMs;
        private boolean watered;

        private Plot(float x, float z) {
            this.x = x; this.z = z;
        }

        private Plot(Plot other) {
            this.x = other.x; this.z = other.z;
            this.stage = other.stage; this.growthMs = other.growthMs;
            this.watered = other.watered;
        }

        public float x() { return x; }
        public float z() { return z; }
        public int stage() { return stage; }
        public long growthMs() { return growthMs; }
        public boolean watered() { return watered; }
    }
}
