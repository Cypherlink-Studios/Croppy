package dev.darkblade.croppy.api.variable;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

import java.util.Optional;

/**
 * Central registry managing registered {@link DataType} definitions across core and addon modules.
 */
public interface DataTypeRegistry extends Registry<DataType<?>>, Service {

    /**
     * Retrieves a data type matching both key and expected value class.
     *
     * @param key        data type key
     * @param valueClass expected Java value class
     * @param <T>        value type parameter
     * @return optional containing the typed data type if found and compatible
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<DataType<T>> getTyped(Key key, Class<T> valueClass) {
        if (key == null || valueClass == null) {
            return Optional.empty();
        }
        return get(key)
                .filter(dt -> valueClass.isAssignableFrom(dt.getValueClass()))
                .map(dt -> (DataType<T>) dt);
    }
}
