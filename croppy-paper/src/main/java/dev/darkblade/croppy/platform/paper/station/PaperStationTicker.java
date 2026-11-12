package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.fuel.FuelProperties;
import dev.darkblade.croppy.api.fuel.FuelProvider;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseEffects;
import dev.darkblade.croppy.api.process.PhaseParticle;
import dev.darkblade.croppy.api.process.PhaseSound;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.rule.RuleExecution;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.StationStateEvaluator;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.platform.paper.growth.PaperDisplaySynchronizer;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Scheduled recurring ticker executing active processes, updating thermal dynamics,
 * evaluating rule triggers, and synchronizing in-world display effects synchronously.
 */
public final class PaperStationTicker implements Runnable {

    private final Plugin plugin;
    private final DefaultStationManager stationManager;
    private final ProcessEngine processEngine;
    private final PaperThermalEvaluator thermalEvaluator;
    private final RuleEngine ruleEngine;
    private final PaperDisplaySynchronizer displaySynchronizer;
    private final ItemFactory itemFactory;
    private final dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker;
    private final FuelProvider fuelProvider;
    private final ProcessRegistry processRegistry;
    private final long tickIntervalTicks;
    private final CroppyLogger logger;
    private BukkitTask task;

    public PaperStationTicker(
            Plugin plugin,
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            PaperThermalEvaluator thermalEvaluator,
            RuleEngine ruleEngine,
            PaperDisplaySynchronizer displaySynchronizer,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker,
            FuelProvider fuelProvider,
            ProcessRegistry processRegistry,
            long tickIntervalTicks,
            CroppyLogger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
        this.thermalEvaluator = Objects.requireNonNull(thermalEvaluator, "thermalEvaluator cannot be null");
        this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine cannot be null");
        this.displaySynchronizer = displaySynchronizer;
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.viewerTracker = viewerTracker;
        this.fuelProvider = fuelProvider;
        this.processRegistry = processRegistry;
        this.tickIntervalTicks = Math.max(1, tickIntervalTicks);
        this.logger = logger != null
                ? logger
                : (plugin.getLogger() != null ? new JulCroppyLogger(plugin.getLogger()) : NoOpCroppyLogger.INSTANCE);
    }

    public PaperStationTicker(
            Plugin plugin,
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            PaperThermalEvaluator thermalEvaluator,
            RuleEngine ruleEngine,
            PaperDisplaySynchronizer displaySynchronizer,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker,
            FuelProvider fuelProvider,
            ProcessRegistry processRegistry,
            long tickIntervalTicks
    ) {
        this(plugin, stationManager, processEngine, thermalEvaluator, ruleEngine, displaySynchronizer, itemFactory, viewerTracker, fuelProvider, processRegistry, tickIntervalTicks, null);
    }

    public PaperStationTicker(
            Plugin plugin,
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            PaperThermalEvaluator thermalEvaluator,
            RuleEngine ruleEngine,
            PaperDisplaySynchronizer displaySynchronizer,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker,
            FuelProvider fuelProvider,
            long tickIntervalTicks
    ) {
        this(plugin, stationManager, processEngine, thermalEvaluator, ruleEngine, displaySynchronizer, itemFactory, viewerTracker, fuelProvider, null, tickIntervalTicks);
    }

    public PaperStationTicker(
            Plugin plugin,
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            PaperThermalEvaluator thermalEvaluator,
            RuleEngine ruleEngine,
            PaperDisplaySynchronizer displaySynchronizer,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker,
            long tickIntervalTicks
    ) {
        this(plugin, stationManager, processEngine, thermalEvaluator, ruleEngine, displaySynchronizer, itemFactory, viewerTracker, null, tickIntervalTicks);
    }

    public PaperStationTicker(
            Plugin plugin,
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            PaperThermalEvaluator thermalEvaluator,
            RuleEngine ruleEngine,
            PaperDisplaySynchronizer displaySynchronizer,
            ItemFactory itemFactory,
            long tickIntervalTicks
    ) {
        this(plugin, stationManager, processEngine, thermalEvaluator, ruleEngine, displaySynchronizer, itemFactory, null, null, tickIntervalTicks);
    }

    /**
     * Starts the recurring ticker task.
     */
    public synchronized void start() {
        if (task != null && !task.isCancelled()) {
            return;
        }
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, tickIntervalTicks, tickIntervalTicks);
    }

    /**
     * Cancels the recurring ticker task.
     */
    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        for (StationInstance station : stationManager.getAllStations()) {
            try {
                tickStation(station);
            } catch (Throwable t) {
                logger.error(LogChannel.STATION, "Error ticking station at " + station.getPosition(), t);
            }
        }
    }

    private void tickStation(StationInstance station) {
        BlockPosition pos = station.getPosition();
        World world = Bukkit.getWorld(pos.worldName());
        if (world == null || !world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
            return;
        }
        tickStation(station, world);
    }

    void tickStation(StationInstance station, World world) {
        BlockPosition pos = station.getPosition();

        // 0. Heat Source Component Ticking & Fuel Auto-Replenishment
        Optional<HeatSourceComponent> heatSourceOpt = station.getComponent(HeatSourceComponent.class);
        if (heatSourceOpt.isPresent()) {
            HeatSourceComponent heatSource = heatSourceOpt.get();
            if (heatSource.isActive() && heatSource.getBurnTicksRemaining() <= 0) {
                station.getComponent(StorageComponent.class).ifPresent(storage -> tryReplenishFuel(heatSource, storage));
            }
            if (heatSource instanceof DefaultHeatSourceComponent defaultHeatSource) {
                defaultHeatSource.tick(tickIntervalTicks);
            }
        }

        // 1. Thermal Convergence Update
        thermalEvaluator.updateThermalState(station, world, 5.0);

        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        double temp = thermalOpt.map(ThermalComponent::getTemperature).orElse(20.0);
        boolean isHeated = thermalOpt.map(ThermalComponent::isHeated).orElse(false);
        String thermalZone = thermalOpt.map(t -> t.getThermalZone().name()).orElse("COLD");

        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        ProcessInstance process = (processOpt.isPresent() && processOpt.get().hasActiveProcess())
                ? processOpt.get().getActiveProcess().get()
                : null;

        boolean processFinished = process != null && process.getState() == ProcessState.COMPLETED;
        long idleTicks = processFinished ? process.getPhaseElapsedTicks() : 0L;

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        boolean hasInputs = storageOpt.isPresent() && !storageOpt.get().getInputs().isEmpty();
        boolean hasOutputs = storageOpt.isPresent() && !storageOpt.get().getOutputs().isEmpty();
        int inputCount = storageOpt.map(s -> s.getInputs().stream().mapToInt(StoredItem::amount).sum()).orElse(0);
        boolean isEmpty = !hasInputs && !hasOutputs;

        EvaluationContext context = EvaluationContext.empty()
                .with("world.name", world.getName())
                .with("world.time", world.getTime())
                .with("station.light", world.getBlockAt(pos.x(), pos.y(), pos.z()).getLightLevel())
                .with("station.temperature", temp)
                .with("station.is_heated", isHeated)
                .with("station.thermal_zone", thermalZone)
                .with("process.state", process != null ? process.getState().name() : "NONE")
                .with("process.finished", processFinished)
                .with("process.idle_ticks", idleTicks)
                .with("storage.has_inputs", hasInputs)
                .with("storage.has_outputs", hasOutputs)
                .with("storage.is_empty", isEmpty)
                .with("storage.input_count", inputCount);

        // 2. Process Engine Tick & Auto-Initiation
        if (process != null && (process.getState() == ProcessState.RUNNING || process.getState() == ProcessState.AWAITING_INPUT)) {
            processEngine.tick(station, tickIntervalTicks, context);
        } else if (hasInputs && (process == null || process.getState() == ProcessState.IDLE)) {
            boolean started = processEngine.tryStartProcess(station, context);
            if (started) {
                process = processOpt.flatMap(ProcessComponent::getActiveProcess).orElse(null);
            }
        }

        // 3. Rule Engine Tick Evaluation
        List<RuleExecution> executions = ruleEngine.evaluateAndApply(RuleTrigger.STATION_TICK, station, context);
        for (RuleExecution exec : executions) {
            executeRulePlatformActions(world, pos, exec.appliedActions());
        }

        // 4. Multi-sensory ambient particles & sound loops
        renderAmbientEffects(world, pos, station, temp, process);

        // 5. Display Component Virtual Packet Synchronization
        if (viewerTracker != null) {
            viewerTracker.syncStation(station, world, process);
        } else if (displaySynchronizer != null) {
            Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
            if (displayOpt.isPresent() && displayOpt.get().isDirty()) {
                displaySynchronizer.synchronizeStationDisplay(station, world, itemFactory);
            }
        }
    }

    private void renderAmbientEffects(World world, BlockPosition pos, StationInstance station, double temp, ProcessInstance process) {
        Location center = new Location(world, pos.x() + 0.5, pos.y() + 1.1, pos.z() + 0.5);
        String stationType = station.getStationKey().value().toLowerCase(Locale.ROOT);

        // Subtle working smoke
        if (process != null && process.getState() == ProcessState.RUNNING) {
            world.spawnParticle(Particle.SMOKE, center, 2, 0.1, 0.1, 0.1, 0.01);
        }

        // Phase-configured multi-sensory feedback
        if (process != null && process.getState() == ProcessState.RUNNING && processRegistry != null) {
            processRegistry.get(process.getProcessKey()).ifPresent(def -> {
                if (process.getCurrentPhaseIndex() < def.phases().size()) {
                    PhaseDefinition phase = def.phases().get(process.getCurrentPhaseIndex());
                    renderPhaseEffects(world, pos, phase.effects(), process.getPhaseElapsedTicks());
                }
            });
        }

        // State-configured ambient sensory feedback
        StationStateEvaluator.evaluate(station, process).ifPresent(stateDef -> {
            stateDef.getEffects().ifPresent(effects -> {
                renderPhaseEffects(world, pos, effects, world.getTime());
            });
        });

        // Pot boiling/simmering effects
        if (stationType.contains("pot") && temp >= 75.0) {
            Location potLiquid = new Location(world, pos.x() + 0.5, pos.y() + 0.45, pos.z() + 0.5);
            world.spawnParticle(Particle.SPLASH, potLiquid, 2, 0.15, 0.05, 0.15, 0.02);
            world.spawnParticle(Particle.CLOUD, potLiquid.clone().add(0, 0.2, 0), 1, 0.1, 0.1, 0.1, 0.01);

            if (temp >= 100.0 && world.getTime() % 40 == 0) {
                world.playSound(potLiquid, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.4f, 1.2f);
            }
        }

        // Skillet sizzling effects
        if ((stationType.contains("skillet") || stationType.contains("pan")) && temp >= 120.0) {
            Location panSurface = new Location(world, pos.x() + 0.5, pos.y() + 0.15, pos.z() + 0.5);
            world.spawnParticle(Particle.SMOKE, panSurface, 2, 0.1, 0.05, 0.1, 0.01);

            if (world.getTime() % 30 == 0) {
                world.spawnParticle(Particle.LAVA, panSurface, 1, 0.1, 0.02, 0.1, 0);
                world.playSound(panSurface, Sound.BLOCK_FIRE_EXTINGUISH, 0.25f, 1.5f);
            }
        }

        // Scorching hazard smoke
        if (temp >= 220.0) {
            world.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, -0.3, 0), 3, 0.15, 0.1, 0.15, 0.02);
        }

        // Heat source station colored DUST particles
        Optional<HeatSourceComponent> heatOpt = station.getComponent(HeatSourceComponent.class);
        if (heatOpt.isPresent()) {
            HeatSourceComponent hs = heatOpt.get();
            if (hs.isActive() && hs.isBurning()) {
                renderHeatSourceDustParticles(world, pos, hs);
            }
        }
    }

    private void renderHeatSourceDustParticles(World world, BlockPosition pos, HeatSourceComponent heatSource) {
        Location center = new Location(world, pos.x() + 0.5, pos.y() + 0.95, pos.z() + 0.5);
        double temp = heatSource.getOutputTemperature();

        org.bukkit.Color color;
        if (temp < 90.0) {
            // Warm amber
            color = org.bukkit.Color.fromRGB(255, 140, 20);
        } else if (temp <= 220.0) {
            // Incandescent red/orange
            color = org.bukkit.Color.fromRGB(240, 50, 10);
        } else if (temp > 350.0) {
            // Extreme alchemical cyan/blue
            color = org.bukkit.Color.fromRGB(40, 220, 255);
        } else {
            // High sear bright fiery orange
            color = org.bukkit.Color.fromRGB(255, 90, 10);
        }

        try {
            Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
            world.spawnParticle(Particle.DUST, center, 4, 0.15, 0.05, 0.15, 0.0, dust);
        } catch (Exception ex) {
            logger.trace(LogChannel.STATION, () -> "Failed to spawn thermal dust particle at " + center, ex);
        }
    }

    private void renderPhaseEffects(World world, BlockPosition pos, PhaseEffects effects, long phaseElapsedTicks) {
        if (effects == null) {
            return;
        }

        Location center = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);

        if (effects.hasParticle()) {
            PhaseParticle p = effects.particle();
            try {
                Particle particle = Particle.valueOf(p.type().toUpperCase(Locale.ROOT));
                Location pLoc = center.clone().add(0, 0.45, 0);
                world.spawnParticle(particle, pLoc, p.count(), p.offsetX(), p.offsetY(), p.offsetZ(), p.speed());
            } catch (Exception ex) {
                logger.trace(LogChannel.STATION,
                        () -> "Failed to spawn process particle '" + p.type() + "' at " + center, ex);
            }
        }

        if (effects.hasSound()) {
            PhaseSound s = effects.sound();
            if (s.intervalTicks() > 0 && phaseElapsedTicks % s.intervalTicks() == 0) {
                try {
                    String soundName = s.sound().toLowerCase(Locale.ROOT);
                    NamespacedKey soundKey = soundName.contains(":")
                            ? NamespacedKey.fromString(soundName)
                            : NamespacedKey.minecraft(soundName);
                    if (soundKey != null) {
                        Sound sound = org.bukkit.Registry.SOUNDS.get(soundKey);
                        if (sound != null) {
                            world.playSound(center, sound, s.volume(), s.pitch());
                        }
                    }
                } catch (Exception ex) {
                    logger.trace(LogChannel.STATION, () -> "Failed to play process sound at " + center, ex);
                }
            }
        }
    }

    private void tryReplenishFuel(HeatSourceComponent heatSource, StorageComponent storage) {
        if (fuelProvider == null) {
            return;
        }
        for (StoredItem item : storage.getInputs()) {
            Key itemKey = item.itemKey();
            Optional<FuelProperties> fuelOpt = fuelProvider.resolveFuel(itemKey);
            if (fuelOpt.isPresent()) {
                FuelProperties props = fuelOpt.get();
                storage.removeInput(itemKey, 1);
                if (heatSource instanceof DefaultHeatSourceComponent defaultHeatSource) {
                    defaultHeatSource.startBurn(props.burnTicks());
                } else {
                    heatSource.setBurnTicksRemaining(props.burnTicks());
                    heatSource.setMaxBurnTicks(props.burnTicks());
                }
                break;
            }
        }
    }

    private void executeRulePlatformActions(World world, BlockPosition pos, List<RuleAction> actions) {
        Location center = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        for (RuleAction action : actions) {
            String type = action.type().toUpperCase(Locale.ROOT);
            if ("PLAY_SOUND".equals(type)) {
                Object sObj = action.parameters().get("sound");
                if (sObj != null) {
                    try {
                        String sName = sObj.toString();
                        NamespacedKey sKey = sName.contains(":")
                                ? NamespacedKey.fromString(sName.toLowerCase(Locale.ROOT))
                                : NamespacedKey.minecraft(sName.toLowerCase(Locale.ROOT));
                        if (sKey != null) {
                            Sound sound = org.bukkit.Registry.SOUNDS.get(sKey);
                            if (sound != null) {
                                float vol = action.parameters().get("volume") instanceof Number n ? n.floatValue() : 1.0f;
                                float pitch = action.parameters().get("pitch") instanceof Number p ? p.floatValue() : 1.0f;
                                world.playSound(center, sound, vol, pitch);
                            }
                        }
                    } catch (Exception ex) {
                        logger.trace(LogChannel.STATION,
                                () -> "Failed to play rule action sound '" + sObj + "' at " + center, ex);
                    }
                }
            } else if ("EMIT_PARTICLES".equals(type)) {
                Object pObj = action.parameters().get("particle");
                if (pObj != null) {
                    try {
                        Particle p = Particle.valueOf(pObj.toString().toUpperCase(Locale.ROOT));
                        int count = action.parameters().get("count") instanceof Number n ? n.intValue() : 5;
                        world.spawnParticle(p, center, count, 0.2, 0.2, 0.2, 0.02);
                    } catch (Exception ex) {
                        logger.trace(LogChannel.STATION,
                                () -> "Failed to spawn rule action particle '" + pObj + "' at " + center, ex);
                    }
                }
            }
        }
    }
}
