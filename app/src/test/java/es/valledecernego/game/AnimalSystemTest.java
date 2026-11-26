package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class AnimalSystemTest {
    @Test public void forageFeedsTheNearestAdultAndConsumesOneUnit() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);

        assertEquals(AnimalSystem.FED, herd.feedNearest(WorldLayout.COW_X, WorldLayout.COW_Z));
        assertEquals(1, herd.getForage());
        assertEquals(25.0f, herd.getCowHunger(), 0.01f);
        assertEquals(70.0f, herd.getBullHunger(), 0.01f);
    }

    @Test public void actualWorldSpawnPointsCanFeedBothAdults() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        assertEquals(AnimalSystem.FED, herd.feedNearest(WorldLayout.COW_X, WorldLayout.COW_Z));
        assertEquals(AnimalSystem.FED, herd.feedNearest(WorldLayout.BULL_X, WorldLayout.BULL_Z));
        assertEquals(0, herd.getForage());
    }

    @Test public void caredForPairStartsPregnancyAndProducesGrowingCalf() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        assertEquals(AnimalSystem.FED, herd.feedNearest(WorldLayout.COW_X, WorldLayout.COW_Z));
        assertEquals(AnimalSystem.FED, herd.feedNearest(WorldLayout.BULL_X, WorldLayout.BULL_Z));

        herd.update(AnimalSystem.BREEDING_READY_MS - 1L);
        assertFalse(herd.isPregnant());
        herd.update(1L);
        assertTrue(herd.isPregnant());

        herd.update(AnimalSystem.GESTATION_DURATION_MS);
        assertFalse(herd.isPregnant());
        assertTrue(herd.hasCalf());
        assertTrue(herd.isCalfGrowing());

        herd.update(AnimalSystem.CALF_GROWTH_DURATION_MS);
        assertFalse(herd.isCalfGrowing());
    }

    @Test public void calfGrowthProgressMovesFromNewbornToAdult() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        herd.feedNearest(WorldLayout.COW_X, WorldLayout.COW_Z);
        herd.feedNearest(WorldLayout.BULL_X, WorldLayout.BULL_Z);
        herd.update(AnimalSystem.BREEDING_READY_MS + AnimalSystem.GESTATION_DURATION_MS);

        assertTrue(herd.hasCalf());
        assertEquals(0f, herd.getCalfGrowthProgress(), 0.001f);
        herd.update(AnimalSystem.CALF_GROWTH_DURATION_MS / 2L);
        assertEquals(0.5f, herd.getCalfGrowthProgress(), 0.001f);
        herd.update(AnimalSystem.CALF_GROWTH_DURATION_MS / 2L);
        assertEquals(1f, herd.getCalfGrowthProgress(), 0.001f);
        assertFalse(herd.isCalfGrowing());
    }

    @Test public void winterRainIncreasesHerdFeedDemand() {
        AnimalSystem mild = new AnimalSystem();
        AnimalSystem harsh = new AnimalSystem();
        mild.update(10_000L, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        harsh.update(10_000L, SeasonSystem.WINTER, SeasonSystem.RAIN);
        assertTrue(harsh.getCowHunger() > mild.getCowHunger());
        assertEquals(71.5f, mild.getCowHunger(), 0.01f);
        assertEquals(72.2275f, harsh.getCowHunger(), 0.02f);
    }

    @Test public void saveAndRestoreKeepsPregnancyProgress() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        herd.feedNearest(WorldLayout.COW_X, WorldLayout.COW_Z);
        herd.feedNearest(WorldLayout.BULL_X, WorldLayout.BULL_Z);
        herd.update(AnimalSystem.BREEDING_READY_MS + 45_000L);
        assertTrue(herd.isPregnant());

        AnimalSystem restored = new AnimalSystem();
        restored.loadState(herd.saveState());
        assertTrue(restored.isPregnant());
        assertEquals(45_000L, restored.getGestationMs());

        restored.update(AnimalSystem.GESTATION_DURATION_MS - 45_000L);
        assertTrue(restored.hasCalf());
    }

    @Test public void stableFeedTargetsHungriestAdultWithoutUsingLooseForage() {
        AnimalSystem herd = new AnimalSystem();
        herd.update(60_000L);

        assertTrue(herd.feedFromStable());
        assertEquals(49.0f, herd.getCowHunger(), 0.01f);
        assertEquals(79.0f, herd.getBullHunger(), 0.01f);
        assertEquals(0, herd.getForage());
    }

    @Test public void removingForageReturnsOnlyWhatWasAvailable() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(3);
        assertEquals(2, herd.removeForage(2));
        assertEquals(1, herd.getForage());
        assertEquals(1, herd.removeForage(99));
        assertEquals(0, herd.getForage());
        assertEquals(0, herd.removeForage(-1));
    }

    @Test public void interactionOutsideThePastureDoesNothing() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        assertEquals(AnimalSystem.NO_ANIMAL, herd.feedNearest(30.0f, 30.0f));
        assertEquals(2, herd.getForage());
    }
}
