package dev.darkblade.croppy.api.process;

/**
 * Execution and advancement types for recipe phases.
 */
public enum PhaseType {
    /**
     * Automatically advances after duration ticks elapse while conditions hold.
     */
    TIMED,

    /**
     * Suspends execution in AWAITING_INPUT until a player performs a required manual interaction.
     */
    MANUAL_INTERACTION,

    /**
     * Advances as soon as environmental or world condition thresholds are satisfied.
     */
    ENVIRONMENTAL
}
