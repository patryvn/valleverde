package es.valledecernego.game;

/**
 * Uses the cube-only humanoid only as an emergency path when the detailed
 * renderer cannot initialize. Drawing it underneath the full model makes the
 * torso and head look like opaque rectangles, even when all detailed parts work.
 * GL_NO_ERROR and submitted draw-call counts are not pixel evidence.
 */
public final class PlayerRenderPolicy {
    public static final float OPAQUE_BASE_ALPHA = 1f;

    private PlayerRenderPolicy() { }

    /**
     * Draw the emergency silhouette only when the primary renderer is unavailable
     * and the independent flat compatibility shader initialized successfully.
     * If the detailed batch later proves incomplete, GameRenderer draws the
     * silhouette after that batch instead of hiding the detailed geometry beneath it.
     */
    public static boolean shouldDrawOpaqueBase(boolean rendererReady, boolean silhouetteShaderReady) {
        return !rendererReady && silhouetteShaderReady;
    }
}
