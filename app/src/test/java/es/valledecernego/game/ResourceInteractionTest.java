package es.valledecernego.game;

import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class ResourceInteractionTest {
    @Test public void selectsNearestActiveResourceWithinActionRange() {
        ArrayList<float[]> items = new ArrayList<>();
        items.add(new float[]{2f, 0f, 1f, 1f});
        items.add(new float[]{1f, 0f, 2f, 1f});
        assertEquals(1, ResourceInteraction.nearestIndex(items, 0f, 0f, 2.4f));
    }

    @Test public void ignoresCollectedResources() {
        ArrayList<float[]> items = new ArrayList<>();
        items.add(new float[]{.2f, 0f, 1f, 0f});
        items.add(new float[]{1f, 0f, 2f, 1f});
        assertEquals(1, ResourceInteraction.nearestIndex(items, 0f, 0f, 2.4f));
    }

    @Test public void returnsNoTargetWhenOutsideRange() {
        ArrayList<float[]> items = new ArrayList<>();
        items.add(new float[]{3f, 0f, 1f, 1f});
        assertEquals(-1, ResourceInteraction.nearestIndex(items, 0f, 0f, 2.4f));
    }

    @Test public void exactRangeBoundaryCanBeCollected() {
        ArrayList<float[]> items = new ArrayList<>();
        items.add(new float[]{1.8f, 1.6f, 2f, 1f});
        assertEquals(0, ResourceInteraction.nearestIndex(items, 0f, 0f, 2.4f));
    }

    @Test public void unrestrictedLookupFindsNearestForDistanceDisplay() {
        ArrayList<float[]> items = new ArrayList<>();
        items.add(new float[]{8f, 0f, 1f, 1f});
        items.add(new float[]{4f, 0f, 2f, 1f});
        assertEquals(1, ResourceInteraction.nearestIndex(items, 0f, 0f, Float.POSITIVE_INFINITY));
    }
}
