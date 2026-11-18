package dev.darkblade.croppy.api.process;

import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;

/**
 * Lifecycle listener receiving callbacks from the core process engine.
 * All methods have default no-op implementations so listeners can subscribe selectively.
 */
public interface ProcessListener {

    /**
     * Called immediately after a process starts on a station.
     *
     * @param station    target station
     * @param definition process definition
     * @param instance   newly created process instance
     */
    default void onProcessStart(StationInstance station, ProcessDefinition definition, ProcessInstance instance) {}

    /**
     * Called on each active process tick before/after phase evaluation.
     *
     * @param station    target station
     * @param definition process definition
     * @param instance   active process instance
     * @param deltaTicks elapsed ticks since last evaluation
     */
    default void onProcessTick(StationInstance station, ProcessDefinition definition, ProcessInstance instance, long deltaTicks) {}

    /**
     * Called when a process transitions from one phase to the next.
     *
     * @param station            target station
     * @param definition         process definition
     * @param instance           active process instance
     * @param previousPhaseIndex completed phase index
     * @param newPhaseIndex      new active phase index
     */
    default void onPhaseAdvance(StationInstance station, ProcessDefinition definition, ProcessInstance instance,
                                int previousPhaseIndex, int newPhaseIndex) {}

    /**
     * Called when a process completes all phases and resolves outputs.
     *
     * @param station    target station
     * @param definition process definition
     * @param instance   completed process instance
     * @param outputs    resolved output items
     */
    default void onProcessComplete(StationInstance station, ProcessDefinition definition, ProcessInstance instance,
                                   List<StoredItem> outputs) {}

    /**
     * Called when a process fails due to timeout or condition failure.
     *
     * @param station     target station
     * @param definition  process definition
     * @param instance    failed process instance
     * @param failedPhase phase that triggered failure
     */
    default void onProcessFail(StationInstance station, ProcessDefinition definition, ProcessInstance instance,
                               PhaseDefinition failedPhase) {}
}
