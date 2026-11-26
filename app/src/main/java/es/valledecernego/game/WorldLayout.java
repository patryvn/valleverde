package es.valledecernego.game;

/** Deterministic spawn layout for the first playable rural area. */
public final class WorldLayout {
    public static final float SPAWN_Z = 5f;
    public static final int TREE = 1;
    public static final int ROCK = 2;
    public static final int BUSH = 3;
    public static final int COW = 4;
    public static final int CALF = 5;
    public static final int STABLE = 6;
    public static final int MARKET = 7;
    public static final int VILLAGE_BOARD = 8;
    public static final int HOUSE = 9;

    private static final float[][] INITIAL_OBJECTS = {
        {-10f,16f,TREE,1f},
        {-4f,24f,TREE,1f},
        {5f,23f,TREE,1f},
        {12f,17f,TREE,1f},
        {-14f,32f,TREE,1f},
        {0f,38f,TREE,1f},
        {16f,31f,TREE,1f},
        {-4f,14f,ROCK,1f},
        {8f,27f,ROCK,1f},
        {-12f,26f,ROCK,1f},
        {3f,13f,BUSH,1f},
        {-8f,20f,BUSH,1f},
        {4f,17f,COW,1f},
        {6f,19f,CALF,1f},
        {-9f,21f,STABLE,1f},
        {10f,25f,MARKET,1f},
        {-6f,14f,VILLAGE_BOARD,1f},
        {0f,36f,HOUSE,1f}
    };

    private WorldLayout() { }

    /** Returns a deep copy so a game session cannot mutate the shared spawn template. */
    public static float[][] initialObjects() {
        float[][] copy = new float[INITIAL_OBJECTS.length][];
        for (int i = 0; i < INITIAL_OBJECTS.length; i++) {
            copy[i] = INITIAL_OBJECTS[i].clone();
        }
        return copy;
    }

    public static int countType(int type) {
        int count = 0;
        for (float[] object : INITIAL_OBJECTS) {
            if ((int) object[2] == type) count++;
        }
        return count;
    }
}
