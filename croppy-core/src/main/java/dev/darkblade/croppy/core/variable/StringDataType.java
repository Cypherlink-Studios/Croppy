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
 * Standard UTF-8 string data type ({@code croppy:string}).
 */
public final class StringDataType implements SimpleDataType<String> {

    public static final Key KEY = Key.of("croppy", "string");
    public static final StringDataType INSTANCE = new StringDataType();

    private final List<VariableOperation<String>> operations;

    private StringDataType() {
        this.operations = List.of(
                StandardOperations.set(this),
                StandardOperations.appendString()
        );
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Class<String> getValueClass() {
        return String.class;
    }

    @Override
    public String getDefaultValue() {
        return "";
    }

    @Override
    public String parse(Object rawInput) {
        return rawInput == null ? "" : rawInput.toString();
    }

    @Override
    public void writeBinary(DataOutput out, String value) throws IOException {
        out.writeUTF(value != null ? value : "");
    }

    @Override
    public String readBinary(DataInput in) throws IOException {
        return in.readUTF();
    }

    @Override
    public Collection<VariableOperation<String>> getSupportedOperations() {
        return operations;
    }
}
