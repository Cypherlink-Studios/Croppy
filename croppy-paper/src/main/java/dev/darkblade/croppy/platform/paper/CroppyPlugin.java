package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import dev.darkblade.croppy.platform.paper.event.CropGrowEvent;
import dev.darkblade.croppy.platform.paper.growth.PaperDisplaySynchronizer;
import dev.darkblade.croppy.platform.paper.growth.PaperEnvironmentProvider;
import dev.darkblade.croppy.platform.paper.growth.PaperGrowthTicker;
import dev.darkblade.croppy.platform.paper.listener.CropBreakListener;
import dev.darkblade.croppy.platform.paper.listener.CropGrowthListener;
import dev.darkblade.croppy.platform.paper.listener.CropPhysicsListener;
import dev.darkblade.croppy.platform.paper.listener.CropPlantListener;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main Paper plugin adapter bridging server lifecycle events to the decoupled core.
 */
public class CroppyPlugin extends JavaPlugin {

    private CroppyCore core;
    private PaperGrowthTicker growthTicker;

    @Override
    public void onEnable() {
        getLogger().info("Enabling Croppy plugin on Paper 1.21.8...");
        saveDefaultConfig();

        this.core = new CroppyCore(getLogger());
        try {
            this.core.initialize();
            setupSpeciesDefinitions();
            registerListeners();
            setupGrowthSimulation();
            getLogger().info("Croppy enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Croppy: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new CropPlantListener(core), this);
        pm.registerEvents(new CropGrowthListener(core), this);
        pm.registerEvents(new CropBreakListener(core), this);
        pm.registerEvents(new CropPhysicsListener(core), this);
        getLogger().info("Registered crop lifecycle listeners.");
    }

    private void setupGrowthSimulation() {
        long tickInterval = getConfig().getLong("growth.tick-interval", 20L);
        double baseProgress = getConfig().getDouble("growth.base-progress", 1.0);
        int waterSearchRadius = getConfig().getInt("growth.water-search-radius", 4);

        GrowthEngine growthEngine = core.getGrowthEngine();
        growthEngine.setBaseProgress(baseProgress);

        PaperEnvironmentProvider envProvider = new PaperEnvironmentProvider(getServer(), waterSearchRadius);
        PaperDisplaySynchronizer displaySync = new PaperDisplaySynchronizer(getServer(), getLogger());

        growthEngine.setDisplaySynchronizer(displaySync);
        growthEngine.setStageAdvanceInterceptor((crop, species, fromStage, toStage) -> {
            World world = getServer().getWorld(crop.getPosition().worldName());
            if (world == null) {
                return false;
            }
            Block block = world.getBlockAt(crop.getPosition().x(), crop.getPosition().y(), crop.getPosition().z());
            StageDefinition prevStage = species.getStage(fromStage);
            StageDefinition nextStage = species.getStage(toStage);

            CropGrowEvent event = new CropGrowEvent(crop, species, block, fromStage, toStage, prevStage, nextStage);
            getServer().getPluginManager().callEvent(event);
            return !event.isCancelled();
        });

        this.growthTicker = new PaperGrowthTicker(this, core, envProvider);
        this.growthTicker.start(tickInterval);
        getLogger().info("Started PaperGrowthTicker with interval of " + tickInterval + " ticks (base progress: " + baseProgress + ").");
    }

    private void setupSpeciesDefinitions() {
        Path speciesDir = getDataFolder().toPath().resolve("species");
        if (!Files.exists(speciesDir)) {
            saveResource("species/wheat.yml", false);
            saveResource("species/carrot.yml", false);
            saveResource("species/potato.yml", false);
        }

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        List<Species> loaded = loader.loadDirectory(speciesDir, getLogger());
        for (Species species : loaded) {
            core.getSpeciesRegistry().register(species);
        }
        getLogger().info("Registered " + core.getSpeciesRegistry().size() + " crop species into SpeciesRegistry.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Croppy plugin...");
        if (this.growthTicker != null) {
            this.growthTicker.stop();
            this.growthTicker = null;
        }
        if (this.core != null) {
            this.core.shutdown();
        }
        getLogger().info("Croppy disabled cleanly.");
    }

    /**
     * @return the central core domain orchestrator
     */
    public CroppyCore getCore() {
        return core;
    }

    /**
     * @return the active growth ticker task
     */
    public PaperGrowthTicker getGrowthTicker() {
        return growthTicker;
    }
}
