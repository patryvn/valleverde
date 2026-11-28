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

        // Feet rotate around the same hip pivot as their leg. Treating boots
        // as stationary parts made the legs swing underneath fixed footwear.
        if (part.motion == PlayerModelSpec.Motion.LEFT_FOOT
                || part.motion == PlayerModelSpec.Motion.RIGHT_FOOT) {
            float dy = part.y - PlayerModelSpec.LEG_PIVOT_Y;
            float dz = part.z;
            float cosine = (float) Math.cos(motionAngle);
            float sine = (float) Math.sin(motionAngle);
            localY = PlayerModelSpec.LEG_PIVOT_Y + dy * cosine - dz * sine;
            localZ = dy * sine + dz * cosine;
            localPitch = motionAngle;
        } else if (part.motion != PlayerModelSpec.Motion.NONE) {
            // Motion is independent of mesh shape: the compatibility model uses
            // boxes for limbs but must keep the same upper-pivot walk animation.
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
            part.sx, part.sy, part.sz, facing, localPitch, part.roll);
    }

    public static final class Pose {
        public final float x, y, z;
        public final float sx, sy, sz;
        public final float yaw, pitch, roll;

        private Pose(float x, float y, float z, float sx, float sy, float sz,
                     float yaw, float pitch, float roll) {
            this.x = x; this.y = y; this.z = z;
            this.sx = sx; this.sy = sy; this.sz = sz;
            this.yaw = yaw; this.pitch = pitch; this.roll = roll;
        }
    }
}
