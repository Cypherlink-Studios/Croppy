package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationStateDefinition;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;
import dev.darkblade.croppy.core.expression.ExpressionNode;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates workstation component states against declarative {@link StationStateDefinition} conditions
 * to resolve the current active state.
 */
public final class StationStateEvaluator {

    private static final Comparator<StationStateDefinition> PRIORITY_COMPARATOR =
            Comparator.comparingInt(StationStateDefinition::priority).reversed();

    private StationStateEvaluator() {
    }

    /**
     * Evaluates the given station instance and returns the highest priority active state.
     *
     * @param station the workstation instance
     * @return optional containing the active state definition if matched, or empty for default
     */
    public static Optional<StationStateDefinition> evaluate(StationInstance station) {
        ProcessInstance process = null;
        if (station != null) {
            Optional<ProcessComponent> procComp = station.getComponent(ProcessComponent.class);
            if (procComp.isPresent()) {
                process = procComp.get().getActiveProcess().orElse(null);
            }
        }
        return evaluate(station, process);
    }

    /**
     * Evaluates the given station instance and active process, returning the highest priority active state.
     *
     * @param station the workstation instance
     * @param process active process instance, or null if none
     * @return optional containing the active state definition if matched, or empty for default
     */
    public static Optional<StationStateDefinition> evaluate(StationInstance station, ProcessInstance process) {
        if (station == null) {
            return Optional.empty();
        }

        StationDefinition definition = station.getDefinition();
        List<StationStateDefinition> states = definition.states();
        if (states.isEmpty()) {
            return Optional.empty();
        }

        EvaluationContext context = buildContext(station, process);

        List<StationStateDefinition> sortedStates = states.stream()
                .sorted(PRIORITY_COMPARATOR)
                .toList();

        StationStateDefinition fallbackState = null;

        for (StationStateDefinition stateDef : sortedStates) {
            Optional<String> conditionOpt = stateDef.getCondition();
            if (conditionOpt.isEmpty() || conditionOpt.get().isBlank()) {
                if (fallbackState == null) {
                    fallbackState = stateDef;
                }
                continue;
            }

            try {
                ExpressionNode node = ExpressionCompiler.compile(conditionOpt.get());
                Object result = node.evaluate(context);
                if (Boolean.TRUE.equals(result)) {
                    return Optional.of(stateDef);
                }
            } catch (Exception ignored) {
                // Ignore evaluation errors for malformed dynamic expressions
            }
        }

        return Optional.ofNullable(fallbackState);
    }

    /**
     * Constructs an {@link EvaluationContext} populating physical and runtime variables from the station.
     *
     * @param station the workstation instance
     * @param process active process instance, or null if none
     * @return populated evaluation context
     */
    public static EvaluationContext buildContext(StationInstance station, ProcessInstance process) {
        Objects.requireNonNull(station, "station cannot be null");
        Map<String, Object> vars = new HashMap<>();

        // 1. General station properties
        vars.put("state", station.getActiveStateId());
        vars.put("yaw", (double) station.getYaw());

        // 2. Thermal metrics
        double temp = 20.0;
        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        if (thermalOpt.isPresent()) {
            temp = thermalOpt.get().getTemperature();
        }
        vars.put("temperature", temp);
        vars.put("temp", temp);

        // 3. Display component metrics
        String thermalZone = "NONE";
        double progress = 0.0;
        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isPresent()) {
            thermalZone = displayOpt.get().getThermalZone();
            progress = displayOpt.get().getProgress();
            vars.put("status_text", displayOpt.get().getStatusText());
        }
        vars.put("thermal_zone", thermalZone);
        vars.put("progress", progress);

        // 4. Heat source metrics
        long fuelTicks = 0;
        boolean isActive = false;
        Optional<HeatSourceComponent> heatOpt = station.getComponent(HeatSourceComponent.class);
        if (heatOpt.isPresent()) {
            fuelTicks = heatOpt.get().getBurnTicksRemaining();
            isActive = heatOpt.get().isActive();
        }
        vars.put("fuel_ticks", fuelTicks);
        vars.put("fuel", fuelTicks);
        vars.put("is_active", isActive);
        vars.put("active", isActive);

        // 5. Storage metrics
        boolean hasInputs = false;
        int inputCount = 0;
        boolean hasOutputs = false;
        int outputCount = 0;
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isPresent()) {
            StorageComponent storage = storageOpt.get();
            hasInputs = !storage.getInputs().isEmpty();
            inputCount = storage.getInputs().size();
            hasOutputs = !storage.getOutputs().isEmpty();
            outputCount = storage.getOutputs().size();
        }
        vars.put("has_inputs", hasInputs);
        vars.put("input_count", inputCount);
        vars.put("has_outputs", hasOutputs);
        vars.put("output_count", outputCount);

        // 6. Process metrics
        boolean hasProcess = process != null;
        ProcessState pState = hasProcess ? process.getState() : ProcessState.IDLE;
        vars.put("has_process", hasProcess);
        vars.put("process_state", pState.name());
        if (hasProcess) {
            vars.put("process_key", process.getProcessKey().asString());
            vars.put("recipe_name", process.getProcessKey().value());
            vars.put("phase_elapsed", (double) process.getPhaseElapsedTicks());
        } else {
            vars.put("process_key", "");
            vars.put("recipe_name", "");
            vars.put("phase_elapsed", 0.0);
        }

        return EvaluationContext.of(vars);
    }
}
