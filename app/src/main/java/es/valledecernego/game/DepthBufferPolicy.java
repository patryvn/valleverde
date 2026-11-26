package es.valledecernego.game;

/** EGL depth-buffer preference for stable overlap between the player's small 3D parts. */
public final class DepthBufferPolicy {
    private static final int[] PREFERRED_DEPTH_BITS = {24, 16};

    private DepthBufferPolicy() { }

    /** Prefer 24-bit depth, while retaining a 16-bit fallback for older mobile GPUs. */
    public static int[] preferredDepthBits() {
        return PREFERRED_DEPTH_BITS.clone();
    }
}
