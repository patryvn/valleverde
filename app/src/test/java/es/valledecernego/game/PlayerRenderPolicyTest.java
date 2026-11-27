package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerRenderPolicyTest {
    @Test public void detailedRendererDoesNotDrawOpaqueCubeUnderlay() {
        assertFalse(PlayerRenderPolicy.shouldDrawOpaqueBase(true, false));
        assertFalse(PlayerRenderPolicy.shouldDrawOpaqueBase(true, true));
        assertEquals(1f, PlayerRenderPolicy.OPAQUE_BASE_ALPHA, 0f);
    }

    @Test public void flatCompatibilityShaderIsUsedOnlyWhenPrimaryRendererIsUnavailable() {
        assertTrue(PlayerRenderPolicy.shouldDrawOpaqueBase(false, true));
    }

    @Test public void noAvailableRendererDoesNotAttemptToDrawBase() {
        assertFalse(PlayerRenderPolicy.shouldDrawOpaqueBase(false, false));
    }

    @Test public void completeDetailedBatchStaysOnTheDetailedPath() {
        assertTrue(PlayerRenderPolicy.shouldDrawDetailedModel(true, false));
        assertFalse(PlayerRenderPolicy.shouldSwitchToCompatibility(true, 60, 60, 0));
    }

    @Test public void runtimeGlErrorSwitchesToCompatibilityEvenWhenDrawCallsWereSubmitted() {
        assertTrue(PlayerRenderPolicy.shouldSwitchToCompatibility(true, 60, 60, 0x0502));
    }

    @Test public void incompleteDetailedBatchSwitchesToCompatibility() {
        assertTrue(PlayerRenderPolicy.shouldSwitchToCompatibility(true, 17, 60, 0));
    }

    @Test public void failedPrimaryRendererUsesTheIndependentFallbackShader() {
        assertTrue(PlayerRenderPolicy.shouldUseCompatibilityModel(false, false, true));
        assertFalse(PlayerRenderPolicy.shouldDrawDetailedModel(false, false));
    }

    @Test public void runtimeFailureKeepsCompatibilityModeUntilContextIsRecreated() {
        assertTrue(PlayerRenderPolicy.shouldUseCompatibilityModel(true, true, true));
        assertFalse(PlayerRenderPolicy.shouldDrawDetailedModel(true, true));
    }

    @Test public void invalidExpectedPartCountIsRejected() {
        try {
            PlayerRenderPolicy.shouldSwitchToCompatibility(true, 0, 0, 0);
            fail("expectedParts must be positive");
        } catch (IllegalArgumentException expected) {
            assertEquals("expectedParts must be positive", expected.getMessage());
        }
    }
}
