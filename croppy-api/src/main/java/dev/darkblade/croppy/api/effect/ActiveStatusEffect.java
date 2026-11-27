package dev.darkblade.croppy.api.effect;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of an active custom status effect applied to a player.
 *
 * @param effectKey            effect definition key
 * @param amplifier            effect level / amplifier ({@code >= 0})
 * @param intensity            continuous intensity value ({@code >= 0.0})
 * @param remainingTicks       remaining duration in game ticks ({@code >= 0})
 * @param maxTicks             initial or maximum duration in ticks ({@code >= 0})
 * @param appliedAtEpochMillis epoch milliseconds when first applied
 * @param lastTickEpochMillis  epoch milliseconds of the most recent update
 * @param data                 arbitrary immutable per-instance metadata
 */
public record ActiveStatusEffect(
        Key effectKey,
        int amplifier,
        double intensity,
        long remainingTicks,
        long maxTicks,
        long appliedAtEpochMillis,
        long lastTickEpochMillis,
        Map<String, Object> data
) {
    public ActiveStatusEffect {
        Objects.requireNonNull(effectKey, "effectKey cannot be null");
        if (amplifier < 0) {
            throw new IllegalArgumentException("amplifier cannot be negative: " + amplifier);
        }
        if (!Double.isFinite(intensity) || intensity < 0.0) {
            throw new IllegalArgumentException("intensity must be finite and non-negative: " + intensity);
        }
        if (remainingTicks < 0L) {
            remainingTicks = 0L;
        }
        if (maxTicks < 0L) {
            maxTicks = 0L;
        }
        if (data == null || data.isEmpty()) {
            data = Map.of();
        } else {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
            data = Collections.unmodifiableMap(copy);
        }
    }

    public boolean isExpired(boolean intensityDriven) {
        return intensityDriven ? intensity <= 0.0 : remainingTicks <= 0L;
    }

    public long remainingSeconds() {
        return Math.max(0L, (remainingTicks + 19L) / 20L);
    }
}
