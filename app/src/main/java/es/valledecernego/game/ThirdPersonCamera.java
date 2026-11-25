package es.valledecernego.game;

/** Third-person camera orbit kept behind the direction the player is facing. */
public final class ThirdPersonCamera {
    private static final float DISTANCE = 4.2f;
    private static final float TARGET_HEIGHT = 1.1f;
    private static final float BASE_ELEVATION = 0.28f;

    private ThirdPersonCamera() { }

    public static Pose follow(float playerX, float playerZ, float playerY,
                              float yaw, float pitch) {
        float elevation = BASE_ELEVATION + pitch;
        float horizontal = DISTANCE * (float) Math.cos(elevation);
        float targetY = TARGET_HEIGHT + playerY;
        float cameraX = playerX + (float) Math.sin(yaw) * horizontal;
        float cameraY = targetY + DISTANCE * (float) Math.sin(elevation);
        float cameraZ = playerZ - (float) Math.cos(yaw) * horizontal;
        return new Pose(cameraX, cameraY, cameraZ, targetY);
    }

    public static final class Pose {
        private final float x;
        private final float y;
        private final float z;
        private final float targetY;

        Pose(float x, float y, float z, float targetY) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetY = targetY;
        }

        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
        public float targetY() { return targetY; }
    }
}
