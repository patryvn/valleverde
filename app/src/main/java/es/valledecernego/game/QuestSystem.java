package es.valledecernego.game;

/** Guided first-session objectives. Progress and claimed rewards survive app restarts. */
public final class QuestSystem {
    public static final int WOOD_TARGET = 10;
    public static final int STONE_TARGET = 6;
    public static final int COIN_REWARD = 25;
    public static final int SEED_REWARD = 3;

    private int wood;
    private int stone;
    private int planted;
    private int harvested;
    private boolean woodClaimed;
    private boolean stoneClaimed;
    private boolean plantedClaimed;
    private boolean harvestClaimed;

    /** Returns the coin reward earned by this event; zero when no reward was unlocked. */
    public synchronized int recordWood(int amount) {
        if (amount > 0) wood = Math.min(WOOD_TARGET, wood + amount);
        if (!woodClaimed && wood >= WOOD_TARGET) { woodClaimed = true; return COIN_REWARD; }
        return 0;
    }

    public synchronized int recordStone(int amount) {
        if (amount > 0) stone = Math.min(STONE_TARGET, stone + amount);
        if (!stoneClaimed && stone >= STONE_TARGET) { stoneClaimed = true; return COIN_REWARD; }
        return 0;
    }

    public synchronized int recordPlant() {
        planted = Math.min(1, planted + 1);
        if (!plantedClaimed && planted >= 1) { plantedClaimed = true; return SEED_REWARD; }
        return 0;
    }

    public synchronized int recordHarvest() {
        harvested = Math.min(1, harvested + 1);
        if (!harvestClaimed && harvested >= 1) { harvestClaimed = true; return COIN_REWARD; }
        return 0;
    }

    public synchronized int getWood() { return wood; }
    public synchronized int getStone() { return stone; }
    public synchronized boolean isWoodClaimed() { return woodClaimed; }
    public synchronized boolean isStoneClaimed() { return stoneClaimed; }
    public synchronized boolean isPlantClaimed() { return plantedClaimed; }
    public synchronized boolean isHarvestClaimed() { return harvestClaimed; }

    public synchronized String saveState() {
        return wood + "|" + stone + "|" + planted + "|" + harvested + "|"
            + bit(woodClaimed) + "|" + bit(stoneClaimed) + "|"
            + bit(plantedClaimed) + "|" + bit(harvestClaimed);
    }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] p = saved.split("\\|", -1);
        if (p.length != 8) return;
        try {
            int w = Integer.parseInt(p[0]), s = Integer.parseInt(p[1]);
            int pl = Integer.parseInt(p[2]), h = Integer.parseInt(p[3]);
            if (w < 0 || w > WOOD_TARGET || s < 0 || s > STONE_TARGET
                || pl < 0 || pl > 1 || h < 0 || h > 1) return;
            boolean wc = parseBit(p[4]), sc = parseBit(p[5]);
            boolean plc = parseBit(p[6]), hc = parseBit(p[7]);
            if ((wc && w < WOOD_TARGET) || (sc && s < STONE_TARGET)
                || (plc && pl < 1) || (hc && h < 1)) return;
            wood=w; stone=s; planted=pl; harvested=h;
            woodClaimed=wc; stoneClaimed=sc; plantedClaimed=plc; harvestClaimed=hc;
        } catch (NumberFormatException ignored) {
            // Keep the current quest state when a save is malformed.
        }
    }

    public synchronized String getJournal() {
        return "DIARIO DEL VALLE\n\n"
            + (woodClaimed ? "✓" : "○") + " Recoge madera: " + wood + "/" + WOOD_TARGET
            + "  · Recompensa: " + COIN_REWARD + " monedas\n"
            + (stoneClaimed ? "✓" : "○") + " Recoge piedra: " + stone + "/" + STONE_TARGET
            + "  · Recompensa: " + COIN_REWARD + " monedas\n"
            + (plantedClaimed ? "✓" : "○") + " Planta tu primer cultivo"
            + "  · Recompensa: " + SEED_REWARD + " semillas\n"
            + (harvestClaimed ? "✓" : "○") + " Recoge tu primera cosecha"
            + "  · Recompensa: " + COIN_REWARD + " monedas\n\n"
            + "Siguiente objetivo: reúne recursos y prepara tu primera construcción.";
    }

    private static int bit(boolean value) { return value ? 1 : 0; }
    private static boolean parseBit(String value) {
        if ("0".equals(value)) return false;
        if ("1".equals(value)) return true;
        throw new NumberFormatException("Invalid boolean");
    }
}
