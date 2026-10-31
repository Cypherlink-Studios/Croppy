package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.FailurePolicy;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseType;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Finite state machine execution engine governing process lifecycles and transitions.
 */
public final class ProcessEngine implements Service {

    private final ProcessRegistry processRegistry;

    public ProcessEngine(ProcessRegistry processRegistry) {
        this.processRegistry = Objects.requireNonNull(processRegistry, "processRegistry cannot be null");
    }

    /**
     * Attempts to initiate a recipe on an idle station if storage contains required inputs
     * and start conditions evaluate to true.
     *
     * @param station station instance
     * @param context evaluation context
     * @return true if a process was started
     */
    public boolean tryStartProcess(StationInstance station, EvaluationContext context) {
        Optional<ProcessComponent> processCompOpt = station.getComponent(ProcessComponent.class);
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);

        if (processCompOpt.isEmpty() || storageOpt.isEmpty()) {
            return false;
        }

        ProcessComponent processComp = processCompOpt.get();
        if (processComp.hasActiveProcess() && processComp.getActiveProcess().get().getState() != ProcessState.IDLE) {
            return false;
        }

        StorageComponent storage = storageOpt.get();
        List<StoredItem> currentInputs = storage.getInputs();
        Key stationKey = station.getStationKey();

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
            updateStationDisplay(station, def, instance, initialPhase);
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
        instance.addPhaseElapsedTicks(deltaTicks);

        // Evaluation context enriched with process variables
        EvaluationContext phaseContext = context
                .with("phase.elapsed_ticks", instance.getPhaseElapsedTicks())
                .with("process.total_ticks", instance.getTotalElapsedTicks());

        // Check phase timeouts
        if (phase.timeoutTicks() > 0 && instance.getPhaseElapsedTicks() >= phase.timeoutTicks()) {
            handlePhaseFailure(station, def, instance, phase);
            return;
        }

        switch (phase.type()) {
            case TIMED -> {
                if (phase.condition().evaluate(phaseContext)) {
                    if (instance.getPhaseElapsedTicks() >= phase.durationTicks()) {
                        advancePhase(station, def, instance);
                    } else {
                        updateStationDisplay(station, def, instance, phase);
                    }
                } else {
                    // Condition violation during timed phase
                    handlePhaseFailure(station, def, instance, phase);
                }
            }
            case ENVIRONMENTAL -> {
                if (phase.condition().evaluate(phaseContext)) {
                    advancePhase(station, def, instance);
                } else {
                    updateStationDisplay(station, def, instance, phase);
                }
            }
            case MANUAL_INTERACTION -> {
                if (instance.getState() != ProcessState.AWAITING_INPUT) {
                    instance.setState(ProcessState.AWAITING_INPUT);
                }
                updateStationDisplay(station, def, instance, phase);
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

        advancePhase(station, def, instance);
        return true;
    }

    /**
     * Advances to the next phase or finishes the process.
     */
    public void advancePhase(StationInstance station, ProcessDefinition def, ProcessInstance instance) {
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
            updateStationDisplay(station, def, instance, nextPhase);
        }
    }

    public void completeProcess(StationInstance station, ProcessDefinition def, ProcessInstance instance) {
        instance.setState(ProcessState.COMPLETED);

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isPresent()) {
            StorageComponent storage = storageOpt.get();
            @SuppressWarnings("unchecked")
            List<StoredItem> originalInputs = (List<StoredItem>) instance.getContextVariables()
                    .getOrDefault("original_inputs", List.of());

            List<StoredItem> outputs;
            if (def.outputResolver() != null) {
                outputs = def.outputResolver().resolveOutputs(def, instance, originalInputs);
            } else if (def.defaultResultItem() != null) {
                outputs = List.of(StoredItem.of(def.defaultResultItem(), def.defaultResultAmount()));
            } else {
                outputs = List.of();
            }

            for (StoredItem out : outputs) {
                storage.addOutput(out);
            }
        }

        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isPresent()) {
            DisplayComponent display = displayOpt.get();
            if (def.defaultResultItem() != null) {
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

    private void updateStationDisplay(StationInstance station, ProcessDefinition def, ProcessInstance instance, PhaseDefinition phase) {
        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isEmpty()) {
            return;
        }

        DisplayComponent display = displayOpt.get();
        if (phase.type() == PhaseType.TIMED && phase.durationTicks() > 0) {
            int percent = (int) Math.min(100, (instance.getPhaseElapsedTicks() * 100) / phase.durationTicks());
            display.setStatusText("<gray>" + def.displayName() + " (" + percent + "%)</gray>");
        } else if (phase.type() == PhaseType.MANUAL_INTERACTION) {
            display.setStatusText(phase.promptMessage() != null ? phase.promptMessage() : "<yellow>Awaiting interaction...</yellow>");
        }
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
