package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled recurring ticker executing active processes and updating in-world display effects
 * synchronously on the server main thread.
 */
public final class PaperStationTicker implements Runnable {

    private final Plugin plugin;
    private final DefaultStationManager stationManager;
    private final ProcessEngine processEngine;
    private final long tickIntervalTicks;
    private BukkitTask task;

    public PaperStationTicker(Plugin plugin, DefaultStationManager stationManager, ProcessEngine processEngine, long tickIntervalTicks) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
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
        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        if (processOpt.isPresent() && processOpt.get().hasActiveProcess()) {
            ProcessInstance process = processOpt.get().getActiveProcess().get();
            if (process.getState() == ProcessState.RUNNING || process.getState() == ProcessState.AWAITING_INPUT) {
                BlockPosition pos = station.getPosition();
                World world = Bukkit.getWorld(pos.worldName());
                if (world != null && world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
                    EvaluationContext context = EvaluationContext.empty()
                            .with("world.name", world.getName())
                            .with("world.time", world.getTime())
                            .with("station.light", world.getBlockAt(pos.x(), pos.y(), pos.z()).getLightLevel());

                    processEngine.tick(station, tickIntervalTicks, context);

                    // Render subtle working smoke particles
                    if (process.getState() == ProcessState.RUNNING) {
                        Location center = new Location(world, pos.x() + 0.5, pos.y() + 1.1, pos.z() + 0.5);
                        world.spawnParticle(Particle.SMOKE, center, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        // Check display component dirty updates
        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isPresent() && displayOpt.get().isDirty()) {
            DisplayComponent display = displayOpt.get();
            display.setDirty(false);
        }
    }
}
