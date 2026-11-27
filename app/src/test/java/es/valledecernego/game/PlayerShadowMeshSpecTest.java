package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerShadowMeshSpecTest {
    @Test public void ovalShadowHasThreeInterleavedVerticesPerSlice() {
        int slices=32;
        float[] vertices=PlayerShadowMeshSpec.buildInterleaved(slices);
        assertEquals(slices*3*6,vertices.length);
        for(int i=0;i<vertices.length;i+=6) {
            assertTrue(Float.isFinite(vertices[i]));
            assertTrue(Float.isFinite(vertices[i+1]));
            assertTrue(Float.isFinite(vertices[i+2]));
            assertEquals(0f,vertices[i+3],0.0001f);
            assertEquals(1f,vertices[i+4],0.0001f);
            assertEquals(0f,vertices[i+5],0.0001f);
        }
    }

    @Test public void ovalShadowStaysInsideUnitDisc() {
        float[] vertices=PlayerShadowMeshSpec.buildInterleaved(48);
        for(int i=0;i<vertices.length;i+=6) {
            float x=vertices[i], z=vertices[i+2];
            assertTrue(x>=-1.0001f && x<=1.0001f);
            assertTrue(z>=-1.0001f && z<=1.0001f);
            assertTrue(x*x+z*z<=1.0001f);
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void shadowRejectsTooFewSlices() {
        PlayerShadowMeshSpec.buildInterleaved(7);
    }
}
