package es.valledecernego.game;

import java.util.Locale;

/** Lightweight survival needs; time advances only while the game is active. */
public final class SurvivalSystem {
    public static final int ATE = 1;
    public static final int NO_FOOD = 2;
    public static final int NOT_HUNGRY = 3;
    public static final long HUNGER_INTERVAL_MS = 120_000L;
    public static final long THIRST_INTERVAL_MS = 90_000L;
    private static final long MAX_UPDATE_MS = 3_600_000L;

    private float health = 100f, stamina = 100f, hunger = 100f, thirst = 100f;
    private long hungerElapsedMs, thirstElapsedMs;

    public synchronized void update(long deltaMs, boolean moving, int season, int weather) {
        if (deltaMs <= 0L) return;
        long safeDelta = Math.min(deltaMs, MAX_UPDATE_MS);
        float seconds = safeDelta / 1000f;
        stamina = clamp(stamina + (moving ? -0.55f : 3.5f) * seconds);

        float hungerRate = season == SeasonSystem.WINTER ? 1.20f
            : season == SeasonSystem.SUMMER ? 0.95f : 1.0f;
        if (moving) hungerRate *= 1.08f;
        hungerElapsedMs += Math.round(safeDelta * hungerRate);
        while (hungerElapsedMs >= HUNGER_INTERVAL_MS) {
            hungerElapsedMs -= HUNGER_INTERVAL_MS;
            hunger = Math.max(0f, hunger - 1f);
        }

        float thirstRate = season == SeasonSystem.SUMMER ? 1.20f : 1.0f;
        if (weather == SeasonSystem.RAIN) thirstRate *= 0.45f;
        if (moving) thirstRate *= 1.12f;
        thirstElapsedMs += Math.round(safeDelta * thirstRate);
        while (thirstElapsedMs >= THIRST_INTERVAL_MS) {
            thirstElapsedMs -= THIRST_INTERVAL_MS;
            thirst = Math.max(0f, thirst - 1f);
        }

        if (hunger <= 0f || thirst <= 0f) {
            health = Math.max(0f, health - seconds * 0.45f);
        } else if (!moving && hunger >= 65f && thirst >= 65f) {
            health = Math.min(100f, health + seconds * 0.08f);
        }
    }

    public synchronized boolean eatFood() {
        if (hunger >= 95f) return false;
        hunger = clamp(hunger + 35f);
        health = clamp(health + 2f);
        return true;
    }

    public synchronized boolean drinkWater() {
        if (thirst >= 95f) return false;
        thirst = clamp(thirst + 40f);
        return true;
    }

    public synchronized boolean spendStamina(float amount) {
        if (!isFinite(amount) || amount < 0f || stamina < amount) return false;
        stamina = clamp(stamina - amount);
        return true;
    }

    public synchronized float movementFactor() {
        if (stamina <= 0f) return 0.60f;
        if (stamina < 15f) return 0.75f;
        if (stamina < 35f) return 0.90f;
        return 1f;
    }

    public synchronized float health() { return health; }
    public synchronized float stamina() { return stamina; }
    public synchronized float hunger() { return hunger; }
    public synchronized float thirst() { return thirst; }

    public synchronized String getSummary() {
        return "Vida " + Math.round(health) + " · Energía " + Math.round(stamina)
            + " · Hambre " + Math.round(hunger) + " · Sed " + Math.round(thirst);
    }

    public synchronized String saveState() {
        return String.format(Locale.US, "1|%.3f|%.3f|%.3f|%.3f|%d|%d",
            health, stamina, hunger, thirst, hungerElapsedMs, thirstElapsedMs);
    }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] fields = saved.split("\\|", -1);
        if (fields.length != 7 || !"1".equals(fields[0])) return;
        try {
            float h = Float.parseFloat(fields[1]), s = Float.parseFloat(fields[2]);
            float food = Float.parseFloat(fields[3]), water = Float.parseFloat(fields[4]);
            long hElapsed = Long.parseLong(fields[5]), tElapsed = Long.parseLong(fields[6]);
            if (!validPercent(h) || !validPercent(s) || !validPercent(food) || !validPercent(water)
                || hElapsed < 0L || hElapsed >= HUNGER_INTERVAL_MS
                || tElapsed < 0L || tElapsed >= THIRST_INTERVAL_MS) return;
            health=h; stamina=s; hunger=food; thirst=water;
            hungerElapsedMs=hElapsed; thirstElapsedMs=tElapsed;
        } catch (NumberFormatException ignored) {
            // Keep the current state when a save is malformed.
        }
    }

    private static boolean validPercent(float value) {
        return isFinite(value) && value >= 0f && value <= 100f;
    }
    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
    private static float clamp(float value) {
        return Math.max(0f, Math.min(100f, value));
    }
}
