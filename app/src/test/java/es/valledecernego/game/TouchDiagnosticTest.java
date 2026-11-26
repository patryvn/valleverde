package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class TouchDiagnosticTest {
    @Test public void formatsIntegerViewDimensionsWithoutCrashing() {
        String line = TouchDiagnostic.format("DOWN",
            105.6f, 423.8f, 960, 540, 1,
            0f, 0f, 0f, 0f);

        assertEquals("HUD_TOUCH DOWN x=105.6 y=423.8 view=960x540 mode=1 joy=0.00,0.00 move=0.00,0.00",
            line);
    }

    @Test public void formatsTouchAndMovementValuesWithStableLocale() {
        String line = TouchDiagnostic.format("MOVE",
            151.2f, 423.8f, 960, 540, 1,
            0.75f, -0.25f, 0.75f, -0.25f);

        assertTrue(line.contains("x=151.2 y=423.8"));
        assertTrue(line.contains("view=960x540 mode=1"));
        assertTrue(line.endsWith("joy=0.75,-0.25 move=0.75,-0.25"));
    }
}
