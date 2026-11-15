package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.variable.SimpleDataType;
import dev.darkblade.croppy.api.variable.VariableOperation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Standard 64-bit integer data type ({@code croppy:integer}).
 */
public final class IntegerDataType implements SimpleDataType<Long> {

    public static final Key KEY = Key.of("croppy", "integer");
    public static final IntegerDataType INSTANCE = new IntegerDataType();

    private final List<VariableOperation<Long>> operations;

    private IntegerDataType() {
        this.operations = List.of(
                StandardOperations.set(this),
                StandardOperations.addLong(),
                StandardOperations.subtractLong(),
                StandardOperations.multiplyLong(),
                StandardOperations.clampLong()
        );
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Class<Long> getValueClass() {
        return Long.class;
    }

    @Override
    public Long getDefaultValue() {
        return 0L;
    }

    @Override
    public Long parse(Object rawInput) {
        if (rawInput == null) {
            return 0L;
        }
        if (rawInput instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(rawInput.toString().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer value: " + rawInput, ex);
        }
    }

    @Override
    public void writeBinary(DataOutput out, Long value) throws IOException {
        out.writeLong(value != null ? value : 0L);
    }

    @Override
    public Long readBinary(DataInput in) throws IOException {
        return in.readLong();
    }

    @Override
    public Collection<VariableOperation<Long>> getSupportedOperations() {
        return operations;
    }
}
