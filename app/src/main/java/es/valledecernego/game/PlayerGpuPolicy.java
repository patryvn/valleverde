package es.valledecernego.game;

import java.util.Locale;

/**
 * Selects a conservative player shader for the Mali-G615 family.
 *
 * The Poco X6 Pro uses a Mali-G615 MC6 GPU. The policy avoids the more complex
 * procedural lighting/material shader on that renderer while retaining all
 * 60 humanoid parts, their colours and the same animation transforms.
 */
public final class PlayerGpuPolicy {
    private PlayerGpuPolicy() { }

    public static boolean shouldPreferSimpleShader(String glRenderer) {
        if (glRenderer == null) return false;
        return glRenderer.toLowerCase(Locale.US).contains("mali-g615");
    }

    public static String profileName(String glRenderer) {
        return shouldPreferSimpleShader(glRenderer)
            ? "mali-g615-simple-shader-60"
            : "default-lit-shader-60";
    }
    
    /**
     * Compact on-screen diagnostic for the actual GLES renderer and selected player path.
     * This lets device testers report the GPU without ADB or logcat.
     */
    public static String diagnosticLabel(String glRenderer, boolean simpleShaderSelected) {
        String renderer = glRenderer == null ? "" : glRenderer.trim().replaceAll("\\s+", " ");
        if (renderer.isEmpty() || "null".equalsIgnoreCase(renderer)
                || "unknown".equalsIgnoreCase(renderer)) renderer = "desconocida";
        if (renderer.length() > 22) renderer = renderer.substring(0, 22);
        return "GPU " + renderer + " · " + (simpleShaderSelected ? "SIMPLE60" : "LIT60");
    }
}
