package dev.darkblade.croppy.api.variable;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Defines a typed data contract for Croppy variables, supporting validation,
 * binary serialization, context projection, and strictly typed mutation operations.
 *
 * @param <T> the Java runtime type representing values of this data type
 */
public interface DataType<T> extends Keyed {

    /**
     * @return the canonical namespaced key of this data type (e.g., "croppy:integer")
     */
    @Override
    Key getKey();

    /**
     * @return the runtime Java class of values managed by this type
     */
    Class<T> getValueClass();

    /**
     * @return the safe default value when no explicit initial value is provided
     */
    T getDefaultValue();

    /**
     * @return true if this type holds composite/indexed data, false for atomic single values
     */
    boolean isCompound();

    /**
     * Parses and validates a raw object (such as from YAML config or string input) into type {@code T}.
     *
     * @param rawInput the unvalidated raw value
     * @return validated value of type {@code T}
     * @throws IllegalArgumentException if the input cannot be converted or fails validation
     */
    T parse(Object rawInput);

    /**
     * Serializes a value of type {@code T} into a binary output stream.
     *
     * @param out   target binary stream
     * @param value value to serialize
     * @throws IOException if an I/O error occurs
     */
    void writeBinary(DataOutput out, T value) throws IOException;

    /**
     * Deserializes a value of type {@code T} from a binary input stream.
     *
     * @param in source binary stream
     * @return deserialized value
     * @throws IOException if an I/O error occurs or data is malformed
     */
    T readBinary(DataInput in) throws IOException;

    /**
     * Exports this variable's value (and optionally formatted or nested sub-properties)
     * into a relative variable map.
     *
     * @param variableName variable key relative to the store namespace
     * @param value        current variable value
     * @param destination  target map receiving relative variable entries
     */
    void exportVariables(String variableName, T value, Map<String, Object> destination);

    /**
     * @return unmodifiable collection of mutation operations supported by this data type
     */
    Collection<VariableOperation<T>> getSupportedOperations();

    /**
     * Looks up a supported operation by its case-insensitive identifier.
     *
     * @param operationId operation identifier (e.g., "ADD", "SET")
     * @return optional containing the matching operation if supported
     */
    default Optional<VariableOperation<T>> getOperation(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            return Optional.empty();
        }
        String normalized = operationId.trim().toUpperCase(Locale.ROOT);
        for (VariableOperation<T> op : getSupportedOperations()) {
            if (op.getId().equalsIgnoreCase(normalized)) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}
