package es.valledecernego.game;

/**
 * Immutable render result published once per completed player draw batch.
 * The HUD reads one volatile reference instead of observing counters while
 * the GL thread is resetting and rebuilding them for the next frame.
 */
public final class PlayerRenderSnapshot {
    public final boolean rendererReady;
    public final boolean silhouetteReady;
    public final boolean compatibilityRenderer;
    public final int detailedParts;
    public final int fallbackParts;
    public final int glError;
    public final boolean frameRendered;

    public PlayerRenderSnapshot(boolean rendererReady, boolean silhouetteReady,
                                boolean compatibilityRenderer, int detailedParts,
                                int fallbackParts, int glError, boolean frameRendered) {
        this.rendererReady = rendererReady;
        this.silhouetteReady = silhouetteReady;
        this.compatibilityRenderer = compatibilityRenderer;
        this.detailedParts = Math.max(0, detailedParts);
        this.fallbackParts = Math.max(0, fallbackParts);
        this.glError = glError;
        this.frameRendered = frameRendered;
    }

    public static PlayerRenderSnapshot initial() {
        return new PlayerRenderSnapshot(false, false, false, 0, 0, 0, false);
    }

    public String summary(int expectedDetailedParts, int expectedFallbackParts) {
        if (expectedDetailedParts <= 0 || expectedFallbackParts <= 0) {
            throw new IllegalArgumentException("Expected part counts must be positive");
        }
        if (!rendererReady && !silhouetteReady) return "3D NO DISPONIBLE";
        if (!frameRendered) return "3D OK · esperando primer fotograma";
        if (glError != 0) {
            int reported = rendererReady ? detailedParts : fallbackParts;
            int expected = rendererReady ? expectedDetailedParts : expectedFallbackParts;
            return "3D ERROR GL 0x" + Integer.toHexString(glError)
                + " · " + reported + "/" + expected;
        }
        if (compatibilityRenderer && rendererReady && detailedParts >= expectedDetailedParts) {
            return "3D COMPATIBLE · " + detailedParts + " piezas · shader GLES plano · GL OK";
        }
        if (rendererReady && detailedParts >= expectedDetailedParts) {
            return "3D OK · " + detailedParts + " piezas · respaldo humanoide "
                + fallbackParts + " · GL OK";
        }
        if (silhouetteReady && fallbackParts >= expectedFallbackParts) {
            return "3D COMPATIBLE · " + fallbackParts + " piezas humanoides · GL OK";
        }
        if (silhouetteReady) {
            return "3D INCOMPLETO · " + fallbackParts + "/" + expectedFallbackParts + " piezas";
        }
        return "3D INCOMPLETO · " + detailedParts + "/" + expectedDetailedParts + " piezas";
    }
}
