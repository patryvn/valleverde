package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class SurvivalSystemTest {
    @Test public void movementConsumesEnergyAndRestRestoresIt() {
        SurvivalSystem s = new SurvivalSystem();
        s.update(10_000L, true, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        assertEquals(94.5f, s.stamina(), 0.01f);
        s.update(10_000L, false, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        assertEquals(100f, s.stamina(), 0.001f);
    }

    @Test public void winterIncreasesHungerAndRainReducesThirstDrain() {
        SurvivalSystem winter = new SurvivalSystem();
        SurvivalSystem springRain = new SurvivalSystem();
        winter.update(200_000L, false, SeasonSystem.WINTER, SeasonSystem.SUNNY);
        springRain.update(200_000L, false, SeasonSystem.SPRING, SeasonSystem.RAIN);
        assertEquals(98f, winter.hunger(), 0.001f);
        assertEquals(99f, springRain.hunger(), 0.001f);
        assertTrue(springRain.thirst() > winter.thirst());
    }

    @Test public void foodAndWaterRestoreNeedsOnlyWhenNeeded() {
        SurvivalSystem s = new SurvivalSystem();
        assertFalse(s.eatFood());
        assertFalse(s.drinkWater());
        s.loadState("1|100|100|60|55|0|0");
        assertTrue(s.eatFood());
        assertTrue(s.drinkWater());
        assertEquals(95f, s.hunger(), 0.001f);
        assertEquals(95f, s.thirst(), 0.001f);
    }

    @Test public void starvationAndDehydrationDamageHealth() {
        SurvivalSystem s = new SurvivalSystem();
        s.loadState("1|100|100|0|0|0|0");
        s.update(10_000L, false, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        assertEquals(95.5f, s.health(), 0.01f);
    }

    @Test public void healthyRestCanRestoreLostHealth() {
        SurvivalSystem s = new SurvivalSystem();
        s.loadState("1|80|100|90|90|0|0");
        s.update(10_000L, false, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        assertEquals(80.8f, s.health(), 0.01f);
    }

    @Test public void exhaustedPlayerMovesMoreSlowlyAndCanStillRecover() {
        SurvivalSystem s = new SurvivalSystem();
        s.loadState("1|100|0|80|80|0|0");
        assertEquals(0.60f, s.movementFactor(), 0.001f);
        assertFalse(s.spendStamina(12f));
        s.update(10_000L, false, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        assertEquals(35f, s.stamina(), 0.01f);
        assertEquals(1f, s.movementFactor(), 0.001f);
        assertTrue(s.spendStamina(12f));
    }

    @Test public void saveAndRestorePreservesNeedsAndDrainRemainders() {
        SurvivalSystem original = new SurvivalSystem();
        original.loadState("1|100|80|70|70|0|0");
        original.update(40_000L, true, SeasonSystem.SUMMER, SeasonSystem.CLOUDY);
        original.eatFood();
        original.drinkWater();
        SurvivalSystem restored = new SurvivalSystem();
        restored.loadState(original.saveState());
        assertEquals(original.health(), restored.health(), 0.001f);
        assertEquals(original.stamina(), restored.stamina(), 0.001f);
        assertEquals(original.hunger(), restored.hunger(), 0.001f);
        assertEquals(original.thirst(), restored.thirst(), 0.001f);
        assertEquals(original.saveState(), restored.saveState());
    }

    @Test public void malformedOrOutOfRangeSaveDoesNotPartiallyChangeState() {
        SurvivalSystem s = new SurvivalSystem();
        s.update(10_000L, true, SeasonSystem.SPRING, SeasonSystem.SUNNY);
        String before = s.saveState();
        s.loadState("1|100|100|101|50|0|0");
        s.loadState("not-a-save");
        assertEquals(before, s.saveState());
    }
}
