package es.valledecernego.game;

/**
 * Small, persistent fire-prevention loop for the first playable valley.
 * The risk index is a game mechanic based on season, weather and completed
 * firebreak sections; it is not a real wildfire forecast or spread simulation.
 */
public final class FirePreventionSystem {
    public static final int MAX_SECTIONS = 4;
    public static final int WOOD_PER_SECTION = 2;
    public static final int STONE_PER_SECTION = 1;

    public static final int BUILT = 1;
    public static final int INSUFFICIENT_MATERIALS = 2;
    public static final int COMPLETE = 3;

    private static final int RISK_REDUCTION_PER_SECTION = 14;
    private int sectionsBuilt;

    public synchronized int buildSection(int availableWood, int availableStone) {
        if (sectionsBuilt >= MAX_SECTIONS) return COMPLETE;
        if (availableWood < WOOD_PER_SECTION || availableStone < STONE_PER_SECTION) {
            return INSUFFICIENT_MATERIALS;
        }
        sectionsBuilt++;
        return BUILT;
    }

    public synchronized int getSectionsBuilt() {
        return sectionsBuilt;
    }

    public synchronized int getRiskIndex(int season, int weather) {
        return riskIndex(season, weather, sectionsBuilt);
    }

    /**
     * Deterministic game-only risk abstraction. Drier seasons and sunny weather
     * raise the index; rain and completed firebreak sections lower it.
     */
    public static int riskIndex(int season, int weather, int completedSections) {
        int seasonalBase;
        switch (season) {
            case SeasonSystem.SUMMER: seasonalBase = 62; break;
            case SeasonSystem.AUTUMN: seasonalBase = 44; break;
            case SeasonSystem.WINTER: seasonalBase = 24; break;
            case SeasonSystem.SPRING:
            default: seasonalBase = 34; break;
        }

        int weatherAdjustment;
        switch (weather) {
            case SeasonSystem.SUNNY: weatherAdjustment = 18; break;
            case SeasonSystem.CLOUDY: weatherAdjustment = 8; break;
            case SeasonSystem.RAIN: weatherAdjustment = -22; break;
            case SeasonSystem.FOG: weatherAdjustment = 3; break;
            default: weatherAdjustment = 0; break;
        }

        int sections = Math.max(0, Math.min(MAX_SECTIONS, completedSections));
        return clamp(seasonalBase + weatherAdjustment
            - sections * RISK_REDUCTION_PER_SECTION, 0, 100);
    }

    public synchronized String getSummary(int season, int weather) {
        int risk = getRiskIndex(season, weather);
        return "Cortafuegos " + sectionsBuilt + "/" + MAX_SECTIONS
            + " · Riesgo " + riskLabel(risk) + " (" + risk + "/100)";
    }

    public static String riskLabel(int risk) {
        if (risk >= 60) return "alto";
        if (risk >= 30) return "medio";
        return "bajo";
    }

    public synchronized String saveState() {
        return "1|" + sectionsBuilt;
    }

    /** Invalid saves are ignored atomically, preserving the current progress. */
    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] fields = saved.split("\\|", -1);
        if (fields.length != 2 || !"1".equals(fields[0])) return;
        try {
            int loadedSections = Integer.parseInt(fields[1]);
            if (loadedSections < 0 || loadedSections > MAX_SECTIONS) return;
            sectionsBuilt = loadedSections;
        } catch (NumberFormatException ignored) {
            // Keep the current state when a save is malformed.
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
