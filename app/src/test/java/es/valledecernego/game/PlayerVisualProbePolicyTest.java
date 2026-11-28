package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerVisualProbePolicyTest {
    @Test public void doesNotProbeAgainWhilePlayerIsStationary() {
        assertFalse(PlayerVisualProbePolicy.shouldReprobe(true, false,
            0f, 5f, 0f, 3_000_000_000L, 0f, 5f, 0f, 1_000_000_000L));
    }

    @Test public void reprobesAfterMeaningfulPlayerTranslation() {
        assertTrue(PlayerVisualProbePolicy.shouldReprobe(true, false,
            .65f, 5f, 0f, 1_100_000_000L, 0f, 5f, 0f, 1_000_000_000L));
        assertFalse(PlayerVisualProbePolicy.shouldReprobe(true, false,
            .64f, 5f, 0f, 1_100_000_000L, 0f, 5f, 0f, 1_000_000_000L));
    }

    @Test public void reprobesAfterMeaningfulTurn() {
        assertTrue(PlayerVisualProbePolicy.shouldReprobe(true, false,
            0f, 5f, .35f, 1_100_000_000L, 0f, 5f, 0f, 1_000_000_000L));
    }

    @Test public void wrapsFacingDeltaAcrossPiWithoutFalsePositive() {
        assertFalse(PlayerVisualProbePolicy.shouldReprobe(true, false,
            0f, 5f, -3.12f, 1_100_000_000L, 0f, 5f, 3.12f, 1_000_000_000L));
    }

    @Test public void movingPlayerIsSampledAtTwoSecondIntervals() {
        assertFalse(PlayerVisualProbePolicy.shouldReprobe(true, true,
            0f, 5f, 0f, 2_999_999_999L, 0f, 5f, 0f, 1_000_000_000L));
        assertTrue(PlayerVisualProbePolicy.shouldReprobe(true, true,
            0f, 5f, 0f, 3_000_000_000L, 0f, 5f, 0f, 1_000_000_000L));
    }

    @Test public void unavailableReadbackDoesNotScheduleRepeatedProbes() {
        assertFalse(PlayerVisualProbePolicy.shouldReprobe(false, true,
            2f, 5f, 1f, 9_000_000_000L, 0f, 5f, 0f, 1_000_000_000L));
    }

    @Test public void invalidCoordinatesForceAnotherDiagnosticAttempt() {
        assertTrue(PlayerVisualProbePolicy.shouldReprobe(true, false,
            Float.NaN, 5f, 0f, 2_000_000_000L, 0f, 5f, 0f, 1_000_000_000L));
    }
}
