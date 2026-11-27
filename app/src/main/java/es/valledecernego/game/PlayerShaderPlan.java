package es.valledecernego.game;

/**
 * Resolves the two GLES program roles without discarding the rounded humanoid
 * shader when it must also be promoted to the primary scene renderer.
 */
public final class PlayerShaderPlan {
    public final boolean rendererReady;
    public final boolean silhouetteReady;
    public final boolean compatibilityRenderer;
    public final boolean promoteSilhouette;

    private PlayerShaderPlan(boolean rendererReady, boolean silhouetteReady,
                             boolean compatibilityRenderer, boolean promoteSilhouette) {
        this.rendererReady = rendererReady;
        this.silhouetteReady = silhouetteReady;
        this.compatibilityRenderer = compatibilityRenderer;
        this.promoteSilhouette = promoteSilhouette;
    }

    public static PlayerShaderPlan resolve(boolean primaryReady,
                                            boolean primaryCompatibility,
                                            boolean silhouetteReady) {
        if (primaryReady) {
            return new PlayerShaderPlan(true, silhouetteReady, primaryCompatibility, false);
        }
        if (silhouetteReady) {
            // The same linked program can serve both roles. Keep silhouetteReady
            // true so the rounded sphere/capsule humanoid remains available.
            return new PlayerShaderPlan(true, true, true, true);
        }
        return new PlayerShaderPlan(false, false, false, false);
    }
}
