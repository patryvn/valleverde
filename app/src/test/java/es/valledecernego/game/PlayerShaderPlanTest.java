package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerShaderPlanTest {
    @Test public void promotedFallbackRemainsAvailableForRoundedHumanoid() {
        PlayerShaderPlan plan=PlayerShaderPlan.resolve(false,false,true);
        assertTrue(plan.promoteSilhouette);
        assertTrue(plan.rendererReady);
        assertTrue(plan.silhouetteReady);
        assertTrue(plan.compatibilityRenderer);
    }

    @Test public void healthyPrimaryKeepsIndependentRoundedShader() {
        PlayerShaderPlan plan=PlayerShaderPlan.resolve(true,false,true);
        assertFalse(plan.promoteSilhouette);
        assertTrue(plan.rendererReady);
        assertTrue(plan.silhouetteReady);
        assertFalse(plan.compatibilityRenderer);
    }

    @Test public void existingFlatPrimaryKeepsItsCompatibilityFlag() {
        PlayerShaderPlan plan=PlayerShaderPlan.resolve(true,true,true);
        assertFalse(plan.promoteSilhouette);
        assertTrue(plan.rendererReady);
        assertTrue(plan.silhouetteReady);
        assertTrue(plan.compatibilityRenderer);
    }

    @Test public void noLinkedShaderLeavesBothPlayerPathsUnavailable() {
        PlayerShaderPlan plan=PlayerShaderPlan.resolve(false,false,false);
        assertFalse(plan.promoteSilhouette);
        assertFalse(plan.rendererReady);
        assertFalse(plan.silhouetteReady);
        assertFalse(plan.compatibilityRenderer);
    }
}
