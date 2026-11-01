package dev.darkblade.croppy.api.fuel;

/**
 * Immutable properties describing a fuel source's combustion characteristics.
 *
 * @param burnTicks            base duration in ticks this fuel burns
 * @param maxAchievableTemp    maximum temperature in Celsius achievable with this fuel
 * @param efficiencyMultiplier combustion efficiency multiplier (higher = slower burn)
 */
public record FuelProperties(
        long burnTicks,
        double maxAchievableTemp,
        double efficiencyMultiplier
) {
    public static final double DEFAULT_MAX_TEMP = 1000.0;
    public static final double DEFAULT_EFFICIENCY = 1.0;

    public FuelProperties {
        if (burnTicks <= 0) {
            throw new IllegalArgumentException("burnTicks must be positive: " + burnTicks);
        }
        if (maxAchievableTemp <= 0) {
            throw new IllegalArgumentException("maxAchievableTemp must be positive: " + maxAchievableTemp);
        }
        if (efficiencyMultiplier <= 0) {
            throw new IllegalArgumentException("efficiencyMultiplier must be positive: " + efficiencyMultiplier);
        }
    }

    public static FuelProperties of(long burnTicks) {
        return new FuelProperties(burnTicks, DEFAULT_MAX_TEMP, DEFAULT_EFFICIENCY);
    }
}
