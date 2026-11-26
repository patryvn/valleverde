package es.valledecernego.game;

/**
 * Keeps a complete, opaque humanoid compatibility base underneath the detailed
 * player mesh. GL_NO_ERROR and submitted draw-call counts are not pixel evidence.
 */
public final class PlayerRenderPolicy {
    public static final float OPAQUE_BASE_ALPHA = 1f;

    private PlayerRenderPolicy() { }

    public static boolean shouldDrawOpaqueBase(boolean silhouetteShaderReady) {
        return silhouetteShaderReady;
    }
}
