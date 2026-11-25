package es.valledecernego.game;

/** Small, deterministic gait helpers for the low-poly village residents. */
public final class VillagerAnimationSystem {
    private static final float WALK_SWING_RADIANS = 0.48f;
    private static final float IDLE_SWAY_RADIANS = 0.025f;

    private VillagerAnimationSystem() { }

    /** Left and right legs move in opposite directions. */
    public static float legAngle(float phase, boolean walking, boolean leftLeg) {
        float amplitude = walking ? WALK_SWING_RADIANS : IDLE_SWAY_RADIANS;
        float direction = leftLeg ? 1.0f : -1.0f;
        return (float) Math.sin(phase) * amplitude * direction;
    }

    /** Arms counter-swing against the legs, like a natural walking gait. */
    public static float armAngle(float phase, boolean walking, boolean leftArm) {
        return -legAngle(phase, walking, leftArm) * 0.72f;
    }

    /** A small vertical movement keeps the torso from looking rigid. */
    public static float bodyBob(float phase, boolean walking) {
        if (walking) return Math.abs((float) Math.sin(phase)) * 0.025f;
        return (float) Math.sin(phase * 2.0f) * 0.006f;
    }

    /** Centre offset of a limb rotating around its upper pivot. */
    public static float limbCenterYOffset(float halfLength, float angle) {
        return -halfLength * (float) Math.cos(angle);
    }

    /** Local depth offset of a limb rotating around its upper pivot. */
    public static float limbCenterZOffset(float halfLength, float angle) {
        return -halfLength * (float) Math.sin(angle);
    }
}
