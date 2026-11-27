package es.valledecernego.game;

/**
 * Selects a complete humanoid path instead of trusting shader initialization alone.
 * A driver can accept the program but reject a sphere/capsule draw at runtime.
 */
public final class PlayerRenderPolicy {
    public static final float OPAQUE_BASE_ALPHA = 1f;

    private PlayerRenderPolicy() { }

    /** Draw the emergency silhouette when the detailed renderer is unavailable. */
    public static boolean shouldDrawOpaqueBase(boolean rendererReady, boolean silhouetteShaderReady) {
        return !rendererReady && silhouetteShaderReady;
    }

    /** Use the cube-only model after a runtime geometry failure, or if only the fallback shader exists. */
    public static boolean shouldUseCompatibilityModel(boolean forceCompatibility,
                                                       boolean rendererReady,
                                                       boolean silhouetteShaderReady) {
        return forceCompatibility || shouldDrawOpaqueBase(rendererReady, silhouetteShaderReady);
    }

    /** The detailed mesh batch is attempted only until a runtime failure is detected. */
    public static boolean shouldDrawDetailedModel(boolean rendererReady, boolean forceCompatibility) {
        return rendererReady && !forceCompatibility;
    }

    /**
     * A successful shader link is not proof that every mesh draw reached the GPU.
     * Switch to the cube-only humanoid when a draw reports an error or a part is
     * rejected by the renderer. The caller keeps this decision for the GL context.
     */
    public static boolean shouldSwitchToCompatibility(boolean rendererReady,
                                                        int submittedParts,
                                                        int expectedParts,
                                                        int glError) {
        if (expectedParts <= 0) throw new IllegalArgumentException("expectedParts must be positive");
        return rendererReady && (glError != 0 || submittedParts < expectedParts);
    }
}
