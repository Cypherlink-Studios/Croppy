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
 * Standard 64-bit floating-point decimal data type ({@code croppy:decimal}).
 */
public final class DecimalDataType implements SimpleDataType<Double> {

    public static final Key KEY = Key.of("croppy", "decimal");
    public static final DecimalDataType INSTANCE = new DecimalDataType();

    private final List<VariableOperation<Double>> operations;

    private DecimalDataType() {
        this.operations = List.of(
                StandardOperations.set(this),
                StandardOperations.addDouble(),
                StandardOperations.subtractDouble(),
                StandardOperations.multiplyDouble(),
                StandardOperations.clampDouble()
        );
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Class<Double> getValueClass() {
        return Double.class;
    }

    @Override
    public Double getDefaultValue() {
        return 0.0;
    }

    @Override
    public Double parse(Object rawInput) {
        if (rawInput == null) {
            return 0.0;
        }
        if (rawInput instanceof Number n) {
            double val = n.doubleValue();
            if (!Double.isFinite(val)) {
                throw new IllegalArgumentException("Decimal value must be finite: " + val);
            }
            return val;
        }
        try {
            double val = Double.parseDouble(rawInput.toString().trim());
            if (!Double.isFinite(val)) {
                throw new IllegalArgumentException("Decimal value must be finite: " + val);
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid decimal value: " + rawInput, ex);
        }
    }

    @Override
    public String formatValue(Double value) {
        return value != null ? String.format(Locale.ROOT, "%.1f", value) : "0.0";
    }

    @Override
    public void writeBinary(DataOutput out, Double value) throws IOException {
        out.writeDouble(value != null ? value : 0.0);
    }

    @Override
    public Double readBinary(DataInput in) throws IOException {
        return in.readDouble();
    }

    @Override
    public Collection<VariableOperation<Double>> getSupportedOperations() {
        return operations;
    }
}
