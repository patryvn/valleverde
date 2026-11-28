package es.valledecernego.game;

/**
 * Spatial CPU-side visual check for the player region read back from GLES2.
 *
 * glReadPixels returns rows from the bottom of the framebuffer. The probe is
 * deliberately cropped around the camera target and checks the expected
 * vertical/horizontal zones of a humanoid, not just aggregate colours across
 * the whole scene. This prevents a cow, villager, crop or UI/background patch
 * from making a blue player rectangle pass the watchdog.
 */
public final class PlayerPixelWatchdog {
    private static final int MIN_SKIN_PIXELS = 20;
    private static final int MIN_JACKET_PIXELS = 40;
    private static final int MIN_BOOT_PIXELS = 18;
    private static final int MIN_DARK_PIXELS = 22;
    private static final int MIN_LEFT_ARM_PIXELS = 8;
    private static final int MIN_RIGHT_ARM_PIXELS = 8;
    private static final int MIN_LEFT_LEG_PIXELS = 8;
    private static final int MIN_RIGHT_LEG_PIXELS = 8;

    private PlayerPixelWatchdog() { }

    /** Crop a narrow central band: the third-person camera keeps the avatar near screen centre. */
    public static int sampleWidth(int surfaceWidth) {
        if (surfaceWidth <= 0) return 0;
        return Math.min(surfaceWidth, Math.min(720, Math.max(96, surfaceWidth * 30 / 100)));
    }

    /** Include the full head-to-boots span while excluding most scenery above and below it. */
    public static int sampleHeight(int surfaceHeight) {
        if (surfaceHeight <= 0) return 0;
        return Math.min(surfaceHeight, Math.min(640, Math.max(96, surfaceHeight * 60 / 100)));
    }

    public static final class Result {
        public final int skin, jacket, boots, dark;
        public final int leftArm, rightArm, leftLeg, rightLeg;
        public final boolean humanoid;

        private Result(int skin, int jacket, int boots, int dark,
                       int leftArm, int rightArm, int leftLeg, int rightLeg,
                       float scale) {
            this.skin = skin;
            this.jacket = jacket;
            this.boots = boots;
            this.dark = dark;
            this.leftArm = leftArm;
            this.rightArm = rightArm;
            this.leftLeg = leftLeg;
            this.rightLeg = rightLeg;
            this.humanoid = skin >= MIN_SKIN_PIXELS * scale
                && jacket >= MIN_JACKET_PIXELS * scale
                && boots >= MIN_BOOT_PIXELS * scale
                && dark >= MIN_DARK_PIXELS * scale
                && leftArm >= MIN_LEFT_ARM_PIXELS * scale
                && rightArm >= MIN_RIGHT_ARM_PIXELS * scale
                && leftLeg >= MIN_LEFT_LEG_PIXELS * scale
                && rightLeg >= MIN_RIGHT_LEG_PIXELS * scale;
        }
    }

    /** Inspect tightly packed RGBA pixels in OpenGL's bottom-up row order. */
    public static Result inspect(byte[] rgba, int width, int height) {
        if (rgba == null || width <= 0 || height <= 0
                || (long) width * height * 4L > rgba.length) {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0.5f);
        }

        int skin = 0, jacket = 0, boots = 0, dark = 0;
        int leftArm = 0, rightArm = 0, leftLeg = 0, rightLeg = 0;
        int skinX0 = percent(width, 30), skinX1 = percent(width, 70);
        int skinY0 = percent(height, 55), skinY1 = percent(height, 84);
        int jacketX0 = percent(width, 25), jacketX1 = percent(width, 75);
        int jacketY0 = percent(height, 28), jacketY1 = percent(height, 70);
        int bootsY0 = percent(height, 5), bootsY1 = percent(height, 32);
        int darkX0 = percent(width, 30), darkX1 = percent(width, 70);
        int darkY0 = percent(height, 10), darkY1 = percent(height, 46);
        int leftArmX0 = percent(width, 23), leftArmX1 = percent(width, 44);
        int rightArmX0 = percent(width, 56), rightArmX1 = percent(width, 77);
        int armY0 = percent(height, 28), armY1 = percent(height, 70);
        int leftLegX0 = percent(width, 38), leftLegX1 = percent(width, 49);
        int rightLegX0 = percent(width, 51), rightLegX1 = percent(width, 62);
        int legY0 = percent(height, 10), legY1 = percent(height, 46);

        int length = width * height * 4;
        for (int offset = 0; offset < length; offset += 4) {
            int pixel = offset / 4;
            int x = pixel % width;
            int y = pixel / width;
            int r = rgba[offset] & 0xff;
            int g = rgba[offset + 1] & 0xff;
            int b = rgba[offset + 2] & 0xff;

            boolean skinPixel = r >= 115 && r <= 245 && g >= 45 && g <= 175
                && b >= 20 && b <= 135 && r > g * 1.15f && g > b * 1.10f;
            boolean jacketPixel = r > g * 1.30f && r > b * 1.30f
                && r < 165 && g < 115 && b < 100;
            boolean bootPixel = r >= 90 && r <= 235 && g >= 25 && g <= 155
                && b < 105 && r > g * 1.18f;
            boolean darkPixel = Math.max(r, Math.max(g, b)) < 58;

            if (skinPixel && in(x, y, skinX0, skinX1, skinY0, skinY1)) skin++;
            if (jacketPixel && in(x, y, jacketX0, jacketX1, jacketY0, jacketY1)) jacket++;
            if (bootPixel && in(x, y, jacketX0, jacketX1, bootsY0, bootsY1)) boots++;
            if (darkPixel && in(x, y, darkX0, darkX1, darkY0, darkY1)) dark++;
            if (jacketPixel && in(x, y, leftArmX0, leftArmX1, armY0, armY1)) leftArm++;
            if (jacketPixel && in(x, y, rightArmX0, rightArmX1, armY0, armY1)) rightArm++;
            if (darkPixel && in(x, y, leftLegX0, leftLegX1, legY0, legY1)) leftLeg++;
            if (darkPixel && in(x, y, rightLegX0, rightLegX1, legY0, legY1)) rightLeg++;
        }

        float scale = thresholdScale(width, height);
        return new Result(skin, jacket, boots, dark, leftArm, rightArm, leftLeg, rightLeg, scale);
    }

    private static int percent(int value, int percent) { return value * percent / 100; }

    private static boolean in(int x, int y, int x0, int x1, int y0, int y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    private static float thresholdScale(int width, int height) {
        if (width <= 0 || height <= 0) return 0.5f;
        return Math.max(0.5f, Math.min(2f, Math.min(width, height) / 200f));
    }
}
