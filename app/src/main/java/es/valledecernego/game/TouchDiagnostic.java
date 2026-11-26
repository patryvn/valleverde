package es.valledecernego.game;

import java.util.Locale;

/** Formats HUD diagnostics without mixing integer view dimensions with float conversions. */
public final class TouchDiagnostic {
    private TouchDiagnostic() { }

    public static String format(String event,
                                float x, float y,
                                int viewWidth, int viewHeight,
                                int mode,
                                float joyX, float joyY,
                                float moveX, float moveY) {
        return String.format(Locale.US,
            "HUD_TOUCH %s x=%.1f y=%.1f view=%dx%d mode=%d joy=%.2f,%.2f move=%.2f,%.2f",
            event, x, y, viewWidth, viewHeight, mode,
            joyX, joyY, moveX, moveY);
    }
}
