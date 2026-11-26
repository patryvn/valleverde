package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerRenderPolicyTest {
    @Test public void primaryRendererKeepsOpaqueBaseWhenSeparateFallbackShaderIsUnavailable() {
        assertTrue(PlayerRenderPolicy.shouldDrawOpaqueBase(true, false));
        assertEquals(1f, PlayerRenderPolicy.OPAQUE_BASE_ALPHA, 0f);
    }

    @Test public void flatCompatibilityShaderCanDrawBaseWhenPrimaryRendererIsUnavailable() {
        assertTrue(PlayerRenderPolicy.shouldDrawOpaqueBase(false, true));
    }

    @Test public void noAvailableRendererDoesNotAttemptToDrawBase() {
        assertFalse(PlayerRenderPolicy.shouldDrawOpaqueBase(false, false));
    }

    @Test public void detailedDrawSubmissionDoesNotReplaceTheCompatibilityBase() {
        // Even a complete 60-call batch with GL_NO_ERROR cannot prove that a GPU
        // actually put all those pixels on screen. The cube-only base stays on.
        assertTrue(PlayerRenderPolicy.shouldDrawOpaqueBase(true, false));
    }
}
