package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarketSystemTest {
    private CropSystem createHarvest() {
        CropSystem crops = new CropSystem();
        assertEquals(CropSystem.PLANTED_RESULT, crops.interact(-4.0f, 1.0f));
        assertEquals(CropSystem.WATERED, crops.interact(-4.0f, 1.0f));
        crops.update(CropSystem.GROWTH_DURATION_MS);
        assertEquals(CropSystem.HARVESTED, crops.interact(-4.0f, 1.0f));
        return crops;
    }

    @Test public void sellingHarvestPaysCoinsAndClearsStock() {
        CropSystem crops = createHarvest();
        MarketSystem market = new MarketSystem();
        assertEquals(3, market.sellHarvest(crops));
        assertEquals(156, market.getCoins());
        assertEquals(0, crops.getHarvest());
        assertEquals(0, market.sellHarvest(crops));
        assertEquals(6, market.getCoins());
    }

    @Test public void earnedCoinsCanBuySeedsForTheNextCropCycle() {
        CropSystem crops = createHarvest();
        MarketSystem market = new MarketSystem();
        market.sellHarvest(crops);
        assertTrue(market.buySeedBundle(crops));
        assertEquals(150, market.getCoins());
        assertEquals(9, crops.getSeeds());
    }

    @Test public void purchaseFailsWithoutEnoughCoinsAndDoesNotChangeSeeds() {
        CropSystem crops = new CropSystem();
        MarketSystem market = new MarketSystem();
        assertFalse(market.buySeedBundle(crops));
        assertEquals(6, crops.getSeeds());
        assertEquals(0, market.getCoins());
    }

    @Test public void walletSurvivesSaveAndRejectsInvalidValues() {
        CropSystem crops = createHarvest();
        MarketSystem market = new MarketSystem();
        market.sellHarvest(crops);
        MarketSystem restored = new MarketSystem();
        restored.loadState(market.saveState());
        assertEquals(156, restored.getCoins());
        restored.loadState("-4");
        assertEquals(6, restored.getCoins());
        restored.loadState("not-a-number");
        assertEquals(6, restored.getCoins());
    }
}
