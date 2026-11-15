package dev.darkblade.croppy.api.variable;

import dev.darkblade.croppy.api.expression.VariableProvider;
import dev.darkblade.croppy.api.identifier.Key;

import java.util.Map;
import java.util.Optional;

/**
 * Port contract for a thread-safe container holding arbitrary typed variables
 * attached to a domain entity (station, crop, etc.).
 * Automatically exports stored variables under the {@code "var"} namespace.
 */
public interface TypedVariableStore extends VariableProvider {

    String DEFAULT_NAMESPACE = "var";

    @Override
    default String getVariableNamespace() {
        return DEFAULT_NAMESPACE;
    }

    /**
     * Defines or updates a typed variable in this store.
     *
     * @param name         variable name within the store
     * @param type         data type contract
     * @param initialValue initial value (validated against type)
     * @param <T>          value type
     * @return the created or updated typed variable instance
     */
    <T> TypedVariable<T> define(String name, DataType<T> type, T initialValue);

    /**
     * Retrieves a variable by name.
     *
     * @param name variable name
     * @return optional containing the typed variable if present
     */
    Optional<TypedVariable<?>> getVariable(String name);

    /**
     * Retrieves a variable by name if its value type is compatible with {@code valueClass}.
     *
     * @param name       variable name
     * @param valueClass expected Java value class
     * @param <T>        expected value type
     * @return optional containing the matching typed variable
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<TypedVariable<T>> getTypedVariable(String name, Class<T> valueClass) {
        if (name == null || valueClass == null) {
            return Optional.empty();
        }
        return getVariable(name)
                .filter(v -> valueClass.isAssignableFrom(v.getType().getValueClass()))
                .map(v -> (TypedVariable<T>) v);
    }

    /**
     * Applies a named mutation operation on a stored variable, enforcing type safety.
     *
     * @param name        variable name
     * @param operationId operation identifier supported by the variable's data type
     * @param args        operation arguments
     * @return the resulting value after applying the operation
     * @throws IllegalArgumentException      if the variable does not exist or arguments are invalid
     * @throws UnsupportedOperationException if the data type does not support the operation
     */
    Object applyOperation(String name, String operationId, OperationArgs args);

    /**
     * @param name variable name
     * @return true if a resolved or unresolved variable with this name exists
     */
    boolean hasVariable(String name);

    /**
     * Removes a variable by name.
     *
     * @param name variable name
     * @return true if removed
     */
    boolean removeVariable(String name);

    /**
     * @return unmodifiable map of all resolved typed variables
     */
    Map<String, TypedVariable<?>> getAllVariables();

    /**
     * Stores raw binary payload for a variable whose {@link DataType} was unavailable during loading.
     *
     * @param name       variable name
     * @param typeKey    canonical key of the missing data type
     * @param rawPayload raw binary bytes
     */
    void setUnresolvedVariable(String name, Key typeKey, byte[] rawPayload);

    /**
     * @return unmodifiable map of preserved unresolved variables
     */
    Map<String, UnresolvedVariableData> getUnresolvedVariables();

    /**
     * @return true if the store has no resolved and no unresolved variables
     */
    default boolean isEmpty() {
        return getAllVariables().isEmpty() && getUnresolvedVariables().isEmpty();
    }

    /**
     * @return true if any variable in this store was modified since the last clear
     */
    boolean isDirty();

    /**
     * Clears dirty flags on the store and all contained variables.
     */
    void clearDirty();
}
