package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.variable.DataType;
import dev.darkblade.croppy.api.variable.DataTypeRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Thread-safe default implementation of {@link DataTypeRegistry}.
 */
public final class DefaultDataTypeRegistry extends SimpleRegistry<DataType<?>> implements DataTypeRegistry {

    public DefaultDataTypeRegistry() {
        registerDefaults();
    }

    /**
     * Registers Croppy's built-in primitive and compound data types.
     */
    public void registerDefaults() {
        registerIfAbsent(IntegerDataType.INSTANCE);
        registerIfAbsent(DecimalDataType.INSTANCE);
        registerIfAbsent(BooleanDataType.INSTANCE);
        registerIfAbsent(StringDataType.INSTANCE);
        registerIfAbsent(MapCompoundDataType.INSTANCE);
    }

    private void registerIfAbsent(DataType<?> type) {
        if (!contains(type.getKey())) {
            register(type);
        }
    }
}
