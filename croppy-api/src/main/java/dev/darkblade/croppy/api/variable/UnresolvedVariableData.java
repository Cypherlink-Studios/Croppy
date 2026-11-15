package dev.darkblade.croppy.api.variable;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Objects;

/**
 * Preserves raw binary variable data when an addon-defined {@link DataType}
 * is temporarily unavailable during deserialization.
 *
 * @param name       variable identifier
 * @param typeKey    canonical key of the missing data type
 * @param rawPayload raw binary bytes serialized by the missing data type
 */
public record UnresolvedVariableData(
        String name,
        Key typeKey,
        byte[] rawPayload
) {
    public UnresolvedVariableData {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(typeKey, "typeKey cannot be null");
        rawPayload = (rawPayload == null) ? new byte[0] : rawPayload.clone();
    }

    @Override
    public byte[] rawPayload() {
        return rawPayload.clone();
    }
}
