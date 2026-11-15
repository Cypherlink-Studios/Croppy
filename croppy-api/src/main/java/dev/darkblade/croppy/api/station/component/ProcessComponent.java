package dev.darkblade.croppy.api.station.component;

import dev.darkblade.croppy.api.expression.VariableProvider;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;

import java.util.Map;
import java.util.Optional;

/**
 * Component managing active recipe or crafting processes on a station.
 */
public interface ProcessComponent extends StationComponent, VariableProvider {

    @Override
    default String getVariableNamespace() {
        return "process";
    }

    @Override
    default void populateVariables(Map<String, Object> variables) {
        ProcessInstance process = getActiveProcess().orElse(null);
        boolean hasProcess = process != null;
        ProcessState state = hasProcess ? process.getState() : ProcessState.IDLE;

        double progress = 0.0;
        if (hasProcess) {
            Object rawProg = process.getContextVariables().get("progress");
            if (rawProg instanceof Number n) {
                progress = Math.max(0.0, Math.min(1.0, n.doubleValue()));
            } else if (state == ProcessState.COMPLETED) {
                progress = 1.0;
            }
        }
        int percent = (int) Math.round(progress * 100.0);
        int filled = Math.min(10, (int) Math.round(progress * 10.0));
        int empty = 10 - filled;
        StringBuilder bar = new StringBuilder();
        if (filled > 0) {
            bar.append("<green>").append("■".repeat(filled)).append("</green>");
        }
        if (empty > 0) {
            bar.append("<gray>").append("□".repeat(empty)).append("</gray>");
        }

        variables.put("has_process", hasProcess);
        variables.put("state", state.name());
        variables.put("finished", hasProcess && state == ProcessState.COMPLETED);
        variables.put("idle_ticks", (hasProcess && state == ProcessState.COMPLETED) ? process.getPhaseElapsedTicks() : 0L);
        variables.put("key", hasProcess ? process.getProcessKey().asString() : "");
        variables.put("recipe_name", hasProcess ? process.getProcessKey().value() : "");
        variables.put("phase_index", hasProcess ? process.getCurrentPhaseIndex() + 1 : 1);
        variables.put("phase_elapsed", hasProcess ? (double) process.getPhaseElapsedTicks() : 0.0);
        variables.put("total_elapsed", hasProcess ? (double) process.getTotalElapsedTicks() : 0.0);
        variables.put("progress", progress);
        variables.put("progress_percent", percent);
        variables.put("progress_bar", bar.toString());
    }

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
