package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class HouseSystemTest {
    @Test public void houseRequiresMaterialsAndCanBeBuiltOnce() {
        HouseSystem house = new HouseSystem();
        assertEquals(HouseSystem.INSUFFICIENT_MATERIALS, house.build(9, 4));
        assertFalse(house.isBuilt());
        assertEquals(HouseSystem.BUILT, house.build(10, 4));
        assertTrue(house.isBuilt());
        assertEquals(HouseSystem.BUILT, house.build(0, 0));
    }

    @Test public void houseSurvivesSaveAndLoad() {
        HouseSystem house = new HouseSystem();
        house.build(10, 4);
        HouseSystem restored = new HouseSystem();
        restored.loadState(house.saveState());
        assertTrue(restored.isBuilt());
        restored.loadState("invalid");
        assertTrue(restored.isBuilt());
    }
}
