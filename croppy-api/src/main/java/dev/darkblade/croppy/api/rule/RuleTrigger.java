package dev.darkblade.croppy.api.rule;

/**
 * Trigger points where rules can be evaluated.
 */
public enum RuleTrigger {
    STATION_TICK,
    STATION_INTERACT,
    PROCESS_TICK,
    PROCESS_FINISH,
    PROCESS_FAIL
}
