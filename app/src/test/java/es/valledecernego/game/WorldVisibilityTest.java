package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldVisibilityTest {
    @Test public void keepsObjectsAheadOfThePlayerInsideTheView() {
        assertTrue(WorldVisibility.isVisible(0f,5f,0f,0f,18f,2f,40f));
    }
    @Test public void rejectsObjectsFarBehindTheCamera() {
        assertFalse(WorldVisibility.isVisible(0f,5f,0f,0f,-8f,1f,40f));
    }
    @Test public void rejectsObjectsFarOutsideTheHorizontalFrustum() {
        assertFalse(WorldVisibility.isVisible(0f,5f,0f,24f,10f,.5f,40f));
    }
    @Test public void yawRotatesTheVisibleHalfSpaceWithTheCamera() {
        assertFalse(WorldVisibility.isVisible(0f,0f,0f,12f,0f,1f,40f));
        assertTrue(WorldVisibility.isVisible(0f,0f,(float)(Math.PI/2d),12f,0f,1f,40f));
    }
    @Test public void rejectsObjectsBeyondTheMaximumRenderDistance() {
        assertFalse(WorldVisibility.isVisible(0f,0f,0f,0f,80f,2f,40f));
    }
    @Test public void objectBoundsCanOverlapTheNearPlane() {
        assertTrue(WorldVisibility.isVisible(0f,0f,0f,0f,-1f,2f,40f));
    }
    @Test public void invalidBoundsAreNeverRendered() {
        assertFalse(WorldVisibility.isVisible(0f,0f,0f,1f,2f,-1f,40f));
        assertFalse(WorldVisibility.isVisible(Float.NaN,0f,0f,1f,2f,1f,40f));
    }
    @Test public void typeBoundsMatchTheRuralScene() {
        assertTrue(WorldVisibility.radiusForType(WorldLayout.HOUSE)>WorldVisibility.radiusForType(WorldLayout.ROCK));
        assertTrue(WorldVisibility.radiusForType(WorldLayout.COW)>WorldVisibility.radiusForType(WorldLayout.BUSH));
    }
}
