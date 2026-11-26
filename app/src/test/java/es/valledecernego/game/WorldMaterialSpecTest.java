package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldMaterialSpecTest {
    @Test public void terrainNoiseIsDeterministicAndBounded() {
        float first = WorldMaterialSpec.noise(12.5f, -7.25f, WorldMaterialSpec.PATCH_SCALE);
        float second = WorldMaterialSpec.noise(12.5f, -7.25f, WorldMaterialSpec.PATCH_SCALE);
        assertEquals(first, second, 0f);
        assertTrue(first >= 0f && first <= 1f);
    }

    @Test public void terrainBrightnessHasNaturalVariationAndSafeRange() {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int z = -12; z <= 12; z++) {
            for (int x = -12; x <= 12; x++) {
                float value = WorldMaterialSpec.terrainBrightness(x * .73f, z * .61f);
                min = Math.min(min, value);
                max = Math.max(max, value);
                assertTrue(value >= .80f && value <= 1.02f);
            }
        }
        assertTrue("procedural ground should not be a uniform flat colour", max - min > .025f);
    }

    @Test public void skyGradientIsMonotonicAndClamped() {
        assertEquals(0f, WorldMaterialSpec.skyGradient(-2f), 0f);
        assertEquals(1f, WorldMaterialSpec.skyGradient(2f), 0f);
        float previous = WorldMaterialSpec.skyGradient(-.28f);
        for (int i = -27; i <= 68; i++) {
            float current = WorldMaterialSpec.skyGradient(i / 100f);
            assertTrue(current >= previous);
            previous = current;
        }
    }

    @Test public void invalidNoiseInputsUseNeutralFallback() {
        assertEquals(.5f, WorldMaterialSpec.noise(Float.NaN, 1f, .4f), 0f);
        assertEquals(.5f, WorldMaterialSpec.noise(1f, 1f, 0f), 0f);
        assertTrue(Float.isFinite(WorldMaterialSpec.terrainBrightness(Float.NaN, 1f)));
    }
}
