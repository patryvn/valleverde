package es.valledecernego.game;

/** Keeps CI screenshots free of Android's immersive-mode confirmation overlay. */
public final class WindowModePolicy {
    private static final int GAME_IMMERSIVE_FLAGS = 5894 | 1024 | 512 | 4096;

    private WindowModePolicy() { }

    /** Debug telemetry uses ordinary window mode; real gameplay remains immersive. */
    public static int flags(boolean debugTelemetry) {
        return debugTelemetry ? 0 : GAME_IMMERSIVE_FLAGS;
    }
}
