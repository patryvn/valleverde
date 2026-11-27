package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerGpuPolicyTest {
    @Test public void pocoX6ProMaliG615SelectsTheSimpleShaderWithoutDroppingModelParts() {
        assertTrue(PlayerGpuPolicy.shouldPreferSimpleShader("Mali-G615 MC6"));
        assertTrue(PlayerGpuPolicy.shouldUseBoxOnlyGeometry("ARM Mali-G615 MC6"));
        assertEquals("mali-g615-box-only-60",
            PlayerGpuPolicy.profileName("ARM Mali-G615 MC6"));
    }

    @Test public void rendererMatchingIsCaseInsensitive() {
        assertTrue(PlayerGpuPolicy.shouldPreferSimpleShader("arm mali-g615 mc6"));
    }

    @Test public void swiftshaderKeepsTheDetailedLitRendererForCi() {
        assertFalse(PlayerGpuPolicy.shouldPreferSimpleShader("Google SwiftShader"));
        assertFalse(PlayerGpuPolicy.shouldUseBoxOnlyGeometry("Google SwiftShader"));
        assertEquals("default-lit-shader-60",PlayerGpuPolicy.profileName("Google SwiftShader"));
    }

    @Test public void otherGpuFamiliesKeepTheDetailedRenderer() {
        assertFalse(PlayerGpuPolicy.shouldPreferSimpleShader("Adreno 740"));
        assertFalse(PlayerGpuPolicy.shouldPreferSimpleShader("Mali-G715"));
        assertFalse(PlayerGpuPolicy.shouldPreferSimpleShader("PowerVR Rogue"));
    }

    @Test public void missingRendererInformationDoesNotGuess() {
        assertFalse(PlayerGpuPolicy.shouldPreferSimpleShader(null));
        assertEquals("default-lit-shader-60",PlayerGpuPolicy.profileName(null));
    }

    @Test public void diagnosticLabelShowsActualGpuAndSelectedShaderPath() {
        assertEquals("GPU Mali-G615 MC6 · BOX60",
            PlayerGpuPolicy.diagnosticLabel("Mali-G615 MC6", true));
        assertEquals("GPU Google SwiftShader · SIMPLE60",
            PlayerGpuPolicy.diagnosticLabel("Google SwiftShader", true, false));
        assertEquals("GPU Google SwiftShader · LIT60",
            PlayerGpuPolicy.diagnosticLabel("Google SwiftShader", false));
    }

    @Test public void diagnosticLabelHandlesMissingAndLongRendererNames() {
        assertEquals("GPU desconocida · LIT60",
            PlayerGpuPolicy.diagnosticLabel(null, false));
        assertEquals("GPU 1234567890123456789012 · SIMPLE60",
            PlayerGpuPolicy.diagnosticLabel("123456789012345678901234567890", true, false));
    }
}
