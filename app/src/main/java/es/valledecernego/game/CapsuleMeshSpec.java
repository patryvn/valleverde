package es.valledecernego.game;

/**
 * Pure geometry for a capsule whose longitudinal axis is Y.
 *
 * PlayerModelTransform rotates limbs around their upper pivot and scales their
 * length on Y. Keeping the mesh axis aligned with that contract is essential:
 * an X-axis capsule renders arms and legs sideways even when the transforms
 * and tests report correct world-space positions.
 *
 * Vertices are interleaved as position XYZ and normal XYZ.
 */
public final class CapsuleMeshSpec {
    private CapsuleMeshSpec() { }

    public static float[] buildInterleaved(int hemisphereStacks, int slices) {
        if (hemisphereStacks < 1) {
            throw new IllegalArgumentException("hemisphereStacks must be at least 1");
        }
        if (slices < 3) {
            throw new IllegalArgumentException("slices must be at least 3");
        }

        int ringCount = (hemisphereStacks + 1) * 2 + 4;
        float[][] rings = new float[ringCount][3];
        int ringIndex = 0;

        // Lower hemisphere: pole at y=-1, equator at y=-0.5.
        for (int i = 0; i <= hemisphereStacks; i++) {
            float angle = (float) (-Math.PI * 0.5 + Math.PI * 0.5 * i / hemisphereStacks);
            rings[ringIndex++] = new float[] {
                -0.5f + 0.5f * (float) Math.sin(angle),
                0.5f * (float) Math.cos(angle),
                (float) Math.sin(angle)
            };
        }

        // Straight cylindrical section, from y=-0.5 to y=0.5.
        for (int i = 1; i < 5; i++) {
            rings[ringIndex++] = new float[] { -0.5f + i * 0.2f, 0.5f, 0f };
        }

        // Upper hemisphere: equator at y=0.5, pole at y=1.
        for (int i = 0; i <= hemisphereStacks; i++) {
            float angle = (float) (Math.PI * 0.5 * i / hemisphereStacks);
            rings[ringIndex++] = new float[] {
                0.5f + 0.5f * (float) Math.sin(angle),
                0.5f * (float) Math.cos(angle),
                (float) Math.sin(angle)
            };
        }

        int vertexCount = (ringCount - 1) * slices * 6;
        float[] vertices = new float[vertexCount * 6];
        int cursor = 0;
        for (int ring = 0; ring < rings.length - 1; ring++) {
            float[] lower = rings[ring];
            float[] upper = rings[ring + 1];
            for (int slice = 0; slice < slices; slice++) {
                float a = (float) (Math.PI * 2.0 * slice / slices);
                float b = (float) (Math.PI * 2.0 * (slice + 1) / slices);
                cursor = appendVertex(vertices, cursor, lower, a);
                cursor = appendVertex(vertices, cursor, upper, a);
                cursor = appendVertex(vertices, cursor, upper, b);
                cursor = appendVertex(vertices, cursor, lower, a);
                cursor = appendVertex(vertices, cursor, upper, b);
                cursor = appendVertex(vertices, cursor, lower, b);
            }
        }
        return vertices;
    }

    private static int appendVertex(float[] target, int cursor, float[] ring, float longitude) {
        float normalY = ring[2];
        float radialNormal = (float) Math.sqrt(Math.max(0f, 1f - normalY * normalY));
        float cos = (float) Math.cos(longitude);
        float sin = (float) Math.sin(longitude);
        target[cursor++] = ring[1] * cos;
        target[cursor++] = ring[0];
        target[cursor++] = ring[1] * sin;
        target[cursor++] = radialNormal * cos;
        target[cursor++] = normalY;
        target[cursor++] = radialNormal * sin;
        return cursor;
    }
}
