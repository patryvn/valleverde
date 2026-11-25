package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class AnimalSystemTest {
    @Test public void forageFeedsTheNearestAdultAndConsumesOneUnit() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);

        assertEquals(AnimalSystem.FED, herd.feedNearest(3.0f, -1.0f));
        assertEquals(1, herd.getForage());
        assertEquals(25.0f, herd.getCowHunger(), 0.01f);
        assertEquals(70.0f, herd.getBullHunger(), 0.01f);
    }

    @Test public void caredForPairStartsPregnancyAndProducesGrowingCalf() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        assertEquals(AnimalSystem.FED, herd.feedNearest(3.0f, -1.0f));
        assertEquals(AnimalSystem.FED, herd.feedNearest(6.0f, -2.0f));

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

    @Test public void saveAndRestoreKeepsPregnancyProgress() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        herd.feedNearest(3.0f, -1.0f);
        herd.feedNearest(6.0f, -2.0f);
        herd.update(AnimalSystem.BREEDING_READY_MS + 45_000L);
        assertTrue(herd.isPregnant());

        AnimalSystem restored = new AnimalSystem();
        restored.loadState(herd.saveState());
        assertTrue(restored.isPregnant());
        assertEquals(45_000L, restored.getGestationMs());

        restored.update(AnimalSystem.GESTATION_DURATION_MS - 45_000L);
        assertTrue(restored.hasCalf());
    }

    @Test public void interactionOutsideThePastureDoesNothing() {
        AnimalSystem herd = new AnimalSystem();
        herd.addForage(2);
        assertEquals(AnimalSystem.NO_ANIMAL, herd.feedNearest(30.0f, 30.0f));
        assertEquals(2, herd.getForage());
    }
}
