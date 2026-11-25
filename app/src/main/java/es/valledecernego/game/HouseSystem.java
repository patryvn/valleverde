package es.valledecernego.game;

/** First personal home on the player's plot. */
public final class HouseSystem {
    public static final int BUILT = 1;
    public static final int INSUFFICIENT_MATERIALS = 2;
    public static final int WOOD_COST = 10;
    public static final int STONE_COST = 6;

    private boolean built;

    public synchronized int build(int wood, int stone) {
        if (built) return BUILT;
        if (wood < WOOD_COST || stone < STONE_COST) return INSUFFICIENT_MATERIALS;
        built = true;
        return BUILT;
    }

    public synchronized boolean isBuilt() { return built; }

    public synchronized String saveState() { return built ? "1" : "0"; }

    public synchronized void loadState(String state) {
        if ("1".equals(state)) built = true;
        else if ("0".equals(state)) built = false;
    }
}
