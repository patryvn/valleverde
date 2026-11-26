package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class TreeVisualSpecTest {
    @Test public void worldCoordinatesProduceStableBoundedAppearance() {
        float seed = TreeVisualSpec.seed(-10f, 16f);
        assertTrue(seed >= 0f && seed < 1f);
        assertEquals(seed, TreeVisualSpec.seed(-10f, 16f), 0f);
        assertEquals(TreeVisualSpec.species(-10f, 16f), TreeVisualSpec.species(-10f, 16f));
        assertEquals(TreeVisualSpec.scale(5f, 23f), TreeVisualSpec.scale(5f, 23f), 0f);
        assertTrue(TreeVisualSpec.scale(5f, 23f) >= 0.92f);
        assertTrue(TreeVisualSpec.scale(5f, 23f) < 1.08f);
        assertTrue(TreeVisualSpec.crownScale(5f, 23f) >= 0.92f);
        assertTrue(TreeVisualSpec.crownScale(5f, 23f) < 1.08f);
        assertTrue(TreeVisualSpec.foliageShade(5f, 23f) >= 0.90f);
        assertTrue(TreeVisualSpec.foliageShade(5f, 23f) < 1.08f);
    }

    @Test public void initialRuralForestContainsBothEvergreenAndBroadleafVariants() {
        int conifers = 0;
        int broadleaf = 0;
        for (float[] object : WorldLayout.initialObjects()) {
            if ((int) object[2] != WorldLayout.TREE) continue;
            if (TreeVisualSpec.species(object[0], object[1]) == TreeVisualSpec.CONIFER) conifers++;
            else broadleaf++;
        }
        assertTrue("the starting area should include at least one conifer", conifers > 0);
        assertTrue("the starting area should include broadleaf trees", broadleaf > 0);
    }

    @Test public void coniferCanopyNarrowsAndRisesAcrossFourTiers() {
        float previousRadius = Float.MAX_VALUE;
        float previousY = -Float.MAX_VALUE;
        for (int tier = 0; tier < TreeVisualSpec.CONIFER_TIER_COUNT; tier++) {
            float radius = TreeVisualSpec.coniferTierRadius(tier);
            float height = TreeVisualSpec.coniferTierHeight(tier);
            float centerY = TreeVisualSpec.coniferTierCenterY(tier);
            assertTrue(radius > 0f);
            assertTrue(height > 0f);
            assertTrue(radius < previousRadius);
            assertTrue(centerY > previousY);
            previousRadius = radius;
            previousY = centerY;
        }
        assertEquals(0f, TreeVisualSpec.coniferTierRadius(-1), 0f);
        assertEquals(0f, TreeVisualSpec.coniferTierRadius(TreeVisualSpec.CONIFER_TIER_COUNT), 0f);
    }

    @Test public void invalidCoordinatesUseFiniteFallbackValues() {
        assertEquals(0.5f, TreeVisualSpec.seed(Float.NaN, 0f), 0f);
        assertTrue(Float.isFinite(TreeVisualSpec.scale(Float.POSITIVE_INFINITY, 1f)));
        assertEquals(TreeVisualSpec.BROADLEAF, TreeVisualSpec.species(Float.NaN, 1f));
    }
}
