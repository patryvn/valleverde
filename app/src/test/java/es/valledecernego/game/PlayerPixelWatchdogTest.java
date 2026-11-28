package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerPixelWatchdogTest {
    @Test public void recognisesSpatialHeadTorsoBothArmsBothLegsAndBoots() {
        int width = 100, height = 100;
        byte[] rgba = new byte[width * height * 4];
        fill(rgba, width, height, 0, 0, width, height, 38, 92, 38);
        // glReadPixels is bottom-up: the head is near the high-y end.
        fill(rgba, width, height, 42, 58, 58, 78, 180, 115, 80);
        fill(rgba, width, height, 35, 34, 65, 68, 125, 48, 28);
        fill(rgba, width, height, 26, 38, 39, 65, 125, 48, 28);
        fill(rgba, width, height, 61, 38, 74, 65, 125, 48, 28);
        fill(rgba, width, height, 42, 14, 49, 39, 25, 24, 22);
        fill(rgba, width, height, 51, 14, 58, 39, 25, 24, 22);
        fill(rgba, width, height, 35, 5, 47, 20, 185, 92, 24);
        fill(rgba, width, height, 53, 5, 65, 20, 185, 92, 24);

        PlayerPixelWatchdog.Result result = PlayerPixelWatchdog.inspect(rgba, width, height);
        assertTrue(result.humanoid);
        assertTrue(result.skin >= 10);
        assertTrue(result.jacket >= 20);
        assertTrue(result.boots >= 9);
        assertTrue(result.dark >= 11);
        assertTrue(result.leftArm >= 4);
        assertTrue(result.rightArm >= 4);
        assertTrue(result.leftLeg >= 4);
        assertTrue(result.rightLeg >= 4);
    }

    @Test public void rejectsTheSingleBlueRectangleReportedOnThePhone() {
        int width = 100, height = 100;
        byte[] rgba = new byte[width * height * 4];
        fill(rgba, width, height, 0, 0, width, height, 35, 90, 210);
        fill(rgba, width, height, 35, 20, 65, 85, 35, 90, 210);

        PlayerPixelWatchdog.Result result = PlayerPixelWatchdog.inspect(rgba, width, height);
        assertFalse(result.humanoid);
        assertEquals(0, result.skin);
        assertEquals(0, result.jacket);
        assertEquals(0, result.boots);
        assertEquals(0, result.leftArm);
        assertEquals(0, result.rightArm);
        assertEquals(0, result.leftLeg);
        assertEquals(0, result.rightLeg);
    }

    @Test public void rejectsHumanoidColourDecoysOutsideThePlayerProjection() {
        int width = 100, height = 100;
        byte[] rgba = new byte[width * height * 4];
        fill(rgba, width, height, 0, 0, width, height, 38, 92, 38);
        fill(rgba, width, height, 2, 58, 19, 78, 180, 115, 80);
        fill(rgba, width, height, 2, 34, 19, 68, 125, 48, 28);
        fill(rgba, width, height, 2, 5, 19, 20, 185, 92, 24);
        fill(rgba, width, height, 81, 14, 98, 39, 25, 24, 22);
        fill(rgba, width, height, 35, 20, 65, 85, 35, 90, 210);

        PlayerPixelWatchdog.Result result = PlayerPixelWatchdog.inspect(rgba, width, height);
        assertFalse(result.humanoid);
    }

    @Test public void sampleRegionScalesForHighResolutionLandscapePhonesAndSmallEmulators() {
        assertEquals(216, PlayerPixelWatchdog.sampleWidth(720));
        assertEquals(243, PlayerPixelWatchdog.sampleHeight(405));
        assertEquals(720, PlayerPixelWatchdog.sampleWidth(2712));
        assertEquals(640, PlayerPixelWatchdog.sampleHeight(1220));
        assertEquals(108, PlayerPixelWatchdog.sampleWidth(360));
        assertEquals(121, PlayerPixelWatchdog.sampleHeight(203));
        assertEquals(0, PlayerPixelWatchdog.sampleWidth(0));
        assertEquals(0, PlayerPixelWatchdog.sampleHeight(-1));
    }

    @Test public void rejectsNullMalformedAndEmptyReadbacksWithoutThrowing() {
        assertFalse(PlayerPixelWatchdog.inspect(null, 100, 100).humanoid);
        assertFalse(PlayerPixelWatchdog.inspect(new byte[0], 100, 100).humanoid);
        assertFalse(PlayerPixelWatchdog.inspect(new byte[16], 0, 4).humanoid);
        assertFalse(PlayerPixelWatchdog.inspect(new byte[16], 4, 4).humanoid);
    }

    private static void fill(byte[] rgba, int width, int height,
                             int x0, int y0, int x1, int y1,
                             int red, int green, int blue) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int offset = (y * width + x) * 4;
                rgba[offset] = (byte) red;
                rgba[offset + 1] = (byte) green;
                rgba[offset + 2] = (byte) blue;
                rgba[offset + 3] = (byte) 255;
            }
        }
    }
}
