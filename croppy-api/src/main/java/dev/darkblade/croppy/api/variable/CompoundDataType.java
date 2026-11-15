package dev.darkblade.croppy.api.variable;

import java.util.Optional;

/**
 * Specialization of {@link DataType} representing composite, structured, or indexed
 * multi-property values (e.g., nested compounds, fluid tanks, inventory records).
 *
 * @param <T> the composite value type
 */
public interface CompoundDataType<T> extends DataType<T> {

    @Override
    default boolean isCompound() {
        return true;
    }

    /**
     * Resolves a sub-property or indexed field from the compound value.
     *
     * @param value        the compound value
     * @param propertyPath relative property or index path (e.g., "fluid", "amount", "0")
     * @return optional containing the resolved sub-property value if present
     */
    Optional<Object> getProperty(T value, String propertyPath);
}
