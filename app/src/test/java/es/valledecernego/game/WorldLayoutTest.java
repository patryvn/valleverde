package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldLayoutTest {
    @Test public void ruralLandmarksArePlacedAheadOfTheSpawn() {
        float[][] objects = WorldLayout.initialObjects();
        assertTrue(objects.length >= 16);
        for (float[] object : objects) {
            assertEquals(4, object.length);
            assertTrue("all landmarks must be ahead of the spawn", object[1] > WorldLayout.SPAWN_Z + 5f);
            assertTrue("landmarks must remain within the initial playable width", Math.abs(object[0]) <= 18f);
            assertTrue("landmarks must remain on the expanded ground plane", object[1] <= 40f);
            assertEquals(1f, object[3], 0f);
        }
        assertTrue(WorldLayout.countType(WorldLayout.TREE) >= 6);
        assertTrue(WorldLayout.countType(WorldLayout.ROCK) >= 2);
        assertTrue(WorldLayout.countType(WorldLayout.COW) >= 1);
        assertTrue(WorldLayout.countType(WorldLayout.CALF) >= 1);
        assertTrue(WorldLayout.countType(WorldLayout.STABLE) >= 1);
        assertTrue(WorldLayout.countType(WorldLayout.MARKET) >= 1);
        assertTrue(WorldLayout.countType(WorldLayout.VILLAGE_BOARD) >= 1);
        assertTrue(WorldLayout.countType(WorldLayout.HOUSE) >= 1);
    }

    @Test public void eachSessionReceivesItsOwnMutableObjectCopies() {
        float[][] first = WorldLayout.initialObjects();
        float[][] second = WorldLayout.initialObjects();
        first[0][0] = 999f;
        first[0][3] = 0f;
        assertNotEquals(first[0][0], second[0][0], 0f);
        assertEquals(1f, second[0][3], 0f);
    }
}
