package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.VariableProvider;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationStateDefinition;
import dev.darkblade.croppy.api.station.component.StationComponent;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;
import dev.darkblade.croppy.core.expression.ExpressionNode;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates workstation component states and typed variable stores against declarative
 * {@link StationStateDefinition} conditions to resolve the current active state.
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
            var procComp = station.getComponent(dev.darkblade.croppy.api.station.component.ProcessComponent.class);
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
     * Constructs an {@link EvaluationContext} dynamically aggregating all attached
     * {@link VariableProvider} components, the station's {@code var.*} typed variable store,
     * and backward-compatible legacy aliases.
     *
     * @param station the workstation instance
     * @param process active process instance, or null if none
     * @return populated evaluation context
     */
    public static EvaluationContext buildContext(StationInstance station, ProcessInstance process) {
        Objects.requireNonNull(station, "station cannot be null");
        Map<String, Object> vars = new HashMap<>();

        // 1. General neutral station properties
        vars.put("station.state", station.getActiveStateId());
        vars.put("station.yaw", (double) station.getYaw());
        vars.put("station.key", station.getStationKey().asString());
        if (station.getDefinition() != null) {
            vars.put("station.name", station.getDefinition().displayName());
        } else {
            vars.put("station.name", station.getStationKey().value());
        }

        // 2. Modular Component Variable Providers (thermal.*, heat.*, storage.*, process.*, display.*, addon.*)
        for (StationComponent component : station.getComponents().values()) {
            if (component instanceof VariableProvider provider) {
                provider.exportNamespaced(vars);
            }
        }

        // Ensure explicitly supplied ProcessInstance is exported even if ProcessComponent wasn't attached/updated
        if (process != null) {
            new DefaultProcessComponent(process).exportNamespaced(vars);
        }

        // Sync process progress with display progress if process.progress wasn't explicitly set
        if (vars.containsKey("display.progress")) {
            double dispProg = ((Number) vars.get("display.progress")).doubleValue();
            String bar = dev.darkblade.croppy.core.station.visual.StationPlaceholderFormatter.buildProgressBar((float) dispProg, 10);
            vars.put("display.progress_bar", bar);
            if (!vars.containsKey("process.progress") || ((Number) vars.get("process.progress")).doubleValue() == 0.0) {
                if (dispProg > 0.0) {
                    vars.put("process.progress", dispProg);
                    vars.put("process.progress_percent", Math.round(dispProg * 100.0));
                    vars.put("process.progress_bar", bar);
                }
            }
        }

        // 3. Typed Variable Store (var.*)
        station.getVariableStore().ifPresent(store -> store.exportNamespaced(vars));

        // 4. Backward-compatible aliases (preserves legacy un-namespaced & 'station.*' keys)
        applyLegacyAliases(station, process, vars);

        return EvaluationContext.of(vars);
    }

    private static void applyLegacyAliases(StationInstance station, ProcessInstance process, Map<String, Object> vars) {
        vars.putIfAbsent("state", station.getActiveStateId());
        vars.putIfAbsent("yaw", (double) station.getYaw());

        Object temp = vars.getOrDefault("thermal.temp", 20.0);
        Object isHeated = vars.getOrDefault("thermal.is_heated", false);
        Object zone = vars.containsKey("thermal.zone")
                ? vars.get("thermal.zone")
                : vars.getOrDefault("display.thermal_zone", "NONE");

        vars.putIfAbsent("temperature", temp);
        vars.putIfAbsent("temp", temp);
        vars.putIfAbsent("station.temperature", temp);
        vars.putIfAbsent("station.is_heated", isHeated);
        vars.putIfAbsent("station.thermal_zone", zone);
        vars.putIfAbsent("thermal_zone", zone);

        vars.putIfAbsent("progress", vars.getOrDefault("display.progress", vars.getOrDefault("process.progress", 0.0)));
        if (vars.containsKey("display.status_text")) {
            vars.putIfAbsent("status_text", vars.get("display.status_text"));
        }

        Object fuelTicks = vars.getOrDefault("heat.fuel_ticks", 0L);
        Object isActive = vars.getOrDefault("heat.is_active", false);
        vars.putIfAbsent("fuel_ticks", fuelTicks);
        vars.putIfAbsent("fuel", fuelTicks);
        vars.putIfAbsent("is_active", isActive);
        vars.putIfAbsent("active", isActive);

        vars.putIfAbsent("has_inputs", vars.getOrDefault("storage.has_inputs", false));
        vars.putIfAbsent("input_count", vars.getOrDefault("storage.input_count", 0));
        vars.putIfAbsent("has_outputs", vars.getOrDefault("storage.has_outputs", false));
        vars.putIfAbsent("output_count", vars.getOrDefault("storage.output_count", 0));

        boolean hasProcess = process != null || Boolean.TRUE.equals(vars.get("process.has_process"));
        String pState = vars.containsKey("process.state")
                ? vars.get("process.state").toString()
                : (process != null ? process.getState().name() : ProcessState.IDLE.name());
        vars.putIfAbsent("has_process", hasProcess);
        vars.putIfAbsent("process_state", pState);
        vars.putIfAbsent("process_key", vars.getOrDefault("process.key", ""));
        vars.putIfAbsent("recipe_name", vars.getOrDefault("process.recipe_name", ""));
        vars.putIfAbsent("phase_elapsed", vars.getOrDefault("process.phase_elapsed", 0.0));
        vars.putIfAbsent("process.finished", Boolean.TRUE.equals(vars.get("process.finished")));
    }
}
