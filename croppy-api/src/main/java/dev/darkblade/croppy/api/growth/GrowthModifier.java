package dev.darkblade.croppy.api.growth;

/**
 * Functional interface for calculating growth rate multipliers or factors based on crop context.
 */
@FunctionalInterface
public interface GrowthModifier {

    /**
     * Calculates a growth multiplier to apply to the base progress.
     * A value of 1.0 represents standard growth, values greater than 1.0 accelerate growth,
     * values less than 1.0 slow growth, and 0.0 halts growth.
     *
     * @param context the growth evaluation context
     * @return the calculated growth multiplier (should be non-negative)
     */
    double calculateModifier(GrowthContext context);
}
