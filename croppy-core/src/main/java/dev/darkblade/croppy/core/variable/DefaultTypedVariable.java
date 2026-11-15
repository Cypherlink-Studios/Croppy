package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.variable.DataType;
import dev.darkblade.croppy.api.variable.OperationArgs;
import dev.darkblade.croppy.api.variable.TypedVariable;
import dev.darkblade.croppy.api.variable.VariableOperation;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Thread-safe default implementation of {@link TypedVariable}.
 *
 * @param <T> the value type
 */
public final class DefaultTypedVariable<T> implements TypedVariable<T> {

    private final String name;
    private final DataType<T> type;
    private volatile T value;
    private volatile boolean dirty;

    public DefaultTypedVariable(String name, DataType<T> type, T initialValue) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.type = Objects.requireNonNull(type, "type cannot be null");
        T validated = (initialValue != null) ? validateType(initialValue) : type.getDefaultValue();
        this.value = validated;
        this.dirty = false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataType<T> getType() {
        return type;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public synchronized void setValue(T newValue) {
        T validated = (newValue != null) ? validateType(newValue) : type.getDefaultValue();
        if (!Objects.equals(this.value, validated)) {
            this.value = validated;
            this.dirty = true;
        }
    }

    @Override
    public synchronized T applyOperation(String operationId, OperationArgs args) {
        Objects.requireNonNull(operationId, "operationId cannot be null");
        OperationArgs safeArgs = (args == null) ? OperationArgs.EMPTY : args;

        VariableOperation<T> op = type.getOperation(operationId).orElseThrow(() -> {
            String supported = type.getSupportedOperations().stream()
                    .map(VariableOperation::getId)
                    .collect(Collectors.joining(", "));
            return new UnsupportedOperationException(
                    "Unsupported operation '" + operationId + "' for variable '" + name
                            + "' of type '" + type.getKey().asString() + "'. Supported: [" + supported + "]");
        });

        T updated = op.apply(this.value, safeArgs);
        T validated = (updated != null) ? validateType(updated) : type.getDefaultValue();
        if (!Objects.equals(this.value, validated)) {
            this.value = validated;
            this.dirty = true;
        }
        return this.value;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    private T validateType(T candidate) {
        if (!type.getValueClass().isInstance(candidate)) {
            throw new IllegalArgumentException(
                    "Value of type " + candidate.getClass().getName()
                            + " is incompatible with variable '" + name + "' type " + type.getKey().asString());
        }
        return candidate;
    }
}
