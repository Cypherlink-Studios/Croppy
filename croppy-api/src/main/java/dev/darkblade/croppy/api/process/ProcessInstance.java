package dev.darkblade.croppy.api.process;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Map;

/**
 * Runtime state instance of an active crafting or processing recipe.
 */
public interface ProcessInstance {

    /**
     * @return unique key of the process / recipe definition being executed
     */
    Key getProcessKey();

    /**
     * @return current execution state
     */
    ProcessState getState();

    /**
     * Sets the active execution state.
     *
     * @param state the new state
     */
    void setState(ProcessState state);

    /**
     * @return index of the currently active phase
     */
    int getCurrentPhaseIndex();

    /**
     * Sets the current active phase index.
     *
     * @param index phase index
     */
    void setCurrentPhaseIndex(int index);

    /**
     * @return elapsed ticks in current active phase
     */
    long getPhaseElapsedTicks();

    /**
     * Increments elapsed ticks in current phase by given amount.
     *
     * @param ticks ticks to increment
     */
    void addPhaseElapsedTicks(long ticks);

    /**
     * Resets elapsed ticks in current phase to zero.
     */
    void resetPhaseElapsedTicks();

    /**
     * @return total elapsed ticks across all phases of this process
     */
    long getTotalElapsedTicks();

    /**
     * @return runtime context variables stored during execution
     */
    Map<String, Object> getContextVariables();

    /**
     * Sets a runtime context variable.
     *
     * @param key   variable name
     * @param value variable value
     */
    void setContextVariable(String key, Object value);
}
