package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class QuestSystemTest {
    @Test public void objectivesUnlockOneTimeRewards() {
        QuestSystem quests = new QuestSystem();
        assertEquals(0, quests.recordWood(4));
        assertEquals(25, quests.recordWood(6));
        assertEquals(0, quests.recordWood(2));
        assertEquals(25, quests.recordStone(6));
        assertEquals(3, quests.recordPlant());
        assertEquals(25, quests.recordHarvest());
    }

    @Test public void journalProgressSurvivesSaveAndLoad() {
        QuestSystem quests = new QuestSystem();
        quests.recordWood(4);
        quests.recordStone(2);
        quests.recordPlant();
        QuestSystem restored = new QuestSystem();
        restored.loadState(quests.saveState());
        assertEquals(4, restored.getWood());
        assertEquals(2, restored.getStone());
        assertTrue(restored.isPlantClaimed());
        assertFalse(restored.isWoodClaimed());
    }

    @Test public void malformedSaveDoesNotCorruptProgress() {
        QuestSystem quests = new QuestSystem();
        quests.recordWood(2);
        quests.loadState("bad|save");
        assertEquals(2, quests.getWood());
        assertFalse(quests.isWoodClaimed());
    }
}
