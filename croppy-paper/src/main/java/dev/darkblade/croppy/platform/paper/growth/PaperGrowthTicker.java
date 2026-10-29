package dev.darkblade.croppy.platform.paper.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.EnvironmentProvider;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Scheduled Bukkit runnable executing periodic growth simulation cycles for all active crops.
 */
public class PaperGrowthTicker extends BukkitRunnable {

    private final Plugin plugin;
    private final CroppyCore core;
    private final EnvironmentProvider environmentProvider;
    private BukkitTask task;

    public PaperGrowthTicker(Plugin plugin, CroppyCore core, EnvironmentProvider environmentProvider) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
        this.core = Objects.requireNonNull(core, "core must not be null");
        this.environmentProvider = Objects.requireNonNull(environmentProvider, "environmentProvider must not be null");
    }

    @Override
    public void run() {
        CropManager cropManager = core.getCropManager();
        SpeciesRegistry speciesRegistry = core.getSpeciesRegistry();
        GrowthEngine growthEngine = core.getGrowthEngine();

        for (CropInstance crop : cropManager.getAllCrops()) {
            try {
                Species species = speciesRegistry.get(crop.getSpeciesKey()).orElse(null);
                if (species == null) {
                    continue;
                }

                if (crop.isMature(species)) {
                    continue;
                }

                CropEnvironment env = environmentProvider.getEnvironment(crop.getPosition());
                if (!env.isLoaded()) {
                    continue;
                }

                growthEngine.processCrop(crop, species, env);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing growth for crop at " + crop.getPosition(), e);
            }
        }
    }

    /**
     * Starts the periodic ticker task with the specified interval.
     *
     * @param tickInterval interval in server ticks
     */
    public synchronized void start(long tickInterval) {
        stop();
        this.task = this.runTaskTimer(plugin, tickInterval, tickInterval);
    }

    /**
     * Stops the ticker task if running.
     */
    public synchronized void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * @return true if the ticker task is currently active
     */
    public boolean isRunning() {
        return this.task != null && !this.task.isCancelled();
    }
}
