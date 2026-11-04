package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.*;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.core.recipe.RecipeMatcher;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;

import java.util.*;

/**
 * Finite state machine execution engine governing process lifecycles and transitions.
 */
public final class ProcessEngine implements Service {

    private final ProcessRegistry processRegistry;
    private final RecipeRegistry recipeRegistry;
    private final RecipeMatcher recipeMatcher;
    private final ProcessDisplayFormatter displayFormatter;

    public ProcessEngine(ProcessRegistry processRegistry) {
        this(processRegistry, null, new RecipeMatcher(), new ProcessDisplayFormatter());
    }

    public ProcessEngine(ProcessRegistry processRegistry, RecipeRegistry recipeRegistry) {
        this(processRegistry, recipeRegistry, new RecipeMatcher(), new ProcessDisplayFormatter());
    }

    public ProcessEngine(
            ProcessRegistry processRegistry,
            RecipeRegistry recipeRegistry,
            RecipeMatcher recipeMatcher,
            ProcessDisplayFormatter displayFormatter
    ) {
        this.processRegistry = Objects.requireNonNull(processRegistry, "processRegistry cannot be null");
        this.recipeRegistry = recipeRegistry;
        this.recipeMatcher = recipeMatcher != null ? recipeMatcher : new RecipeMatcher();
        this.displayFormatter = displayFormatter != null ? displayFormatter : new ProcessDisplayFormatter();
    }

    /**
     * Attempts to initiate a recipe or standalone process on an idle station if storage contains required inputs
     * and start conditions evaluate to true.
     *
     * @param station station instance
     * @param context evaluation context
     * @return true if a process was started
     */
    public boolean tryStartProcess(StationInstance station, EvaluationContext context) {
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return false;
        }

        ProcessComponent processComp = station.getComponent(ProcessComponent.class).orElseGet(() -> {
            DefaultProcessComponent comp = new DefaultProcessComponent();
            station.setComponent(ProcessComponent.class, comp);
            return comp;
        });

        if (processComp.hasActiveProcess() && processComp.getActiveProcess().get().getState() != ProcessState.IDLE) {
            return false;
        }

        StorageComponent storage = storageOpt.get();
        List<StoredItem> currentInputs = storage.getInputs();
        Key stationKey = station.getStationKey();

        // 1. Try matching registered culinary recipes first
        if (recipeRegistry != null) {
            List<RecipeDefinition> stationRecipes = new ArrayList<>();
            for (RecipeDefinition r : recipeRegistry.values()) {
                if (r.stationKey().equals(stationKey)) {
                    stationRecipes.add(r);
                }
            }

            Optional<RecipeDefinition> matchOpt = recipeMatcher.findMatch(stationRecipes, currentInputs);
            if (matchOpt.isPresent()) {
                RecipeDefinition recipe = matchOpt.get();
                Key procKey = recipe.processKey();
                Optional<ProcessDefinition> procDefOpt = processRegistry.get(procKey);
                if (procDefOpt.isPresent()) {
                    ProcessDefinition procDef = procDefOpt.get();
                    if (procDef.startConditions().evaluate(context)) {
                        // Consume recipe inputs
                        for (StoredItem req : recipe.inputRequirements()) {
                            storage.removeInput(req.itemKey(), req.amount());
                        }

                        DefaultProcessInstance instance = new DefaultProcessInstance(procDef.key());
                        instance.setState(ProcessState.RUNNING);
                        instance.setCurrentPhaseIndex(0);
                        instance.setContextVariable("original_inputs", currentInputs);
                        instance.setContextVariable("recipe_key", recipe.key().asString());
                        instance.setContextVariable("recipe_name", recipe.displayName());
                        instance.setContextVariable("result_item", recipe.resultItemKey().asString());
                        instance.setContextVariable("result_amount", recipe.resultAmount());

                        PhaseDefinition initialPhase = procDef.phases().get(0);
                        if (initialPhase.type() == PhaseType.MANUAL_INTERACTION) {
                            instance.setState(ProcessState.AWAITING_INPUT);
                        }

                        processComp.setActiveProcess(instance);
                        updateStationDisplay(station, procDef, instance, initialPhase, context);
                        return true;
                    }
                }
            }
        }

        // 2. Fallback: match standalone process definitions directly
        for (ProcessDefinition def : processRegistry.values()) {
            if (!def.stationKey().equals(stationKey)) {
                continue;
            }

            if (!matchesInputs(def.inputRequirements(), currentInputs)) {
                continue;
            }

            if (!def.startConditions().evaluate(context)) {
                continue;
            }

            // Consume required inputs
            for (StoredItem req : def.inputRequirements()) {
                storage.removeInput(req.itemKey(), req.amount());
            }

            // Create and assign active process
            DefaultProcessInstance instance = new DefaultProcessInstance(def.key());
            instance.setState(ProcessState.RUNNING);
            instance.setCurrentPhaseIndex(0);
            instance.setContextVariable("original_inputs", currentInputs);

            PhaseDefinition initialPhase = def.phases().get(0);
            if (initialPhase.type() == PhaseType.MANUAL_INTERACTION) {
                instance.setState(ProcessState.AWAITING_INPUT);
            }

            processComp.setActiveProcess(instance);
            updateStationDisplay(station, def, instance, initialPhase, context);
            return true;
        }

        return false;
    }

    /**
     * Ticks the active process on a station.
     *
     * @param station    the station instance
     * @param deltaTicks elapsed game ticks (typically 1 or 20)
     * @param context    evaluation context
     */
    public void tick(StationInstance station, long deltaTicks, EvaluationContext context) {
        Optional<ProcessComponent> processCompOpt = station.getComponent(ProcessComponent.class);
        if (processCompOpt.isEmpty() || !processCompOpt.get().hasActiveProcess()) {
            return;
        }

        ProcessComponent processComp = processCompOpt.get();
        ProcessInstance instance = processComp.getActiveProcess().orElseThrow();
        if (instance.getState() == ProcessState.IDLE || instance.getState() == ProcessState.COMPLETED || instance.getState() == ProcessState.FAILED) {
            return;
        }

        Optional<ProcessDefinition> defOpt = processRegistry.get(instance.getProcessKey());
        if (defOpt.isEmpty()) {
            instance.setState(ProcessState.FAILED);
            return;
        }

        ProcessDefinition def = defOpt.get();
        if (instance.getCurrentPhaseIndex() >= def.phases().size()) {
            completeProcess(station, def, instance);
            return;
        }

        PhaseDefinition phase = def.phases().get(instance.getCurrentPhaseIndex());

        // Evaluation context enriched with process variables
        EvaluationContext phaseContext = context
                .with("phase.elapsed_ticks", instance.getPhaseElapsedTicks())
                .with("process.total_ticks", instance.getTotalElapsedTicks());

        switch (phase.type()) {
            case TIMED -> {
                if (phase.condition().evaluate(phaseContext)) {
                    instance.addPhaseElapsedTicks(deltaTicks);
                    if (instance.getPhaseElapsedTicks() >= phase.durationTicks()) {
                        advancePhase(station, def, instance, context);
                    } else {
                        updateStationDisplay(station, def, instance, phase, context);
                    }
                } else {
                    if (phase.pauseOnConditionFailure()) {
                        // Condition paused (e.g. insufficient heat): freeze elapsed duration
                        updateStationDisplayPaused(station, def, instance, phase, context);
                    } else {
                        handlePhaseFailure(station, def, instance, phase);
                    }
                }
            }
            case ENVIRONMENTAL -> {
                instance.addPhaseElapsedTicks(deltaTicks);
                if (phase.condition().evaluate(phaseContext)) {
                    advancePhase(station, def, instance, context);
                } else {
                    updateStationDisplay(station, def, instance, phase, context);
                }
            }
            case MANUAL_INTERACTION -> {
                instance.addPhaseElapsedTicks(deltaTicks);
                if (instance.getState() != ProcessState.AWAITING_INPUT) {
                    instance.setState(ProcessState.AWAITING_INPUT);
                }
                // Check phase timeout for interactive phase
                if (phase.timeoutTicks() > 0 && instance.getPhaseElapsedTicks() >= phase.timeoutTicks()) {
                    handlePhaseFailure(station, def, instance, phase);
                    return;
                }
                updateStationDisplay(station, def, instance, phase, context);
            }
        }
    }

    /**
     * Handles player manual interaction with a station in an AWAITING_INPUT phase.
     *
     * @param station station instance
     * @param itemKey item key in player's hand (or null)
     * @param amount  item quantity in hand
     * @return true if the interaction successfully advanced the process
     */
    public boolean handleManualInteraction(StationInstance station, Key itemKey, int amount) {
        Optional<ProcessComponent> processCompOpt = station.getComponent(ProcessComponent.class);
        if (processCompOpt.isEmpty() || !processCompOpt.get().hasActiveProcess()) {
            return false;
        }

        ProcessInstance instance = processCompOpt.get().getActiveProcess().orElseThrow();
        if (instance.getState() != ProcessState.AWAITING_INPUT) {
            return false;
        }

        Optional<ProcessDefinition> defOpt = processRegistry.get(instance.getProcessKey());
        if (defOpt.isEmpty() || instance.getCurrentPhaseIndex() >= defOpt.get().phases().size()) {
            return false;
        }

        ProcessDefinition def = defOpt.get();
        PhaseDefinition phase = def.phases().get(instance.getCurrentPhaseIndex());

        if (phase.type() != PhaseType.MANUAL_INTERACTION) {
            return false;
        }

        // Validate item requirement
        if (phase.requiredItem() != null) {
            if (itemKey == null || !phase.requiredItem().equals(itemKey) || amount < phase.requiredAmount()) {
                return false;
            }
        }

        advancePhase(station, def, instance, EvaluationContext.empty());
        return true;
    }

    /**
     * Advances to the next phase or finishes the process.
     */
    public void advancePhase(StationInstance station, ProcessDefinition def, ProcessInstance instance, EvaluationContext context) {
        int nextIndex = instance.getCurrentPhaseIndex() + 1;
        instance.resetPhaseElapsedTicks();

        if (nextIndex >= def.phases().size()) {
            completeProcess(station, def, instance);
        } else {
            instance.setCurrentPhaseIndex(nextIndex);
            PhaseDefinition nextPhase = def.phases().get(nextIndex);
            if (nextPhase.type() == PhaseType.MANUAL_INTERACTION) {
                instance.setState(ProcessState.AWAITING_INPUT);
            } else {
                instance.setState(ProcessState.RUNNING);
            }
            updateStationDisplay(station, def, instance, nextPhase, context);
        }
    }

    public void advancePhase(StationInstance station, ProcessDefinition def, ProcessInstance instance) {
        advancePhase(station, def, instance, EvaluationContext.empty());
    }

    public void completeProcess(StationInstance station, ProcessDefinition def, ProcessInstance instance) {
        instance.setState(ProcessState.COMPLETED);

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        Key resultDisplayKey = null;

        if (storageOpt.isPresent()) {
            StorageComponent storage = storageOpt.get();
            @SuppressWarnings("unchecked")
            List<StoredItem> originalInputs = (List<StoredItem>) instance.getContextVariables()
                    .getOrDefault("original_inputs", List.of());

            List<StoredItem> outputs;
            if (instance.getContextVariables().containsKey("result_item")) {
                Key resKey = Key.parse(String.valueOf(instance.getContextVariables().get("result_item")));
                int amount = instance.getContextVariables().get("result_amount") instanceof Number num ? num.intValue() : 1;
                outputs = List.of(StoredItem.of(resKey, amount));
            } else if (def.outputResolver() != null) {
                outputs = def.outputResolver().resolveOutputs(def, instance, originalInputs);
            } else if (def.defaultResultItem() != null) {
                outputs = List.of(StoredItem.of(def.defaultResultItem(), def.defaultResultAmount()));
            } else {
                outputs = List.of();
            }

            for (StoredItem out : outputs) {
                storage.addOutput(out);
            }

            if (!outputs.isEmpty()) {
                resultDisplayKey = outputs.get(0).itemKey();
            }
        }

        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isPresent()) {
            DisplayComponent display = displayOpt.get();
            if (resultDisplayKey != null) {
                display.setDisplayedItem(resultDisplayKey);
            } else if (def.defaultResultItem() != null) {
                display.setDisplayedItem(def.defaultResultItem());
            }
            display.setStatusText("<green>Output Ready</green>");
            display.setDirty(true);
        }
    }

    public void handlePhaseFailure(StationInstance station, ProcessDefinition def, ProcessInstance instance, PhaseDefinition phase) {
        FailurePolicy policy = phase.failurePolicy();

        if (policy == FailurePolicy.CANCEL) {
            instance.setState(ProcessState.IDLE);
            station.getComponent(ProcessComponent.class).ifPresent(ProcessComponent::clearActiveProcess);
        } else if (policy == FailurePolicy.SPOIL || policy == FailurePolicy.BYPRODUCT) {
            instance.setState(ProcessState.FAILED);
            if (phase.byproductItem() != null) {
                station.getComponent(StorageComponent.class).ifPresent(storage -> {
                    storage.addOutput(StoredItem.of(phase.byproductItem(), 1));
                });
            }
        }

        station.getComponent(DisplayComponent.class).ifPresent(display -> {
            display.setStatusText("<red>Process Failed</red>");
            display.setDirty(true);
        });
    }

    private void updateStationDisplay(StationInstance station, ProcessDefinition def, ProcessInstance instance, PhaseDefinition phase, EvaluationContext context) {
        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isEmpty()) {
            return;
        }

        DisplayComponent display = displayOpt.get();
        String formatted = displayFormatter.format(phase, def, instance, context);
        display.setStatusText(formatted);
        display.setDirty(true);
    }

    private void updateStationDisplayPaused(StationInstance station, ProcessDefinition def, ProcessInstance instance, PhaseDefinition phase, EvaluationContext context) {
        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isEmpty()) {
            return;
        }

        DisplayComponent display = displayOpt.get();
        String formatted = displayFormatter.format(phase, def, instance, context);
        display.setStatusText("<yellow>Paused: </yellow>" + formatted);
        display.setDirty(true);
    }

    private boolean matchesInputs(List<StoredItem> requirements, List<StoredItem> available) {
        if (requirements.isEmpty()) {
            return true;
        }

        for (StoredItem req : requirements) {
            int found = 0;
            for (StoredItem in : available) {
                if (in.itemKey().equals(req.itemKey())) {
                    found += in.amount();
                }
            }
            if (found < req.amount()) {
                return false;
            }
        }

        return true;
    }
}
