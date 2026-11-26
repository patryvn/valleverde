package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class WindowModePolicyTest {
    @Test public void debugTelemetryKeepsSystemBarsVisibleForReliableCiCapture() {
        assertEquals(0, WindowModePolicy.flags(true));
    }

    @Test public void normalGameplayKeepsTheExistingImmersivePresentation() {
        assertEquals(5894 | 1024 | 512 | 4096, WindowModePolicy.flags(false));
        assertNotEquals(0, WindowModePolicy.flags(false));
    }
}
