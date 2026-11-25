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
        assertEquals("Cuadra construida", restored.getSummary());
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
