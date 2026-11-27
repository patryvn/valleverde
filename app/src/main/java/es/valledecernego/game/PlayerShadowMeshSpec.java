package es.valledecernego.game;

/**
 * Flat oval ground shadow made from a triangle fan.
 * Vertices are interleaved as position XYZ and an upward normal XYZ so the
 * same buffer works with both the lit renderer and the minimal GLES shader.
 */
public final class PlayerShadowMeshSpec {
    private PlayerShadowMeshSpec() { }

    public static float[] buildInterleaved(int slices) {
        if (slices < 8) throw new IllegalArgumentException("slices must be at least 8");
        float[] vertices = new float[slices * 3 * 6];
        int cursor = 0;
        for (int i = 0; i < slices; i++) {
            double a = Math.PI * 2.0 * i / slices;
            double b = Math.PI * 2.0 * (i + 1) / slices;
            cursor = append(vertices, cursor, 0f, 0f, 0f);
            cursor = append(vertices, cursor, (float)Math.cos(b), 0f, (float)Math.sin(b));
            cursor = append(vertices, cursor, (float)Math.cos(a), 0f, (float)Math.sin(a));
        }
        return vertices;
    }

    private static int append(float[] target, int cursor, float x, float y, float z) {
        target[cursor++] = x;
        target[cursor++] = y;
        target[cursor++] = z;
        target[cursor++] = 0f;
        target[cursor++] = 1f;
        target[cursor++] = 0f;
        return cursor;
    }
}
