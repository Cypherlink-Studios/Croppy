package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.variable.DataType;
import dev.darkblade.croppy.api.variable.DataTypeRegistry;
import dev.darkblade.croppy.api.variable.OperationArgs;
import dev.darkblade.croppy.api.variable.TypedVariable;
import dev.darkblade.croppy.api.variable.TypedVariableStore;
import dev.darkblade.croppy.api.variable.UnresolvedVariableData;
import dev.darkblade.croppy.api.variable.VariableSpec;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe default implementation of {@link TypedVariableStore}.
 */
public final class DefaultTypedVariableStore implements TypedVariableStore {

    private final Map<String, TypedVariable<?>> variables = new ConcurrentHashMap<>();
    private final Map<String, UnresolvedVariableData> unresolvedVariables = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    @Override
    public <T> TypedVariable<T> define(String name, DataType<T> type, T initialValue) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        DefaultTypedVariable<T> var = new DefaultTypedVariable<>(name, type, initialValue);
        variables.put(name, var);
        unresolvedVariables.remove(name);
        dirty = true;
        return var;
    }

    /**
     * Populates initial variables from declarative {@link VariableSpec} entries if not already present.
     *
     * @param specs    variable specifications
     * @param registry data type registry resolving type keys
     */
    public void initializeFromSpecs(Collection<VariableSpec> specs, DataTypeRegistry registry) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        Objects.requireNonNull(registry, "registry cannot be null");
        for (VariableSpec spec : specs) {
            if (hasVariable(spec.name())) {
                continue;
            }
            DataType<?> type = registry.get(spec.typeKey()).orElseThrow(() ->
                    new IllegalArgumentException("Unknown data type key '" + spec.typeKey() + "' for variable '" + spec.name() + "'"));
            defineFromRaw(spec.name(), type, spec.initialValue());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void defineFromRaw(String name, DataType<T> type, Object rawValue) {
        T parsed = (rawValue != null) ? type.parse(rawValue) : type.getDefaultValue();
        define(name, type, parsed);
    }

    @Override
    public Optional<TypedVariable<?>> getVariable(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(variables.get(name));
    }

    @Override
    public Object applyOperation(String name, String operationId, OperationArgs args) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(operationId, "operationId cannot be null");

        TypedVariable<?> variable = variables.get(name);
        if (variable == null) {
            if (unresolvedVariables.containsKey(name)) {
                throw new IllegalStateException("Cannot mutate unresolved variable '" + name + "': missing data type");
            }
            throw new IllegalArgumentException("Variable '" + name + "' is not defined in this store");
        }

        Object result = variable.applyOperation(operationId, args);
        if (variable.isDirty()) {
            dirty = true;
        }
        return result;
    }

    @Override
    public boolean hasVariable(String name) {
        return name != null && (variables.containsKey(name) || unresolvedVariables.containsKey(name));
    }

    @Override
    public boolean removeVariable(String name) {
        if (name == null) {
            return false;
        }
        boolean removedVar = variables.remove(name) != null;
        boolean removedUnresolved = unresolvedVariables.remove(name) != null;
        if (removedVar || removedUnresolved) {
            dirty = true;
            return true;
        }
        return false;
    }

    @Override
    public Map<String, TypedVariable<?>> getAllVariables() {
        return Collections.unmodifiableMap(variables);
    }

    @Override
    public void setUnresolvedVariable(String name, Key typeKey, byte[] rawPayload) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(typeKey, "typeKey cannot be null");
        unresolvedVariables.put(name, new UnresolvedVariableData(name, typeKey, rawPayload));
        variables.remove(name);
    }

    @Override
    public Map<String, UnresolvedVariableData> getUnresolvedVariables() {
        return Collections.unmodifiableMap(unresolvedVariables);
    }

    @Override
    public void populateVariables(Map<String, Object> destination) {
        Objects.requireNonNull(destination, "destination cannot be null");
        for (TypedVariable<?> variable : variables.values()) {
            exportSingle(variable, destination);
        }
    }

    private <T> void exportSingle(TypedVariable<T> variable, Map<String, Object> destination) {
        T val = variable.getValue();
        if (val != null) {
            variable.getType().exportVariables(variable.getName(), val, destination);
        }
    }

    @Override
    public boolean isDirty() {
        if (dirty) {
            return true;
        }
        for (TypedVariable<?> var : variables.values()) {
            if (var.isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void clearDirty() {
        dirty = false;
        for (TypedVariable<?> var : variables.values()) {
            var.clearDirty();
        }
    }
}
