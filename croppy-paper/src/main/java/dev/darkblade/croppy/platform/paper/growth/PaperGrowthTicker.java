package dev.darkblade.croppy.platform.paper.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.EnvironmentProvider;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.growth.GrowthResult;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import dev.darkblade.croppy.platform.paper.watering.DailyTransitionService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * Scheduled Bukkit runnable executing periodic growth simulation cycles for all active crops,
 * incorporating day tracking and daily transition checks.
 */
public class PaperGrowthTicker extends BukkitRunnable {

    private final Plugin plugin;
    private final CroppyCore core;
    private final EnvironmentProvider environmentProvider;
    private final DailyTransitionService dailyTransitionService;
    private final VirtualBlockDisplayTracker displayTracker;
    private final CroppyLogger logger;
    private BukkitTask task;

    public PaperGrowthTicker(
            Plugin plugin,
            CroppyCore core,
            EnvironmentProvider environmentProvider,
            DailyTransitionService dailyTransitionService,
            VirtualBlockDisplayTracker displayTracker,
            CroppyLogger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
        this.core = Objects.requireNonNull(core, "core must not be null");
        this.environmentProvider = Objects.requireNonNull(environmentProvider, "environmentProvider must not be null");
        this.dailyTransitionService = dailyTransitionService;
        this.displayTracker = displayTracker;
        this.logger = logger != null
                ? logger
                : (plugin.getLogger() != null ? new JulCroppyLogger(plugin.getLogger()) : NoOpCroppyLogger.INSTANCE);
    }

    public PaperGrowthTicker(Plugin plugin, CroppyCore core, EnvironmentProvider environmentProvider, DailyTransitionService dailyTransitionService, VirtualBlockDisplayTracker displayTracker) {
        this(plugin, core, environmentProvider, dailyTransitionService, displayTracker, null);
    }

    public PaperGrowthTicker(Plugin plugin, CroppyCore core, EnvironmentProvider environmentProvider, DailyTransitionService dailyTransitionService) {
        this(plugin, core, environmentProvider, dailyTransitionService, null, null);
    }

    public PaperGrowthTicker(Plugin plugin, CroppyCore core, EnvironmentProvider environmentProvider) {
        this(plugin, core, environmentProvider, null, null, null);
    }

    @Override
    public void run() {
        CropManager cropManager = core.getCropManager();
        SpeciesRegistry speciesRegistry = core.getSpeciesRegistry();
        GrowthEngine growthEngine = core.getGrowthEngine();

        // Check daily transitions for all active worlds
        if (dailyTransitionService != null) {
            for (World world : plugin.getServer().getWorlds()) {
                dailyTransitionService.checkWorldDay(world);
            }
        }

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

                World world = plugin.getServer().getWorld(crop.getPosition().worldName());
                long currentDay = (world != null) ? (world.getFullTime() / 24000L) : -1L;

                GrowthResult result = growthEngine.processCrop(crop, species, env, currentDay);
                if (result.withered()) {
                    handleWitheredCrop(world, crop);
                }
            } catch (Exception e) {
                logger.warn(LogChannel.GROWTH, "Error processing growth for crop at " + crop.getPosition(), e);
            }
        }
    }

    private void handleWitheredCrop(World world, CropInstance crop) {
        core.getCropManager().removeCrop(crop.getPosition());
        core.getStorage().ifPresent(s -> s.deleteCrop(crop.getPosition()));
        if (displayTracker != null) {
            displayTracker.removeDisplay(crop.getPosition());
        }

        if (world != null) {
            int chunkX = crop.getPosition().x() >> 4;
            int chunkZ = crop.getPosition().z() >> 4;
            if (world.isChunkLoaded(chunkX, chunkZ)) {
                Block block = world.getBlockAt(crop.getPosition().x(), crop.getPosition().y(), crop.getPosition().z());
                block.setType(Material.DEAD_BUSH);
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
        if (this.task != null && !this.task.isCancelled()) {
            this.task.cancel();
            this.task = null;
        }
    }
}
