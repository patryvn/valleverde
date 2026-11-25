package es.valledecernego.game;

/**
 * Lightweight world calendar and deterministic seasonal weather.
 * Time advances only while the game renderer is active.
 */
public final class SeasonSystem {
    public static final long SEASON_LENGTH_MS = 180_000L;
    public static final long WEATHER_LENGTH_MS = 45_000L;
    public static final int SPRING = 0, SUMMER = 1, AUTUMN = 2, WINTER = 3;
    public static final int SUNNY = 0, CLOUDY = 1, RAIN = 2, FOG = 3;

    private static final String[] SEASON_NAMES = {"Primavera", "Verano", "Otoño", "Invierno"};
    private static final String[] WEATHER_NAMES = {"Despejado", "Nublado", "Lluvia", "Niebla"};
    private static final int[][] WEATHER_BY_SEASON = {
        {CLOUDY, RAIN, SUNNY, FOG},
        {SUNNY, SUNNY, CLOUDY, RAIN},
        {CLOUDY, RAIN, FOG, RAIN},
        {CLOUDY, FOG, RAIN, SUNNY}
    };

    private int seasonIndex;
    private long seasonElapsedMs;
    private long weatherElapsedMs;
    private int weatherIndex;

    public synchronized void update(long deltaMs) {
        if (deltaMs <= 0L) return;
        long remaining = Math.min(deltaMs, 3_600_000L);
        while (remaining > 0L) {
            long seasonStep = Math.min(remaining, SEASON_LENGTH_MS - seasonElapsedMs);
            long weatherStep = Math.min(seasonStep, WEATHER_LENGTH_MS - weatherElapsedMs);
            seasonElapsedMs += weatherStep;
            weatherElapsedMs += weatherStep;
            remaining -= weatherStep;
            if (weatherElapsedMs >= WEATHER_LENGTH_MS) {
                weatherElapsedMs = 0L;
                weatherIndex = (weatherIndex + 1) % 4;
            }
            if (seasonElapsedMs >= SEASON_LENGTH_MS) {
                seasonElapsedMs = 0L;
                seasonIndex = (seasonIndex + 1) % 4;
                weatherIndex = 0;
            }
        }
    }

    public synchronized int getSeason() {
        return seasonIndex;
    }

    public synchronized int getWeather() {
        return WEATHER_BY_SEASON[getSeason()][weatherIndex];
    }

    public synchronized String getSummary() {
        return SEASON_NAMES[getSeason()] + " · " + WEATHER_NAMES[getWeather()];
    }

    public synchronized float skyRed() {
        switch (getWeather()) {
            case RAIN: return 0.38f;
            case FOG: return 0.62f;
            case CLOUDY: return 0.52f;
            default: return getSeason() == WINTER ? 0.58f : 0.56f;
        }
    }

    public synchronized float skyGreen() {
        switch (getWeather()) {
            case RAIN: return 0.46f;
            case FOG: return 0.68f;
            case CLOUDY: return 0.62f;
            default: return 0.72f;
        }
    }

    public synchronized float skyBlue() {
        switch (getWeather()) {
            case RAIN: return 0.55f;
            case FOG: return 0.72f;
            case CLOUDY: return 0.76f;
            default: return 0.83f;
        }
    }

    public synchronized float foliageRed() {
        switch (getSeason()) {
            case AUTUMN: return 0.66f;
            case WINTER: return 0.40f;
            case SPRING: return 0.20f;
            default: return 0.12f;
        }
    }

    public synchronized float foliageGreen() {
        switch (getSeason()) {
            case AUTUMN: return 0.34f;
            case WINTER: return 0.34f;
            case SPRING: return 0.48f;
            default: return 0.36f;
        }
    }

    public synchronized float foliageBlue() {
        return getSeason() == AUTUMN ? 0.12f : 0.16f;
    }

    public synchronized String saveState() {
        return "1|" + seasonIndex + "|" + seasonElapsedMs + "|" + weatherElapsedMs + "|" + weatherIndex;
    }

    /** Invalid saves are ignored without partially changing the current calendar. */
    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] fields = saved.split("\\|", -1);
        if (fields.length != 5 || !"1".equals(fields[0])) return;
        try {
            int savedSeason = Integer.parseInt(fields[1]);
            long season = Long.parseLong(fields[2]);
            long weather = Long.parseLong(fields[3]);
            int index = Integer.parseInt(fields[4]);
            if (savedSeason < 0 || savedSeason > 3
                || season < 0L || season >= SEASON_LENGTH_MS
                || weather < 0L || weather >= WEATHER_LENGTH_MS
                || index < 0 || index > 3) return;
            seasonIndex = savedSeason;
            seasonElapsedMs = season;
            weatherElapsedMs = weather;
            weatherIndex = index;
        } catch (NumberFormatException ignored) {
            // Keep the current state when a save is malformed.
        }
    }
}
