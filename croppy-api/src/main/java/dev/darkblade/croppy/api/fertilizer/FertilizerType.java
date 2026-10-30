package dev.darkblade.croppy.api.fertilizer;

/**
 * Defines the functional category and primary effect of a fertilizer.
 */
public enum FertilizerType {
    /**
     * Multiplies the rate of growth points accumulated per simulation tick.
     */
    SPEED,

    /**
     * Grants a probability that soil retains hydration across world day transitions.
     */
    RETAINING,

    /**
     * Increases probability of harvesting higher-tier quality drops.
     */
    QUALITY
}
