package dev.darkblade.croppy.api.effect;

/**
 * Defines how repeated applications of a {@link CustomStatusEffect} combine with an active instance.
 */
public enum StackingPolicy {
    /**
     * Replaces the active instance when applied again.
     */
    REPLACE,
    /**
     * Adds the new duration to the remaining duration (clamped to max duration).
     */
    EXTEND,
    /**
     * Keeps the higher amplifier/duration and upgrades amplifier up to max amplifier.
     */
    UPGRADE,
    /**
     * Accumulates intensity up to max intensity while refreshing duration.
     */
    STACK_INTENSITY
}
