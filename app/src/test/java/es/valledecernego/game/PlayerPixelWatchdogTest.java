package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerPixelWatchdogTest {
    @Test public void recognisesHeadTorsoLegsAndBootsInRenderedHumanoidPixels() {
        int width = 100, height = 100;
        byte[] rgba = new byte[width * height * 4];
        fill(rgba, width, height, 0, 0, width, height, 38, 92, 38);
        fill(rgba, width, height, 42, 10, 58, 29, 180, 115, 80);
        fill(rgba, width, height, 34, 30, 66, 62, 125, 48, 28);
        fill(rgba, width, height, 39, 61, 46, 83, 25, 24, 22);
        fill(rgba, width, height, 54, 61, 61, 83, 25, 24, 22);
        fill(rgba, width, height, 35, 82, 47, 94, 185, 92, 24);
        fill(rgba, width, height, 53, 82, 65, 94, 185, 92, 24);

        PlayerPixelWatchdog.Result result = PlayerPixelWatchdog.inspect(rgba, width, height);
        assertTrue(result.humanoid);
        assertTrue(result.skin >= 40);
        assertTrue(result.jacket >= 80);
        assertTrue(result.boots >= 40);
        assertTrue(result.dark >= 40);
    }

    @Test public void rejectsTheSingleBlueRectangleReportedOnThePhone() {
        int width = 100, height = 100;
        byte[] rgba = new byte[width * height * 4];
        fill(rgba, width, height, 0, 0, width, height, 35, 90, 210);
        fill(rgba, width, height, 35, 25, 65, 85, 35, 90, 210);

        PlayerPixelWatchdog.Result result = PlayerPixelWatchdog.inspect(rgba, width, height);
        assertFalse(result.humanoid);
        assertEquals(0, result.skin);
        assertEquals(0, result.jacket);
        assertEquals(0, result.boots);
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
