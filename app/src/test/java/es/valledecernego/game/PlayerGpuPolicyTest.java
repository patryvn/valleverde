package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerGpuPolicyTest {
    @Test public void pocoX6ProMaliG615SelectsTheSimpleShaderWithoutDroppingModelParts() {
        assertTrue(PlayerGpuPolicy.shouldPreferSimpleShader("Mali-G615 MC6"));
        assertEquals("mali-g615-simple-shader-60",
            PlayerGpuPolicy.profileName("ARM Mali-G615 MC6"));
    }

    @Test public void rendererMatchingIsCaseInsensitive() {
        assertTrue(PlayerGpuPolicy.shouldPreferSimpleShader("arm mali-g615 mc6"));
    }

    @Test public void swiftshaderKeepsTheDetailedLitRendererForCi() {
        assertFalse(PlayerGpuPolicy.shouldPreferSimpleShader("Google SwiftShader"));
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
}
