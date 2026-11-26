package es.valledecernego.game;

/** Conservative horizontal frustum and distance test for low-cost world rendering. */
public final class WorldVisibility {
    private static final float HORIZONTAL_TANGENT = 1.12f;
    private static final float NEAR_ALLOWANCE = 1.5f;

    private WorldVisibility() { }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    public static boolean isVisible(float playerX, float playerZ, float yaw,
                                    float objectX, float objectZ,
                                    float radius, float maxDistance) {
        if (radius < 0f || maxDistance <= 0f
                || !isFinite(playerX) || !isFinite(playerZ)
                || !isFinite(yaw) || !isFinite(objectX)
                || !isFinite(objectZ) || !isFinite(radius)
                || !isFinite(maxDistance)) return false;
        float dx = objectX - playerX;
        float dz = objectZ - playerZ;
        float forward = dx * (float)Math.sin(yaw) + dz * (float)Math.cos(yaw);
        float side = dx * (float)Math.cos(yaw) - dz * (float)Math.sin(yaw);
        if (forward + radius < -NEAR_ALLOWANCE) return false;
        if (forward - radius > maxDistance) return false;
        float horizontalLimit = Math.max(0f, forward) * HORIZONTAL_TANGENT
            + radius + NEAR_ALLOWANCE;
        return Math.abs(side) <= horizontalLimit;
    }

    public static float radiusForType(int type) {
        switch (type) {
            case WorldLayout.TREE: return 2.2f;
            case WorldLayout.ROCK:
            case WorldLayout.BUSH: return 1.0f;
            case WorldLayout.COW: return 1.8f;
            case WorldLayout.CALF: return 1.2f;
            case WorldLayout.STABLE:
            case WorldLayout.MARKET: return 3.0f;
            case WorldLayout.VILLAGE_BOARD: return 1.8f;
            case WorldLayout.HOUSE: return 4.2f;
            default: return 1.5f;
        }
    }
}
