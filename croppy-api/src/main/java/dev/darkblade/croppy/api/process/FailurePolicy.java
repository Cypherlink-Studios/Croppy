package dev.darkblade.croppy.api.process;

/**
 * Policies governing how a process behaves upon condition violation or timeout.
 */
public enum FailurePolicy {
    /**
     * Cancels execution and resets the station to idle.
     */
    CANCEL,

    /**
     * Fails the process and spoils the contents into a spoiled byproduct.
     */
    SPOIL,

    /**
     * Terminates early producing an alternative lesser byproduct item.
     */
    BYPRODUCT
}
