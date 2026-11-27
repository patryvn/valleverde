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
}
