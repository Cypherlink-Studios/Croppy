package dev.darkblade.croppy.api.process;

/**
 * Execution states of a process instance.
 */
public enum ProcessState {
    /**
     * Waiting for valid inputs or activation conditions.
     */
    IDLE,

    /**
     * Actively progressing through a recipe phase.
     */
    RUNNING,

    /**
     * Suspended awaiting manual player intervention or item delivery.
     */
    AWAITING_INPUT,

    /**
     * All phases finished; output is ready for collection.
     */
    COMPLETED,

    /**
     * Failed due to condition violation or phase timeout.
     */
    FAILED
}
