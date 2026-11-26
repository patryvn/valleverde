package es.valledecernego.game;

/** Deterministic low-poly tree variants and dimensions shared by rendering and tests. */
public final class TreeVisualSpec {
    public static final int BROADLEAF = 0;
    public static final int CONIFER = 1;
    public static final int CONIFER_TIER_COUNT = 4;

    private TreeVisualSpec() { }

    /** Stable [0,1) hash from world coordinates; invalid positions use a safe midpoint. */
    public static float seed(float x, float z) {
        if (!isFinite(x) || !isFinite(z)) return 0.5f;
        double raw = Math.sin((double) x * 12.9898d + (double) z * 78.233d) * 43758.5453d;
        return (float) (raw - Math.floor(raw));
    }

    /** Mix evergreen conifers with Atlantic broadleaf trees without frame-to-frame randomness. */
    public static int species(float x, float z) {
        return seed(x, z) < 0.38f ? CONIFER : BROADLEAF;
    }

    public static float scale(float x, float z) {
        return 0.92f + seed(x + 19.19f, z - 7.71f) * 0.16f;
    }

    public static float crownScale(float x, float z) {
        return 0.92f + seed(x - 3.17f, z + 11.3f) * 0.16f;
    }

    public static float foliageShade(float x, float z) {
        return 0.90f + seed(x + 5.2f, z - 2.8f) * 0.18f;
    }

    /** Four overlapping tiers create a readable tapered conifer silhouette. */
    public static float coniferTierRadius(int tier) {
        return tier < 0 || tier >= CONIFER_TIER_COUNT ? 0f : 0.86f - 0.16f * tier;
    }

    public static float coniferTierHeight(int tier) {
        return tier < 0 || tier >= CONIFER_TIER_COUNT ? 0f : 0.74f - 0.08f * tier;
    }

    public static float coniferTierCenterY(int tier) {
        return tier < 0 || tier >= CONIFER_TIER_COUNT ? 0f : 1.08f + 0.48f * tier;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
