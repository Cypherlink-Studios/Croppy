package dev.darkblade.croppy.api.variable;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Objects;

/**
 * Declarative specification for an initial variable defined on a station, crop, or entity definition.
 *
 * @param name         variable identifier within the entity's store
 * @param typeKey      canonical data type key (e.g., "croppy:integer", "croppy:boolean")
 * @param initialValue raw initial value parsed by the data type
 */
public record VariableSpec(
        String name,
        Key typeKey,
        Object initialValue
) {
    public VariableSpec {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        Objects.requireNonNull(typeKey, "typeKey cannot be null");
    }
}
