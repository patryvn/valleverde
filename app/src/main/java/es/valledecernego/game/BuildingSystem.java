package es.valledecernego.game;

/** First buildable structure in the valley: a usable cattle shelter and hay store. */
public final class BuildingSystem {
    public static final int BUILT = 1;
    public static final int INSUFFICIENT_MATERIALS = 2;
    public static final int ALREADY_BUILT = 3;

    public static final int STABLE_WOOD_COST = 6;
    public static final int STABLE_STONE_COST = 4;
    public static final int MAX_STABLE_HAY = 30;
    public static final long STABLE_FEED_INTERVAL_MS = 60_000L;

    private boolean stableBuilt = false;
    private int storedHay = 0;
    private long feedTimerMs = 0L;

    public synchronized int buildStable(int wood, int stone) {
        if (stableBuilt) return ALREADY_BUILT;
        if (wood < STABLE_WOOD_COST || stone < STABLE_STONE_COST) {
            return INSUFFICIENT_MATERIALS;
        }
        stableBuilt = true;
        return BUILT;
    }

    /** Stores as much loose forage as the stable's hay capacity allows. */
    public synchronized int storeHay(int amount) {
        if (!stableBuilt || amount <= 0) return 0;
        int accepted = Math.min(amount, MAX_STABLE_HAY - storedHay);
        storedHay += accepted;
        return accepted;
    }

    /**
     * Uses one stored hay every game minute to feed the hungriest adult.
     * No hay is consumed when both adults are already adequately fed.
     */
    public synchronized int updateStable(long deltaMs, AnimalSystem animals) {
        if (!stableBuilt || animals == null || deltaMs <= 0L || storedHay <= 0) return 0;
        if (Long.MAX_VALUE - feedTimerMs < deltaMs) feedTimerMs = Long.MAX_VALUE;
        else feedTimerMs += deltaMs;

        int fed = 0;
        while (feedTimerMs >= STABLE_FEED_INTERVAL_MS && storedHay > 0) {
            if (!animals.feedFromStable()) {
                feedTimerMs = 0L;
                break;
            }
            storedHay--;
            feedTimerMs -= STABLE_FEED_INTERVAL_MS;
            fed++;
        }
        if (storedHay == 0) feedTimerMs = 0L;
        return fed;
    }

    public synchronized boolean isStableBuilt() {
        return stableBuilt;
    }

    public synchronized int getStoredHay() {
        return storedHay;
    }

    public synchronized String getSummary() {
        return stableBuilt
            ? "Cuadra construida • Heno " + storedHay + "/" + MAX_STABLE_HAY
            : "Cuadra pendiente";
    }

    public synchronized String saveState() {
        return (stableBuilt ? "1" : "0") + "|" + storedHay + "|" + feedTimerMs;
    }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        if ("1".equals(saved)) {
            stableBuilt = true;
            storedHay = 0;
            feedTimerMs = 0L;
            return;
        }
        if ("0".equals(saved)) {
            stableBuilt = false;
            storedHay = 0;
            feedTimerMs = 0L;
            return;
        }

        String[] parts = saved.split("\\|", -1);
        if (parts.length != 3) return;
        try {
            boolean loadedBuilt;
            if ("1".equals(parts[0])) loadedBuilt = true;
            else if ("0".equals(parts[0])) loadedBuilt = false;
            else return;

            int loadedHay = Integer.parseInt(parts[1]);
            long loadedTimer = Long.parseLong(parts[2]);
            if (loadedHay < 0 || loadedHay > MAX_STABLE_HAY
                || loadedTimer < 0 || loadedTimer >= STABLE_FEED_INTERVAL_MS
                || (!loadedBuilt && (loadedHay != 0 || loadedTimer != 0L))) return;

            stableBuilt = loadedBuilt;
            storedHay = loadedHay;
            feedTimerMs = loadedTimer;
        } catch (NumberFormatException ignored) {
            // Ignore malformed saves and keep the current building state.
        }
    }
}
