package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.variable.SimpleDataType;
import dev.darkblade.croppy.api.variable.VariableOperation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Standard boolean data type ({@code croppy:boolean}).
 */
public final class BooleanDataType implements SimpleDataType<Boolean> {

    public static final Key KEY = Key.of("croppy", "boolean");
    public static final BooleanDataType INSTANCE = new BooleanDataType();

    private final List<VariableOperation<Boolean>> operations;

    private BooleanDataType() {
        this.operations = List.of(
                StandardOperations.set(this),
                StandardOperations.toggleBoolean()
        );
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Class<Boolean> getValueClass() {
        return Boolean.class;
    }

    @Override
    public Boolean getDefaultValue() {
        return Boolean.FALSE;
    }

    @Override
    public Boolean parse(Object rawInput) {
        if (rawInput == null) {
            return Boolean.FALSE;
        }
        if (rawInput instanceof Boolean b) {
            return b;
        }
        String normalized = rawInput.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean value: " + rawInput);
    }

    @Override
    public void writeBinary(DataOutput out, Boolean value) throws IOException {
        out.writeBoolean(value != null && value);
    }

    @Override
    public Boolean readBinary(DataInput in) throws IOException {
        return in.readBoolean();
    }

    @Override
    public Collection<VariableOperation<Boolean>> getSupportedOperations() {
        return operations;
    }
}
