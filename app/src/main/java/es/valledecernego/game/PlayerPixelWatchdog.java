package es.valledecernego.game;

/**
 * CPU-side visual sanity check for the player region read back from GLES2.
 * A successful draw call is not enough: some mobile drivers can report no GL
 * error while the avatar is reduced to a single solid rectangle.
 */
public final class PlayerPixelWatchdog {
    private static final int MIN_SKIN_PIXELS = 40;
    private static final int MIN_JACKET_PIXELS = 80;
    private static final int MIN_BOOT_PIXELS = 40;
    private static final int MIN_DARK_PIXELS = 40;

    private PlayerPixelWatchdog() { }

    public static final class Result {
        public final int skin;
        public final int jacket;
        public final int boots;
        public final int dark;
        public final boolean humanoid;

        private Result(int skin, int jacket, int boots, int dark) {
            this.skin = skin;
            this.jacket = jacket;
            this.boots = boots;
            this.dark = dark;
            this.humanoid = skin >= MIN_SKIN_PIXELS
                && jacket >= MIN_JACKET_PIXELS
                && boots >= MIN_BOOT_PIXELS
                && dark >= MIN_DARK_PIXELS;
        }
    }

    /** Pixels are tightly packed RGBA bytes, in either row order. */
    public static Result inspect(byte[] rgba, int width, int height) {
        if (rgba == null || width <= 0 || height <= 0
                || (long) width * height * 4L > rgba.length) {
            return new Result(0, 0, 0, 0);
        }
        int skin = 0, jacket = 0, boots = 0, dark = 0;
        int length = width * height * 4;
        for (int i = 0; i < length; i += 4) {
            int r = rgba[i] & 0xff;
            int g = rgba[i + 1] & 0xff;
            int b = rgba[i + 2] & 0xff;
            if (r >= 115 && r <= 245 && g >= 45 && g <= 175
                    && b >= 20 && b <= 135 && r > g * 1.15f && g > b * 1.10f) {
                skin++;
            }
            if (r > g * 1.30f && r > b * 1.30f && r < 165 && g < 115 && b < 100) {
                jacket++;
            }
            if (r >= 90 && r <= 235 && g >= 25 && g <= 155
                    && b < 105 && r > g * 1.18f) {
                boots++;
            }
            if (Math.max(r, Math.max(g, b)) < 58) dark++;
        }
        return new Result(skin, jacket, boots, dark);
    }
}
