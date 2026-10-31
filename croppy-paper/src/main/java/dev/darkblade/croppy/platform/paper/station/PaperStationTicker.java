package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.rule.RuleExecution;
import dev.darkblade.croppy.core.station.DefaultStationManager;
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
import java.util.logging.Level;

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
    private final long tickIntervalTicks;
    private BukkitTask task;

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
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
        this.thermalEvaluator = Objects.requireNonNull(thermalEvaluator, "thermalEvaluator cannot be null");
        this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine cannot be null");
        this.displaySynchronizer = Objects.requireNonNull(displaySynchronizer, "displaySynchronizer cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.tickIntervalTicks = Math.max(1, tickIntervalTicks);
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
                plugin.getLogger().log(Level.SEVERE, "Error ticking station at " + station.getPosition(), t);
            }
        }
    }

    private void tickStation(StationInstance station) {
        BlockPosition pos = station.getPosition();
        World world = Bukkit.getWorld(pos.worldName());
        if (world == null || !world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
            return;
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

        EvaluationContext context = EvaluationContext.empty()
                .with("world.name", world.getName())
                .with("world.time", world.getTime())
                .with("station.light", world.getBlockAt(pos.x(), pos.y(), pos.z()).getLightLevel())
                .with("station.temperature", temp)
                .with("station.is_heated", isHeated)
                .with("station.thermal_zone", thermalZone)
                .with("process.state", process != null ? process.getState().name() : "NONE")
                .with("process.finished", processFinished)
                .with("process.idle_ticks", idleTicks);

        // 2. Process Engine Tick
        if (process != null && (process.getState() == ProcessState.RUNNING || process.getState() == ProcessState.AWAITING_INPUT)) {
            processEngine.tick(station, tickIntervalTicks, context);
        }

        // 3. Rule Engine Tick Evaluation
        List<RuleExecution> executions = ruleEngine.evaluateAndApply(RuleTrigger.STATION_TICK, station, context);
        for (RuleExecution exec : executions) {
            executeRulePlatformActions(world, pos, exec.appliedActions());
        }

        // 4. Multi-sensory ambient particles & sound loops
        renderAmbientEffects(world, pos, station, temp, process);

        // 5. Display Component Synchronization
        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isPresent() && displayOpt.get().isDirty()) {
            displaySynchronizer.synchronizeStationDisplay(station, world, itemFactory);
        }
    }

    private void renderAmbientEffects(World world, BlockPosition pos, StationInstance station, double temp, ProcessInstance process) {
        Location center = new Location(world, pos.x() + 0.5, pos.y() + 1.1, pos.z() + 0.5);
        String stationType = station.getStationKey().value().toLowerCase(Locale.ROOT);

        // Subtle working smoke
        if (process != null && process.getState() == ProcessState.RUNNING) {
            world.spawnParticle(Particle.SMOKE, center, 2, 0.1, 0.1, 0.1, 0.01);
        }

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
                    } catch (Exception ignored) {}
                }
            } else if ("EMIT_PARTICLES".equals(type)) {
                Object pObj = action.parameters().get("particle");
                if (pObj != null) {
                    try {
                        Particle p = Particle.valueOf(pObj.toString().toUpperCase(Locale.ROOT));
                        int count = action.parameters().get("count") instanceof Number n ? n.intValue() : 5;
                        world.spawnParticle(p, center, count, 0.2, 0.2, 0.2, 0.02);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
