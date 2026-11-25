package es.valledecernego.game;

/** In-game village market using only currency earned by playing. */
public final class MarketSystem {
    public static final int HARVEST_PRICE = 2;
    public static final int SEED_BUNDLE_QUANTITY = 3;
    public static final int SEED_BUNDLE_PRICE = 6;
    private static final int MAX_COINS = 1_000_000;
    private int coins = 0;

    public synchronized int sellHarvest(CropSystem crops) {
        if (crops == null) return 0;
        int sold = crops.takeAllHarvest();
        if (sold > 0) {
            long earned = (long) sold * HARVEST_PRICE;
            coins = (int) Math.min(MAX_COINS, (long) coins + earned);
        }
        return sold;
    }

    public synchronized boolean buySeedBundle(CropSystem crops) {
        if (crops == null || coins < SEED_BUNDLE_PRICE) return false;
        int added = crops.addSeeds(SEED_BUNDLE_QUANTITY);
        if (added != SEED_BUNDLE_QUANTITY) return false;
        coins -= SEED_BUNDLE_PRICE;
        return true;
    }

    public synchronized int getCoins() { return coins; }
    public synchronized String saveState() { return Integer.toString(coins); }

    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        try {
            int loaded = Integer.parseInt(saved);
            if (loaded < 0 || loaded > MAX_COINS) return;
            coins = loaded;
        } catch (NumberFormatException ignored) {
            // Ignore invalid data and keep the current wallet.
        }
    }
}
