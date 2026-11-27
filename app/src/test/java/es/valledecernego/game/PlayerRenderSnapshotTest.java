package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerRenderSnapshotTest {
    private static final int DETAILED_PARTS = 60;
    private static final int FALLBACK_PARTS = 11;

    @Test public void initialSnapshotReportsRendererUnavailable() {
        assertEquals("3D NO DISPONIBLE",
            PlayerRenderSnapshot.initial().summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void readyRendererWaitsUntilACompleteFrameIsPublished() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, true, false, 0, 0, 0, false);
        assertEquals("3D OK · esperando primer fotograma",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void detailedModelReportsTheRoundedModelWithoutDrawingTheBoxFallback() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, true, false, DETAILED_PARTS, 0, 0, true);
        assertEquals("3D OK · 60 piezas · modelo detallado · GL OK",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void detailedModelCanStillReportFallbackWhenItWasActuallyNeeded() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, true, false, DETAILED_PARTS, FALLBACK_PARTS, 0, true, true);
        assertEquals("3D OK · 60 piezas · humanoide redondeado 11 · GL OK",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void plainGlesRendererIsReportedAsCompatible() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, false, true, DETAILED_PARTS, 0, 0, true);
        assertEquals("3D COMPATIBLE · 60 piezas · shader GLES plano · GL OK",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void simpleShaderCompatibilityCanReportAllSixtyDetailedParts() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, true, true, DETAILED_PARTS, DETAILED_PARTS, 0, true, false);
        assertEquals("3D COMPATIBLE · 60 piezas · shader GLES plano · GL OK",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void independentHumanoidFallbackReportsItsElevenParts() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            false, true, false, 0, FALLBACK_PARTS, 0, true, true);
        assertEquals("3D COMPATIBLE · 11 piezas · humanoide redondeado · GL OK",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void runtimeGeometryFallbackReportsTheVisibleElevenPartModelNotAttemptedDrawCalls() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, true, false, 0, FALLBACK_PARTS, 0, true, false);
        assertEquals("3D COMPATIBLE · 11 piezas · respaldo cúbico · GL OK",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void glErrorIsReportedBeforeACompletePartCount() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, true, false, DETAILED_PARTS, FALLBACK_PARTS, 0x0502, true);
        assertEquals("3D ERROR GL 0x502 · 60/60",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void incompleteDetailedBatchReportsTheActualCount() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            true, false, false, 59, 0, 0, true);
        assertEquals("3D INCOMPLETO · 59/60 piezas",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void incompleteHumanoidFallbackReportsItsOwnExpectedCount() {
        PlayerRenderSnapshot snapshot = new PlayerRenderSnapshot(
            false, true, false, 0, 8, 0, true);
        assertEquals("3D INCOMPLETO · 8/11 piezas",
            snapshot.summary(DETAILED_PARTS, FALLBACK_PARTS));
    }

    @Test public void expectedPartCountsMustBePositive() {
        try {
            PlayerRenderSnapshot.initial().summary(0, FALLBACK_PARTS);
            fail("zero expected parts should be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Expected part counts must be positive", expected.getMessage());
        }
    }
}
