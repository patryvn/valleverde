package es.valledecernego.game;

/**
 * Lightweight beef-herd care loop for the first playable valley.
 * Game time advances only while the world is running; all state is serializable.
 */
public final class AnimalSystem {
    public static final int NO_ANIMAL = 0;
    public static final int FED = 1;
    public static final int NO_FORAGE = 2;
    public static final int NOT_HUNGRY = 3;

    public static final long BREEDING_READY_MS = 30_000L;
    public static final long GESTATION_DURATION_MS = 120_000L;
    public static final long CALF_GROWTH_DURATION_MS = 180_000L;

    private static final float INTERACTION_DISTANCE = 2.4f;
    private static final float HUNGER_PER_SECOND = 0.15f;
    public static final int MAX_FORAGE = 99;

    private int forage = 0;
    private float cowHunger = 70.0f;
    private float bullHunger = 70.0f;
    private float cowHealth = 100.0f;
    private float bullHealth = 100.0f;
    private long breedingMs = 0L;
    private long gestationMs = 0L;
    private long calfAgeMs = 0L;
    private boolean pregnant = false;
    private boolean calfBorn = false;

    public synchronized int addForage(int amount) {
        if (amount > 0) forage = Math.min(MAX_FORAGE, forage + amount);
        return forage;
    }

    /** Removes loose forage and returns the amount transferred to a building. */
    public synchronized int removeForage(int amount) {
        if (amount <= 0) return 0;
        int removed = Math.min(amount, forage);
        forage -= removed;
        return removed;
    }

    /** Stable feeding consumes stored hay, prioritizing the hungriest adult. */
    public synchronized boolean feedFromStable() {
        float hungriest = Math.max(cowHunger, bullHunger);
        if (hungriest <= 35.0f) return false;
        if (cowHunger >= bullHunger) {
            cowHunger = Math.max(0.0f, cowHunger - 30.0f);
        } else {
            bullHunger = Math.max(0.0f, bullHunger - 30.0f);
        }
        return true;
    }

    public synchronized int feedNearest(float x, float z) {
        float cowDistance = distance(x, z, 3.0f, -1.0f);
        float bullDistance = distance(x, z, 6.0f, -2.0f);
        if (Math.min(cowDistance, bullDistance) > INTERACTION_DISTANCE) return NO_ANIMAL;
        if (forage <= 0) return NO_FORAGE;

        if (cowDistance <= bullDistance) {
            if (cowHunger <= 5.0f) return NOT_HUNGRY;
            cowHunger = Math.max(0.0f, cowHunger - 45.0f);
        } else {
            if (bullHunger <= 5.0f) return NOT_HUNGRY;
            bullHunger = Math.max(0.0f, bullHunger - 45.0f);
        }
        forage--;
        return FED;
    }

    public synchronized void update(long deltaMs) {
        update(deltaMs, SeasonSystem.SPRING, SeasonSystem.CLOUDY);
    }

    /** Cold and wet weather increase the herd's feed demand. */
    public synchronized void update(long deltaMs, int season, int weather) {
        if (deltaMs <= 0L) return;
        float demand = season == SeasonSystem.WINTER ? 1.35f : 1.0f;
        if (weather == SeasonSystem.RAIN || weather == SeasonSystem.FOG) demand *= 1.10f;
        float effectiveDeltaMs = deltaMs * demand;
        float seconds = effectiveDeltaMs / 1000.0f;
        cowHunger = Math.min(100.0f, cowHunger + seconds * HUNGER_PER_SECOND);
        bullHunger = Math.min(100.0f, bullHunger + seconds * HUNGER_PER_SECOND);

        cowHealth = updateHealth(cowHealth, cowHunger, seconds);
        bullHealth = updateHealth(bullHealth, bullHunger, seconds);

        long gestationDeltaMs = pregnant ? Math.round(effectiveDeltaMs) : 0L;
        if (!pregnant && !calfBorn) {
            boolean ready = cowHealth >= 60.0f && bullHealth >= 60.0f
                && cowHunger <= 55.0f && bullHunger <= 55.0f;
            if (ready) {
                long untilPregnancy = BREEDING_READY_MS - breedingMs;
                if (effectiveDeltaMs >= untilPregnancy) {
                    breedingMs = BREEDING_READY_MS;
                    pregnant = true;
                    gestationMs = 0L;
                    gestationDeltaMs = Math.round(effectiveDeltaMs - untilPregnancy);
                } else {
                    breedingMs += Math.round(effectiveDeltaMs);
                    gestationDeltaMs = 0L;
                }
            } else {
                breedingMs = 0L;
                gestationDeltaMs = 0L;
            }
        }

        if (pregnant && gestationDeltaMs > 0L) {
            gestationMs = Math.min(GESTATION_DURATION_MS, gestationMs + gestationDeltaMs);
            if (gestationMs >= GESTATION_DURATION_MS) {
                pregnant = false;
                calfBorn = true;
                calfAgeMs = 0L;
                gestationMs = 0L;
                breedingMs = 0L;
            }
        } else if (calfBorn) {
            calfAgeMs = Math.min(CALF_GROWTH_DURATION_MS, calfAgeMs + Math.round(effectiveDeltaMs));
        }
    }

    private float updateHealth(float health, float hunger, float seconds) {
        if (hunger > 85.0f) return Math.max(0.0f, health - seconds * 0.08f);
        if (hunger < 35.0f) return Math.min(100.0f, health + seconds * 0.02f);
        return health;
    }

    public synchronized int getForage() { return forage; }
    public synchronized float getCowHunger() { return cowHunger; }
    public synchronized float getBullHunger() { return bullHunger; }
    public synchronized float getCowHealth() { return cowHealth; }
    public synchronized float getBullHealth() { return bullHealth; }
    public synchronized long getGestationMs() { return gestationMs; }
    public synchronized boolean isPregnant() { return pregnant; }
    public synchronized boolean hasCalf() { return calfBorn; }
    public synchronized boolean isCalfGrowing() {
        return calfBorn && calfAgeMs < CALF_GROWTH_DURATION_MS;
    }

    public synchronized String getSummary() {
        String stage;
        if (pregnant) stage = "Vaca gestante";
        else if (calfBorn && isCalfGrowing()) stage = "Ternero creciendo";
        else if (calfBorn) stage = "Ternero adulto";
        else if (cowHunger <= 55.0f && bullHunger <= 55.0f) stage = "Preparando reproducción";
        else stage = "Alimenta a vaca y toro";
        return "Forraje " + forage + "  •  Adultos 2  •  " + stage;
    }

    public synchronized String saveState() {
        return forage + "|" + cowHunger + "|" + bullHunger + "|" + cowHealth + "|"
            + bullHealth + "|" + breedingMs + "|" + (pregnant ? 1 : 0) + "|"
            + gestationMs + "|" + (calfBorn ? 1 : 0) + "|" + calfAgeMs;
    }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] parts = saved.split("\\|", -1);
        if (parts.length != 10) return;
        try {
            int loadedForage = Integer.parseInt(parts[0]);
            float loadedCowHunger = Float.parseFloat(parts[1]);
            float loadedBullHunger = Float.parseFloat(parts[2]);
            float loadedCowHealth = Float.parseFloat(parts[3]);
            float loadedBullHealth = Float.parseFloat(parts[4]);
            long loadedBreeding = Long.parseLong(parts[5]);
            boolean loadedPregnant = parseFlag(parts[6]);
            long loadedGestation = Long.parseLong(parts[7]);
            boolean loadedCalf = parseFlag(parts[8]);
            long loadedCalfAge = Long.parseLong(parts[9]);

            if (loadedForage < 0 || loadedForage > MAX_FORAGE
                || !validPercent(loadedCowHunger) || !validPercent(loadedBullHunger)
                || !validPercent(loadedCowHealth) || !validPercent(loadedBullHealth)
                || loadedBreeding < 0 || loadedBreeding > BREEDING_READY_MS
                || loadedGestation < 0 || loadedGestation > GESTATION_DURATION_MS
                || loadedCalfAge < 0 || loadedCalfAge > CALF_GROWTH_DURATION_MS
                || (loadedPregnant && loadedCalf)) return;

            if (loadedPregnant && loadedGestation >= GESTATION_DURATION_MS) return;
            if (!loadedPregnant && loadedGestation != 0L) return;
            if (!loadedCalf && loadedCalfAge != 0L) return;
            if (loadedCalf && loadedBreeding != 0L) return;

            forage = loadedForage;
            cowHunger = loadedCowHunger;
            bullHunger = loadedBullHunger;
            cowHealth = loadedCowHealth;
            bullHealth = loadedBullHealth;
            breedingMs = loadedPregnant ? BREEDING_READY_MS : loadedBreeding;
            pregnant = loadedPregnant;
            gestationMs = loadedGestation;
            calfBorn = loadedCalf;
            calfAgeMs = loadedCalfAge;
        } catch (NumberFormatException ignored) {
            // Invalid save data is ignored; the default herd remains playable.
        }
    }

    private static boolean parseFlag(String value) {
        if ("1".equals(value)) return true;
        if ("0".equals(value)) return false;
        throw new NumberFormatException("Invalid boolean flag");
    }

    private static boolean validPercent(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value)
            && value >= 0.0f && value <= 100.0f;
    }

    private static float distance(float x1, float z1, float x2, float z2) {
        return (float)Math.hypot(x1 - x2, z1 - z2);
    }
}
