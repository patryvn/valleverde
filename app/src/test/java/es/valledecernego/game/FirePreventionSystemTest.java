package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class FirePreventionSystemTest {
    @Test public void newWorldStartsWithNoSectionsAndAVisibleRiskIndex() {
        FirePreventionSystem system = new FirePreventionSystem();
        assertEquals(0, system.getSectionsBuilt());
        assertEquals(80, system.getRiskIndex(SeasonSystem.SUMMER, SeasonSystem.SUNNY));
        assertEquals("alto", FirePreventionSystem.riskLabel(80));
    }

    @Test public void sectionRequiresBothMaterialsAndDoesNotMutateOnFailure() {
        FirePreventionSystem system = new FirePreventionSystem();
        assertEquals(FirePreventionSystem.INSUFFICIENT_MATERIALS, system.buildSection(2, 0));
        assertEquals(FirePreventionSystem.INSUFFICIENT_MATERIALS, system.buildSection(1, 1));
        assertEquals(0, system.getSectionsBuilt());
        assertEquals(FirePreventionSystem.BUILT, system.buildSection(2, 1));
        assertEquals(1, system.getSectionsBuilt());
    }

    @Test public void constructionStopsAtFourSections() {
        FirePreventionSystem system = new FirePreventionSystem();
        for (int i = 0; i < FirePreventionSystem.MAX_SECTIONS; i++) {
            assertEquals(FirePreventionSystem.BUILT, system.buildSection(2, 1));
        }
        assertEquals(FirePreventionSystem.COMPLETE, system.buildSection(99, 99));
        assertEquals(FirePreventionSystem.MAX_SECTIONS, system.getSectionsBuilt());
    }

    @Test public void drySummerIsRiskierThanRainySpring() {
        int summerSunny = FirePreventionSystem.riskIndex(
            SeasonSystem.SUMMER, SeasonSystem.SUNNY, 0);
        int springRain = FirePreventionSystem.riskIndex(
            SeasonSystem.SPRING, SeasonSystem.RAIN, 0);
        assertTrue(summerSunny > springRain);
        assertEquals(80, summerSunny);
        assertEquals(12, springRain);
    }

    @Test public void eachCompletedSectionReducesRiskByFourteenPoints() {
        int before = FirePreventionSystem.riskIndex(
            SeasonSystem.SUMMER, SeasonSystem.SUNNY, 0);
        int after = FirePreventionSystem.riskIndex(
            SeasonSystem.SUMMER, SeasonSystem.SUNNY, 1);
        assertEquals(14, before - after);
        assertEquals(24, FirePreventionSystem.riskIndex(
            SeasonSystem.SUMMER, SeasonSystem.SUNNY, 4));
    }

    @Test public void riskIndexIsClampedAndInvalidSectionCountsAreSafe() {
        assertEquals(0, FirePreventionSystem.riskIndex(
            SeasonSystem.WINTER, SeasonSystem.RAIN, 99));
        assertEquals(80, FirePreventionSystem.riskIndex(
            SeasonSystem.SUMMER, SeasonSystem.SUNNY, -5));
        assertEquals("bajo", FirePreventionSystem.riskLabel(29));
        assertEquals("medio", FirePreventionSystem.riskLabel(30));
        assertEquals("alto", FirePreventionSystem.riskLabel(60));
    }

    @Test public void saveRoundTripRestoresSectionsAndMalformedStateIsIgnored() {
        FirePreventionSystem original = new FirePreventionSystem();
        original.buildSection(2, 1);
        original.buildSection(2, 1);
        String saved = original.saveState();
        assertEquals("1|2", saved);

        FirePreventionSystem restored = new FirePreventionSystem();
        restored.loadState(saved);
        assertEquals(2, restored.getSectionsBuilt());

        restored.loadState("1|5");
        restored.loadState("1|not-a-number");
        restored.loadState("0|3");
        assertEquals(2, restored.getSectionsBuilt());
    }

    @Test public void worldContainsExactlyOneFirePreventionBoard() {
        assertEquals(1, WorldLayout.countType(WorldLayout.FIRE_PREVENTION_BOARD));
    }
}
