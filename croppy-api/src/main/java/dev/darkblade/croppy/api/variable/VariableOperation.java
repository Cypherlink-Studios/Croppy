package dev.darkblade.croppy.api.variable;

/**
 * Represents a strictly typed mutation operation supported by a {@link DataType}.
 *
 * @param <T> the underlying Java value type operated upon
 */
public interface VariableOperation<T> {

    /**
     * @return unique uppercase identifier of this operation (e.g., "SET", "ADD", "TOGGLE", "DRAIN")
     */
    String getId();

    /**
     * Applies this operation to the current value using the given arguments.
     *
     * @param currentValue the current value before mutation
     * @param args         operation parameters
     * @return the resulting value after applying the operation
     * @throws IllegalArgumentException if required arguments are missing or invalid
     */
    T apply(T currentValue, OperationArgs args);
}
