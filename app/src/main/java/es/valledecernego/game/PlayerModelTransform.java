package es.valledecernego.game;

/**
 * Pure world-space transform used by the OpenGL player renderer.
 * Keeping this math outside GLES lets unit tests check every animated part.
 */
public final class PlayerModelTransform {
    private PlayerModelTransform() { }

    public static Pose partPose(float playerX, float playerY, float playerZ,
                                float facing, float bodyBob,
                                PlayerModelSpec.Part part, float motionAngle) {
        float localY = part.y + (part.followsBody ? bodyBob : 0f);
        float localZ = part.z;
        float localPitch = 0f;

        if (part.shape == PlayerModelSpec.Shape.LIMB) {
            float halfLength = part.sy * 0.5f;
            localY += VillagerAnimationSystem.limbCenterYOffset(halfLength, motionAngle);
            localZ += VillagerAnimationSystem.limbCenterZOffset(halfLength, motionAngle);
            localPitch = motionAngle;
        }

        float cosine = (float) Math.cos(facing);
        float sine = (float) Math.sin(facing);
        float worldX = playerX + part.x * cosine + localZ * sine;
        float worldZ = playerZ - part.x * sine + localZ * cosine;
        return new Pose(worldX, playerY + localY, worldZ,
            part.sx, part.sy, part.sz, facing, localPitch);
    }

    public static final class Pose {
        public final float x, y, z;
        public final float sx, sy, sz;
        public final float yaw, pitch;

        private Pose(float x, float y, float z, float sx, float sy, float sz,
                     float yaw, float pitch) {
            this.x = x; this.y = y; this.z = z;
            this.sx = sx; this.sy = sy; this.sz = sz;
            this.yaw = yaw; this.pitch = pitch;
        }
    }
}
