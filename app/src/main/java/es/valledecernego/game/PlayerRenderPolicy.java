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

    /** Toggle between the detailed 60-part model and the cube-only compatibility humanoid. */
    public static boolean toggleCompatibilityMode(boolean current) {
        return !current;
    }

    /** Hit-test for the small 3D/ALT button at the upper-right of the HUD. */
    public static boolean isRenderModeButtonHit(float x, float y, float width, float height) {
        if (!(width > 0f) || !(height > 0f)
                || !Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height)) return false;
        float unit = Math.min(height, width * .55f);
        float centerX = width - unit * .10f;
        float centerY = unit * .25f;
        float radius = unit * .07f;
        float dx = x - centerX;
        float dy = y - centerY;
        return dx * dx + dy * dy <= radius * radius;
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
