package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerRenderPolicyTest {
    @Test public void newSessionsPreferDetailedHumanoidWhenPrimaryRendererIsReady() {
        assertFalse(PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE);
        assertFalse(PlayerRenderPolicy.shouldUseCompatibilityModel(
            PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE,true,true));
        assertTrue(PlayerRenderPolicy.shouldDrawDetailedModel(
            true,PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE));
    }

    @Test public void newSessionsStillUseRoundedFallbackWhenPrimaryRendererIsUnavailable() {
        assertTrue(PlayerRenderPolicy.shouldUseCompatibilityModel(
            PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE,false,true));
        assertFalse(PlayerRenderPolicy.shouldDrawDetailedModel(
            false,PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE));
    }

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

    @Test public void renderModeButtonIsVisibleAndDoesNotOverlapGameplayControls() {
        assertTrue(PlayerRenderPolicy.isRenderModeButtonHit(680f,99f,720f,405f));
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(79f,318f,720f,405f));
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(662f,334f,720f,405f));
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(579f,346f,720f,405f));
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(680f,180f,720f,405f));
    }

    @Test public void renderModeButtonRejectsInvalidCoordinatesAndSizes() {
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(Float.NaN,99f,720f,405f));
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(680f,99f,0f,405f));
        assertFalse(PlayerRenderPolicy.isRenderModeButtonHit(680f,99f,720f,0f));
    }

    @Test public void compatibilityModeToggleAlternatesBetweenTheTwoModes() {
        boolean compatibility=PlayerRenderPolicy.toggleCompatibilityMode(false);
        assertTrue(compatibility);
        compatibility=PlayerRenderPolicy.toggleCompatibilityMode(compatibility);
        assertFalse(compatibility);
        compatibility=PlayerRenderPolicy.toggleCompatibilityMode(compatibility);
        assertTrue(compatibility);
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
