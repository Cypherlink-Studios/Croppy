package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Declarative specification of an effect executed when a custom item is consumed.
 *
 * @param type       namespaced effect handler key (e.g. {@code croppy:potion}, {@code croppy:alcohol})
 * @param chance     execution probability in {@code [0.0, 1.0]}
 * @param minQuality minimum item quality grade {@code [0, 3]} required to trigger
 * @param condition  optional boolean expression evaluated against the consumption context
 * @param parameters arbitrary effect configuration parameters
 */
public record ConsumeEffectSpec(
        Key type,
        double chance,
        int minQuality,
        String condition,
        Map<String, Object> parameters
) {
    public ConsumeEffectSpec {
        Objects.requireNonNull(type, "type cannot be null");
        if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("chance must be within [0.0, 1.0]: " + chance);
        }
        if (minQuality < 0 || minQuality > 3) {
            throw new IllegalArgumentException("minQuality must be within [0, 3]: " + minQuality);
        }
        condition = (condition == null || condition.isBlank()) ? null : condition.trim();
        if (parameters == null || parameters.isEmpty()) {
            parameters = Map.of();
        } else {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    sanitized.put(entry.getKey(), entry.getValue());
                }
            }
            parameters = Collections.unmodifiableMap(sanitized);
        }
    }

    public ConsumeEffectSpec(Key type, Map<String, Object> parameters) {
        this(type, 1.0, 0, null, parameters);
    }

    public Optional<String> getCondition() {
        return Optional.ofNullable(condition);
    }
}
