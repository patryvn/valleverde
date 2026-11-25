package es.valledecernego.game;

/** First buildable structure in the valley: a small cattle shelter. */
public final class BuildingSystem {
    public static final int BUILT = 1;
    public static final int INSUFFICIENT_MATERIALS = 2;
    public static final int ALREADY_BUILT = 3;

    public static final int STABLE_WOOD_COST = 6;
    public static final int STABLE_STONE_COST = 4;

    private boolean stableBuilt = false;

    public synchronized int buildStable(int wood, int stone) {
        if (stableBuilt) return ALREADY_BUILT;
        if (wood < STABLE_WOOD_COST || stone < STABLE_STONE_COST) {
            return INSUFFICIENT_MATERIALS;
        }
        stableBuilt = true;
        return BUILT;
    }

    public synchronized boolean isStableBuilt() {
        return stableBuilt;
    }

    public synchronized String getSummary() {
        return stableBuilt ? "Cuadra construida" : "Cuadra pendiente";
    }

    public synchronized String saveState() {
        return stableBuilt ? "1" : "0";
    }

    public synchronized void loadState(String saved) {
        if ("1".equals(saved)) stableBuilt = true;
        else if ("0".equals(saved)) stableBuilt = false;
    }
}
