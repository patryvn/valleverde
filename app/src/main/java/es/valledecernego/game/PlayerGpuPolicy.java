package es.valledecernego.game;

import java.util.Locale;

/**
 * Selects a cube-only 60-part player path for the Mali-G615 family.
 *
 * The Poco X6 Pro uses a Mali-G615 MC6 GPU. BOX60 avoids both the complex
 * material shader and sphere/capsule mesh buffers while retaining the full
 * character, its colours and the same articulated animation transforms.
 */
public final class PlayerGpuPolicy {
    private PlayerGpuPolicy() { }

    public static boolean shouldPreferSimpleShader(String glRenderer) {
        if (glRenderer == null) return false;
        return glRenderer.toLowerCase(Locale.US).contains("mali-g615");
    }

    public static boolean shouldUseBoxOnlyGeometry(String glRenderer) {
        return shouldUseBoxOnlyGeometry(glRenderer, false);
    }

    /**
     * Explicit user override takes precedence over renderer-name heuristics.
     * Some Android vendors expose generic renderer strings, so a manual BOX60
     * selection must not depend on identifying the GPU family correctly.
     */
    public static boolean shouldUseBoxOnlyGeometry(String glRenderer, boolean forceBoxOnly) {
        return forceBoxOnly || shouldPreferSimpleShader(glRenderer);
    }

    public static String profileName(String glRenderer) {
        return shouldUseBoxOnlyGeometry(glRenderer)
            ? "mali-g615-box-only-60"
            : "default-lit-shader-60";
    }
    
    /**
     * Compact on-screen diagnostic for the actual GLES renderer and selected player path.
     * This lets device testers report the GPU without ADB or logcat.
     */
    public static String diagnosticLabel(String glRenderer, boolean simpleShaderSelected) {
        return diagnosticLabel(glRenderer, simpleShaderSelected,
            simpleShaderSelected && shouldUseBoxOnlyGeometry(glRenderer));
    }

    public static String diagnosticLabel(String glRenderer, boolean simpleShaderSelected,
                                         boolean boxOnlySelected) {
        String renderer = glRenderer == null ? "" : glRenderer.trim().replaceAll("\\s+", " ");
        if (renderer.isEmpty() || "null".equalsIgnoreCase(renderer)
                || "unknown".equalsIgnoreCase(renderer)) renderer = "desconocida";
        if (renderer.length() > 22) renderer = renderer.substring(0, 22);
        String mode = !simpleShaderSelected ? "LIT60" : (boxOnlySelected ? "BOX60" : "SIMPLE60");
        return "GPU " + renderer + " · " + mode;
    }
}
