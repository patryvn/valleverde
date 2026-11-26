package es.valledecernego.game;

/**
 * Keeps a complete, opaque humanoid compatibility base underneath the detailed
 * player mesh. GL_NO_ERROR and submitted draw-call counts are not pixel evidence.
 */
public final class PlayerRenderPolicy {
    public static final float OPAQUE_BASE_ALPHA = 1f;

    private PlayerRenderPolicy() { }

    /**
     * Draw the cube-only base whenever either available renderer can draw it.
     * The primary renderer is preferred; the independent flat shader is the
     * last-resort path when the main program could not be created.
     */
    public static boolean shouldDrawOpaqueBase(boolean rendererReady, boolean silhouetteShaderReady) {
        return rendererReady || silhouetteShaderReady;
    }
}
