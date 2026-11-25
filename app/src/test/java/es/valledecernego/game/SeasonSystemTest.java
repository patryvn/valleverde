package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class SeasonSystemTest {
    @Test public void calendarMovesThroughFourSeasons() {
        SeasonSystem seasons = new SeasonSystem();
        assertEquals(SeasonSystem.SPRING, seasons.getSeason());
        seasons.update(SeasonSystem.SEASON_LENGTH_MS);
        assertEquals(SeasonSystem.SUMMER, seasons.getSeason());
        seasons.update(SeasonSystem.SEASON_LENGTH_MS);
        assertEquals(SeasonSystem.AUTUMN, seasons.getSeason());
        seasons.update(SeasonSystem.SEASON_LENGTH_MS);
        assertEquals(SeasonSystem.WINTER, seasons.getSeason());
        seasons.update(SeasonSystem.SEASON_LENGTH_MS);
        assertEquals(SeasonSystem.SPRING, seasons.getSeason());
    }

    @Test public void weatherChangesWithinEachSeason() {
        SeasonSystem seasons = new SeasonSystem();
        assertEquals(SeasonSystem.CLOUDY, seasons.getWeather());
        seasons.update(SeasonSystem.WEATHER_LENGTH_MS);
        assertEquals(SeasonSystem.RAIN, seasons.getWeather());
        assertTrue(seasons.getSummary().contains("Lluvia"));
    }

    @Test public void seasonAndWeatherSurviveSaveRestore() {
        SeasonSystem original = new SeasonSystem();
        original.update(SeasonSystem.SEASON_LENGTH_MS + 50_000L);
        SeasonSystem restored = new SeasonSystem();
        restored.loadState(original.saveState());
        assertEquals(original.getSeason(), restored.getSeason());
        assertEquals(original.getWeather(), restored.getWeather());
        assertEquals(original.getSummary(), restored.getSummary());
    }

    @Test public void invalidSaveDoesNotCorruptCalendar() {
        SeasonSystem seasons = new SeasonSystem();
        seasons.update(12_000L);
        String before = seasons.saveState();
        seasons.loadState("1|999999999|0|9");
        seasons.loadState("broken");
        assertEquals(before, seasons.saveState());
    }

    @Test public void nonPositiveDeltaDoesNotAdvanceTime() {
        SeasonSystem seasons = new SeasonSystem();
        String before = seasons.saveState();
        seasons.update(0L);
        seasons.update(-10L);
        assertEquals(before, seasons.saveState());
    }
}
