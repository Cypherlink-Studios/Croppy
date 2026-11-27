package dev.darkblade.croppy.api.effect;

import java.util.Objects;

/**
 * Threshold stage descriptor for intensity-driven {@link CustomStatusEffect}s (e.g. Tipsy, Drunk).
 *
 * @param id           stable stage identifier (e.g. {@code "tipsy"})
 * @param displayName  human-readable display label (e.g. {@code "Tipsy"})
 * @param minIntensity minimum intensity required to enter this stage
 * @param priority     tie-breaking priority when stages share a threshold
 */
public record StatusEffectStage(
        String id,
        String displayName,
        double minIntensity,
        int priority
) {
    public StatusEffectStage {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        if (!Double.isFinite(minIntensity) || minIntensity < 0.0) {
            throw new IllegalArgumentException("minIntensity must be finite and non-negative: " + minIntensity);
        }
    }
}
