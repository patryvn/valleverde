package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class BuildingSystemTest {
    @Test public void buildingRequiresMaterialsAndDoesNotConsumeOnFailure() {
        BuildingSystem buildings = new BuildingSystem();
        assertEquals(BuildingSystem.INSUFFICIENT_MATERIALS,
            buildings.buildStable(5, 4));
        assertFalse(buildings.isStableBuilt());
        assertEquals(BuildingSystem.INSUFFICIENT_MATERIALS,
            buildings.buildStable(6, 3));
        assertFalse(buildings.isStableBuilt());
    }

    @Test public void stableCanBeBuiltOnlyOnce() {
        BuildingSystem buildings = new BuildingSystem();
        assertEquals(BuildingSystem.BUILT,
            buildings.buildStable(BuildingSystem.STABLE_WOOD_COST,
                BuildingSystem.STABLE_STONE_COST));
        assertTrue(buildings.isStableBuilt());
        assertEquals(BuildingSystem.ALREADY_BUILT,
            buildings.buildStable(99, 99));
    }

    @Test public void builtStableSurvivesSaveAndRestore() {
        BuildingSystem buildings = new BuildingSystem();
        buildings.buildStable(6, 4);
        BuildingSystem restored = new BuildingSystem();
        restored.loadState(buildings.saveState());
        assertTrue(restored.isStableBuilt());
        assertEquals("Cuadra construida • Heno 0/30", restored.getSummary());
    }

    @Test public void stableStoresForageOnlyAfterConstructionAndRespectsCapacity() {
        BuildingSystem buildings = new BuildingSystem();
        assertEquals(0, buildings.storeHay(5));
        buildings.buildStable(6, 4);
        assertEquals(30, buildings.storeHay(40));
        assertEquals(30, buildings.getStoredHay());
        assertEquals(0, buildings.storeHay(2));
    }

    @Test public void stableConsumesHayToFeedTheHungriestAdultOncePerMinute() {
        BuildingSystem buildings = new BuildingSystem();
        buildings.buildStable(6, 4);
        buildings.storeHay(3);
        AnimalSystem herd = new AnimalSystem();
        herd.update(60_000L);

        assertEquals(1, buildings.updateStable(60_000L, herd));
        assertEquals(2, buildings.getStoredHay());
        assertEquals(49.0f, herd.getCowHunger(), 0.01f);
        assertEquals(79.0f, herd.getBullHunger(), 0.01f);
        assertEquals(0, buildings.updateStable(59_999L, herd));
        assertEquals(1, buildings.updateStable(1L, herd));
        assertEquals(1, buildings.getStoredHay());
        assertEquals(49.0f, herd.getBullHunger(), 0.01f);
    }

    @Test public void stableHayAndPartialFeedTimerSurviveSaveAndRestore() {
        BuildingSystem buildings = new BuildingSystem();
        buildings.buildStable(6, 4);
        buildings.storeHay(4);
        AnimalSystem herd = new AnimalSystem();
        herd.update(60_000L);
        assertEquals(0, buildings.updateStable(30_000L, herd));

        BuildingSystem restored = new BuildingSystem();
        restored.loadState(buildings.saveState());
        assertEquals(4, restored.getStoredHay());
        assertEquals(1, restored.updateStable(30_000L, herd));
        assertEquals(3, restored.getStoredHay());
    }

    @Test public void invalidSaveDoesNotChangeDefaultState() {
        BuildingSystem buildings = new BuildingSystem();
        buildings.loadState("built");
        assertFalse(buildings.isStableBuilt());
        buildings.loadState("1");
        buildings.loadState("invalid");
        assertTrue(buildings.isStableBuilt());
    }
}
