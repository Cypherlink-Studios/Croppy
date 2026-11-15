package dev.darkblade.croppy.api.variable;

/**
 * Runtime instance of a typed variable storing a validated value of type {@code T}.
 *
 * @param <T> the value type managed by this variable
 */
public interface TypedVariable<T> {

    /**
     * @return the variable's local identifier within its owning store (e.g., "cleanliness")
     */
    String getName();

    /**
     * @return the data type contract governing this variable
     */
    DataType<T> getType();

    /**
     * @return current variable value
     */
    T getValue();

    /**
     * Replaces the variable value directly and marks the variable dirty if changed.
     *
     * @param value new value (must match {@link DataType#getValueClass()})
     */
    void setValue(T value);

    /**
     * Applies a named {@link VariableOperation} declared by this variable's {@link DataType}.
     *
     * @param operationId case-insensitive operation identifier (e.g., "ADD", "TOGGLE")
     * @param args        operation arguments
     * @return the new resulting value
     * @throws UnsupportedOperationException if the operation is not supported by this variable's data type
     * @throws IllegalArgumentException      if arguments are missing or invalid
     */
    T applyOperation(String operationId, OperationArgs args);

    /**
     * @return true if this variable has been modified since the last clear
     */
    boolean isDirty();

    /**
     * Clears the dirty modification flag.
     */
    void clearDirty();
}
