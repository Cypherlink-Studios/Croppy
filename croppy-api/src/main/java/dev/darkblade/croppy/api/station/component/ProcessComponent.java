package dev.darkblade.croppy.api.station.component;

import dev.darkblade.croppy.api.process.ProcessInstance;

import java.util.Optional;

/**
 * Component managing active recipe or crafting processes on a station.
 */
public interface ProcessComponent extends StationComponent {

    /**
     * @return current active process instance, if any
     */
    Optional<ProcessInstance> getActiveProcess();

    /**
     * Assigns the active process instance.
     *
     * @param process process instance to assign
     */
    void setActiveProcess(ProcessInstance process);

    /**
     * Clears any active process instance.
     */
    void clearActiveProcess();

    /**
     * @return true if a process instance is assigned
     */
    default boolean hasActiveProcess() {
        return getActiveProcess().isPresent();
    }
}
