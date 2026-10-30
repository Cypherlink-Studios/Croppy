package dev.darkblade.croppy.api.growth;

import java.util.Objects;

/**
 * Immutable policy configuration governing growth behavior when a crop is unwatered.
 *
 * @param type policy type
 * @param slowRateMultiplier multiplier when type is SLOW (e.g. 0.15)
 * @param decayAmount amount to deduct from progress when type is DECAY_PROGRESS
 * @param maxDryDays maximum consecutive dry days allowed before withering under WITHER type
 */
public record UnwateredPolicy(
        UnwateredPolicyType type,
        double slowRateMultiplier,
        double decayAmount,
        int maxDryDays
) {

    public UnwateredPolicy {
        Objects.requireNonNull(type, "type cannot be null");
    }

    public static UnwateredPolicy pause() {
        return new UnwateredPolicy(UnwateredPolicyType.PAUSE, 0.0, 0.0, Integer.MAX_VALUE);
    }

    public static UnwateredPolicy slow(double slowRateMultiplier) {
        return new UnwateredPolicy(UnwateredPolicyType.SLOW, Math.max(0.0, slowRateMultiplier), 0.0, Integer.MAX_VALUE);
    }

    public static UnwateredPolicy decay(double decayAmount) {
        return new UnwateredPolicy(UnwateredPolicyType.DECAY_PROGRESS, 0.0, Math.max(0.0, decayAmount), Integer.MAX_VALUE);
    }

    public static UnwateredPolicy wither(int maxDryDays) {
        return new UnwateredPolicy(UnwateredPolicyType.WITHER, 0.0, 0.0, Math.max(1, maxDryDays));
    }
}
