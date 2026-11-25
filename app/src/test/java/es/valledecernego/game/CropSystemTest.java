package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class CropSystemTest {
    @Test public void cropMustBePlantedWateredAndGrownBeforeHarvest() {
        CropSystem farm = new CropSystem();

        assertEquals(CropSystem.PLANTED_RESULT, farm.interact(-4.0f, 1.0f));
        assertEquals(5, farm.getSeeds());

        farm.update(CropSystem.GROWTH_DURATION_MS * 2);
        assertEquals(CropSystem.PLANTED, farm.plotForTest(0).stage());

        assertEquals(CropSystem.WATERED, farm.interact(-4.0f, 1.0f));
        farm.update(CropSystem.GROWTH_DURATION_MS - 1);
        assertEquals(CropSystem.GROWING, farm.plotForTest(0).stage());

        farm.update(1);
        assertEquals(CropSystem.READY, farm.plotForTest(0).stage());
        assertEquals(1, farm.getReadyCount());

        assertEquals(CropSystem.HARVESTED, farm.interact(-4.0f, 1.0f));
        assertEquals(3, farm.getHarvest());
        assertEquals(6, farm.getSeeds());
        assertEquals(CropSystem.EMPTY, farm.plotForTest(0).stage());
    }

    @Test public void saveAndRestoreKeepsCropGrowthProgress() {
        CropSystem farm = new CropSystem();
        assertEquals(CropSystem.PLANTED_RESULT, farm.interact(-1.6f, 1.0f));
        assertEquals(CropSystem.WATERED, farm.interact(-1.6f, 1.0f));
        farm.update(12_345L);

        CropSystem restored = new CropSystem();
        restored.loadState(farm.saveState());

        assertEquals(CropSystem.GROWING, restored.plotForTest(1).stage());
        assertEquals(12_345L, restored.plotForTest(1).growthMs());
        restored.update(CropSystem.GROWTH_DURATION_MS - 12_345L);
        assertEquals(CropSystem.READY, restored.plotForTest(1).stage());
    }

    @Test public void interactionOutsideFarmDoesNothing() {
        CropSystem farm = new CropSystem();
        assertEquals(CropSystem.NO_PLOT, farm.interact(30.0f, 30.0f));
        assertEquals(6, farm.getSeeds());
    }
    @Test public void deliveredHarvestRespectsInventoryCapacity() {
        CropSystem farm = new CropSystem();
        assertEquals(999, farm.addHarvest(1_000));
        assertEquals(999, farm.getHarvest());
        assertEquals(0, farm.addHarvest(2));
        assertEquals(0, farm.addHarvest(-4));
    }
}

