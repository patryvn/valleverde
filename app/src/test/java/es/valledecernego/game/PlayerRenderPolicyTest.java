package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerRenderPolicyTest {
    @Test public void opaqueHumanoidBaseIsKeptWhenCompatibilityShaderIsReady() {
        assertTrue(PlayerRenderPolicy.shouldDrawOpaqueBase(true));
        assertEquals(1f, PlayerRenderPolicy.OPAQUE_BASE_ALPHA, 0f);
    }

    @Test public void detailedDrawSubmissionDoesNotReplaceTheCompatibilityBase() {
        // Even a complete 60-call batch with GL_NO_ERROR cannot prove that a GPU
        // actually put all those pixels on screen. The compatibility base stays on.
        assertTrue(PlayerRenderPolicy.shouldDrawOpaqueBase(true));
    }

    @Test public void unavailableCompatibilityShaderIsNotCalled() {
        assertFalse(PlayerRenderPolicy.shouldDrawOpaqueBase(false));
    }
}
