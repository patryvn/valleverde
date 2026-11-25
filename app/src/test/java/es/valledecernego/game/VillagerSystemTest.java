package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class VillagerSystemTest {
    @Test public void workersProduceOnlyAfterAssignedJobInterval() {
        VillagerSystem village = new VillagerSystem();
        assertTrue(village.assignJob(0, VillagerSystem.FARMER));
        village.update(VillagerSystem.FARMER_INTERVAL_MS - 1L);
        assertEquals(0, village.getPendingHarvest());
        village.update(1L);
        assertEquals(2, village.getPendingHarvest());
        assertEquals("Lúa", village.snapshot()[0].name());
    }

    @Test public void threeJobsProduceTheirOwnResources() {
        VillagerSystem village = new VillagerSystem();
        assertTrue(village.assignJob(0, VillagerSystem.FARMER));
        assertTrue(village.assignJob(1, VillagerSystem.RANCHER));
        assertTrue(village.assignJob(2, VillagerSystem.WOODCUTTER));
        village.update(VillagerSystem.FARMER_INTERVAL_MS);
        assertEquals(2, village.getPendingHarvest());
        assertEquals(2, village.getPendingForage());
        assertEquals(2, village.getPendingWood());
    }

    @Test public void workersGainExperienceAndLevelUpAfterFiveCycles() {
        VillagerSystem village = new VillagerSystem();
        village.assignJob(2, VillagerSystem.WOODCUTTER);
        village.update(VillagerSystem.WOODCUTTER_INTERVAL_MS * 5L);
        VillagerSystem.Worker worker = village.snapshot()[2];
        assertEquals(5, worker.xp());
        assertEquals(2, worker.level());

        village.update(VillagerSystem.WOODCUTTER_INTERVAL_MS * 5L);
        worker = village.snapshot()[2];
        assertEquals(10, worker.xp());
        assertEquals(3, worker.level());
        village.update(VillagerSystem.WOODCUTTER_INTERVAL_MS);
        assertEquals(23, village.getPendingWood());
    }

    @Test public void partialWorkAndPendingGoodsSurviveSaveAndRestore() {
        VillagerSystem village = new VillagerSystem();
        village.assignJob(0, VillagerSystem.FARMER);
        village.update(35_000L);
        village.update(VillagerSystem.FARMER_INTERVAL_MS);
        assertEquals(2, village.getPendingHarvest());

        VillagerSystem restored = new VillagerSystem();
        restored.loadState(village.saveState());
        assertEquals(2, restored.getPendingHarvest());
        assertEquals(35_000L, restored.snapshot()[0].workMs());
        restored.update(VillagerSystem.FARMER_INTERVAL_MS - 35_000L);
        assertEquals(4, restored.getPendingHarvest());
        assertEquals(0L, restored.snapshot()[0].workMs());
    }

    @Test public void collectionRespectsInventorySpaceAndKeepsTheRemainder() {
        VillagerSystem village = new VillagerSystem();
        village.assignJob(0, VillagerSystem.FARMER);
        village.assignJob(1, VillagerSystem.RANCHER);
        village.assignJob(2, VillagerSystem.WOODCUTTER);
        village.update(VillagerSystem.FARMER_INTERVAL_MS);
        VillagerSystem.Production collected = village.claimProduction(0, 1, 1);
        assertEquals(0, collected.wood());
        assertEquals(1, collected.forage());
        assertEquals(1, collected.harvest());
        assertEquals(2, village.getPendingWood());
        assertEquals(1, village.getPendingForage());
        assertEquals(1, village.getPendingHarvest());
    }

    @Test public void invalidAssignmentsAndInvalidSavesAreIgnored() {
        VillagerSystem village = new VillagerSystem();
        assertFalse(village.assignJob(-1, VillagerSystem.FARMER));
        assertFalse(village.assignJob(0, 99));
        village.assignJob(0, VillagerSystem.RANCHER);
        String before = village.saveState();
        village.loadState("1|0|0|0|1,2,0,0");
        assertEquals(before, village.saveState());
    }

    @Test public void idleWorkersDoNotAccumulateWork() {
        VillagerSystem village = new VillagerSystem();
        village.update(500_000L);
        assertEquals(0, village.getPendingWood());
        assertEquals(0, village.snapshot()[0].workMs());
    }
}
