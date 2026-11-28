package es.valledecernego.game;

/**
 * Throttles the runtime visual watchdog while ensuring that a successful
 * startup check is not treated as proof that the avatar remains visible after
 * walking or turning on a mobile GPU.
 */
public final class PlayerVisualProbePolicy {
    public static final float POSITION_REPROBE_METERS = 0.65f;
    public static final float FACING_REPROBE_RADIANS = 0.35f;
    public static final long MOVING_REPROBE_INTERVAL_NANOS = 2_000_000_000L;

    private PlayerVisualProbePolicy() { }

    public static boolean shouldReprobe(boolean available, boolean moving,
                                        float x, float z, float facing, long nowNanos,
                                        float lastX, float lastZ, float lastFacing,
                                        long lastProbeNanos) {
        if (!available) return false;
        if (!Float.isFinite(x) || !Float.isFinite(z) || !Float.isFinite(facing)
                || !Float.isFinite(lastX) || !Float.isFinite(lastZ)
                || !Float.isFinite(lastFacing)) return true;

        double distance = Math.hypot(x - lastX, z - lastZ);
        if (distance >= POSITION_REPROBE_METERS) return true;

        float delta = facing - lastFacing;
        float wrappedDelta = (float) Math.atan2(Math.sin(delta), Math.cos(delta));
        if (Math.abs(wrappedDelta) >= FACING_REPROBE_RADIANS) return true;

        return moving && lastProbeNanos > 0L && nowNanos >= lastProbeNanos
                && nowNanos - lastProbeNanos >= MOVING_REPROBE_INTERVAL_NANOS;
    }
}
