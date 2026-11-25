package es.valledecernego.game;

import java.util.List;

/** Deterministic selection of the closest active world resource for the touch action button. */
final class ResourceInteraction {
    private ResourceInteraction() {}

    /**
     * Finds the closest active item in the world list.
     * Each item stores x, z, kind and active flag in slots 0..3.
     * A maximum distance is inclusive; use positive infinity for an unrestricted lookup.
     */
    static int nearestIndex(List<float[]> items, float playerX, float playerZ, float maxDistance) {
        if (items == null || items.isEmpty() || maxDistance < 0f) return -1;
        int nearest = -1;
        float bestSquared = maxDistance * maxDistance;
        for (int i = 0; i < items.size(); i++) {
            float[] item = items.get(i);
            if (item == null || item.length < 4 || item[3] <= 0f) continue;
            float dx = playerX - item[0];
            float dz = playerZ - item[1];
            float distanceSquared = dx * dx + dz * dz;
            if (distanceSquared <= bestSquared) {
                bestSquared = distanceSquared;
                nearest = i;
            }
        }
        return nearest;
    }
}
