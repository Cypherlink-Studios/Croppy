package dev.darkblade.croppy.api.variable;

import java.util.Map;

/**
 * Specialization of {@link DataType} representing atomic single-value types
 * (e.g., integer, decimal, boolean, string).
 *
 * @param <T> the atomic value type
 */
public interface SimpleDataType<T> extends DataType<T> {

    @Override
    default boolean isCompound() {
        return false;
    }

    /**
     * Returns an optional human-readable formatted string representation of the value.
     * Subclasses may override to provide unit suffixes or custom formatting.
     *
     * @param value current value
     * @return formatted display string
     */
    default String formatValue(T value) {
        return value != null ? value.toString() : "";
    }

    @Override
    default void exportVariables(String variableName, T value, Map<String, Object> destination) {
        if (variableName != null && value != null) {
            destination.put(variableName, value);
            String formatted = formatValue(value);
            if (formatted != null) {
                destination.put(variableName + "_formatted", formatted);
            }
        }
    }
}
