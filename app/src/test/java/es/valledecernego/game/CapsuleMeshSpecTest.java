package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class CapsuleMeshSpecTest {
    @Test public void capsuleLongAxisIsVerticalForPlayerAndAnimalLimbs() {
        float[] vertices = CapsuleMeshSpec.buildInterleaved(10, 24);
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertices.length; i += 6) {
            minX = Math.min(minX, vertices[i]);
            maxX = Math.max(maxX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            maxY = Math.max(maxY, vertices[i + 1]);
            minZ = Math.min(minZ, vertices[i + 2]);
            maxZ = Math.max(maxZ, vertices[i + 2]);
        }
        assertEquals(-1f, minY, 0.0001f);
        assertEquals(1f, maxY, 0.0001f);
        assertEquals(-0.5f, minX, 0.0001f);
        assertEquals(0.5f, maxX, 0.0001f);
        assertEquals(-0.5f, minZ, 0.0001f);
        assertEquals(0.5f, maxZ, 0.0001f);
        assertTrue("capsule length must follow the Y scale used by limb transforms",
            (maxY - minY) > 1.9f * (maxX - minX));
        assertTrue((maxY - minY) > 1.9f * (maxZ - minZ));
    }

    @Test public void capsuleNormalsAreFiniteAndUnitLength() {
        float[] vertices = CapsuleMeshSpec.buildInterleaved(8, 20);
        for (int i = 0; i < vertices.length; i += 6) {
            for (int axis = 0; axis < 6; axis++) {
                assertTrue("all vertex data must be finite",
                    Float.isFinite(vertices[i + axis]));
            }
            float nx = vertices[i + 3], ny = vertices[i + 4], nz = vertices[i + 5];
            assertEquals(1f, (float) Math.sqrt(nx * nx + ny * ny + nz * nz), 0.0002f);
        }
    }

    @Test public void cylinderSectionKeepsConstantRadius() {
        float[] vertices = CapsuleMeshSpec.buildInterleaved(10, 24);
        int checked = 0;
        for (int i = 0; i < vertices.length; i += 6) {
            float y = vertices[i + 1];
            if (Math.abs(y) <= 0.1001f) {
                float radius = (float) Math.hypot(vertices[i], vertices[i + 2]);
                assertEquals(0.5f, radius, 0.0001f);
                checked++;
            }
        }
        assertTrue("expected vertices in the straight capsule section", checked > 0);
    }

    @Test public void invalidMeshResolutionIsRejected() {
        try {
            CapsuleMeshSpec.buildInterleaved(0, 24);
            fail("zero hemisphere stacks must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("hemisphereStacks must be at least 1", expected.getMessage());
        }
        try {
            CapsuleMeshSpec.buildInterleaved(8, 2);
            fail("fewer than three slices must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("slices must be at least 3", expected.getMessage());
        }
    }
}
