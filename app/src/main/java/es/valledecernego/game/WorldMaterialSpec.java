package es.valledecernego.game;

/** Shared material IDs and deterministic CPU reference for the procedural GLES world shaders. */
public final class WorldMaterialSpec {
    public static final float SOLID = 0f;
    public static final float TERRAIN = 6f;
    public static final float SKY = 7f;
    public static final float PATCH_SCALE = 0.42f;
    public static final float DETAIL_SCALE = 3.7f;

    private WorldMaterialSpec() { }

    /** Smooth value noise in [0,1], matching the GLSL terrain shader's lattice interpolation. */
    public static float noise(float x, float z, float scale) {
        if (!isFinite(x) || !isFinite(z) || !isFinite(scale) || scale <= 0f) return 0.5f;
        double px = (double) x * scale;
        double pz = (double) z * scale;
        double ix = Math.floor(px), iz = Math.floor(pz);
        float fx = smooth((float) (px - ix));
        float fz = smooth((float) (pz - iz));
        float a = hash(ix, iz);
        float b = hash(ix + 1d, iz);
        float c = hash(ix, iz + 1d);
        float d = hash(ix + 1d, iz + 1d);
        return clamp(mix(mix(a, b, fx), mix(c, d, fx), fz), 0f, 1f);
    }

    /** Expected brightness multiplier for the two terrain-noise octaves. */
    public static float terrainBrightness(float x, float z) {
        if (!isFinite(x) || !isFinite(z)) return 0.91f;
        float detail = 0.5f + 0.5f * (float) Math.sin(x * DETAIL_SCALE + Math.sin(z * 3.1f));
        return 0.80f + 0.16f * noise(x, z, PATCH_SCALE) + 0.06f * detail;
    }

    /** Monotonic vertical sky gradient; -0.28 is horizon and 0.68 is zenith. */
    public static float skyGradient(float localY) {
        if (!isFinite(localY)) return 0f;
        return clamp((localY + 0.28f) / 0.96f, 0f, 1f);
    }

    private static float hash(double x, double z) {
        double raw = Math.sin(x * 127.1d + z * 311.7d) * 43758.5453d;
        return (float) (raw - Math.floor(raw));
    }

    private static float smooth(float value) {
        return value * value * (3f - 2f * value);
    }

    private static float mix(float a, float b, float amount) {
        return a + (b - a) * amount;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
