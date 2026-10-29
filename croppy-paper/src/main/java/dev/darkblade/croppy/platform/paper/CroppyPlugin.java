package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.world.ChunkPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import dev.darkblade.croppy.platform.paper.command.CroppyCommands;
import dev.darkblade.croppy.platform.paper.command.parser.SpeciesParser;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.event.CropGrowEvent;
import dev.darkblade.croppy.platform.paper.growth.PaperDisplaySynchronizer;
import dev.darkblade.croppy.platform.paper.growth.PaperEnvironmentProvider;
import dev.darkblade.croppy.platform.paper.growth.PaperGrowthTicker;
import dev.darkblade.croppy.platform.paper.listener.ChunkLifecycleListener;
import dev.darkblade.croppy.platform.paper.listener.CropBreakListener;
import dev.darkblade.croppy.platform.paper.listener.CropGrowthListener;
import dev.darkblade.croppy.platform.paper.listener.CropPhysicsListener;
import dev.darkblade.croppy.platform.paper.listener.CropPlantListener;
import dev.darkblade.croppy.platform.paper.listener.InspectionToolListener;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.storage.CropStorageFactory;
import dev.darkblade.croppy.platform.paper.storage.LibbyDependencyLoader;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Paper plugin adapter bridging server lifecycle events to the decoupled core.
 */
public class CroppyPlugin extends JavaPlugin {

    private CroppyCore core;
    private CropStorage storage;
    private LibbyDependencyLoader libbyLoader;
    private PaperGrowthTicker growthTicker;
    private PaperEnvironmentProvider environmentProvider;
    private PaperDisplaySynchronizer displaySynchronizer;
    private PluginConfig pluginConfig;
    private CropInspectorService inspectorService;

    @Override
    public void onEnable() {
        getLogger().info("Enabling Croppy plugin on Paper 1.21.8...");
        saveDefaultConfig();

        this.pluginConfig = new PluginConfig();
        this.pluginConfig.load(getConfig());

        this.core = new CroppyCore(getLogger());
        try {
            this.core.initialize();

            this.libbyLoader = new LibbyDependencyLoader(this);
            this.storage = CropStorageFactory.create(this, libbyLoader);
            this.core.setStorage(storage);

            setupSpeciesDefinitions();
            loadActiveChunkCrops();
            setupGrowthSimulation();
            setupCommands();
            registerListeners();
            getLogger().info("Croppy enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Croppy: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void loadActiveChunkCrops() {
        int loadedCount = 0;
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Collection<CropInstance> crops = storage.loadChunk(world.getName(), chunk.getX(), chunk.getZ());
                for (CropInstance crop : crops) {
                    if (!core.getCropManager().hasCrop(crop.getPosition())) {
                        core.getCropManager().registerCrop(crop);
                        loadedCount++;
                    }
                }
            }
        }
        getLogger().info("Restored " + loadedCount + " crop instances from currently loaded chunks.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ChunkLifecycleListener(core.getCropManager(), storage, getLogger()), this);
        pm.registerEvents(new CropPlantListener(core), this);
        pm.registerEvents(new CropGrowthListener(core), this);
        pm.registerEvents(new CropBreakListener(core), this);
        pm.registerEvents(new CropPhysicsListener(core), this);
        pm.registerEvents(new InspectionToolListener(this, this.inspectorService, this.pluginConfig), this);
        getLogger().info("Registered crop lifecycle, chunk, and inspection listeners.");
    }

    private void setupGrowthSimulation() {
        long tickInterval = getConfig().getLong("growth.tick-interval", 20L);
        double baseProgress = getConfig().getDouble("growth.base-progress", 1.0);
        int waterSearchRadius = getConfig().getInt("growth.water-search-radius", 4);

        GrowthEngine growthEngine = core.getGrowthEngine();
        growthEngine.setBaseProgress(baseProgress);

        this.environmentProvider = new PaperEnvironmentProvider(getServer(), waterSearchRadius);
        this.displaySynchronizer = new PaperDisplaySynchronizer(getServer(), getLogger());

        growthEngine.setDisplaySynchronizer(this.displaySynchronizer);
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
            if (!event.isCancelled()) {
                storage.saveCrop(crop);
                return true;
            }
            return false;
        });

        this.growthTicker = new PaperGrowthTicker(this, core, this.environmentProvider);
        this.growthTicker.start(tickInterval);
        getLogger().info("Started PaperGrowthTicker with interval of " + tickInterval + " ticks (base progress: " + baseProgress + ").");
    }

    private void setupCommands() {
        this.inspectorService = new CropInspectorService(
                core.getCropManager(),
                core.getSpeciesRegistry(),
                this.environmentProvider
        );

        PaperCommandManager<CommandSourceStack> commandManager = PaperCommandManager.builder()
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(this);

        if (commandManager.hasBrigadierManager()) {
            commandManager.brigadierManager().registerMapping(
                    new TypeToken<SpeciesParser<CommandSourceStack>>() {},
                    builder -> builder.toConstant(ArgumentTypes.key()).cloudSuggestions()
            );
        }

        commandManager.parserRegistry().registerParser(SpeciesParser.speciesParser(core.getSpeciesRegistry()));

        AnnotationParser<CommandSourceStack> annotationParser = new AnnotationParser<>(
                commandManager,
                CommandSourceStack.class
        );
        annotationParser.parse(new CroppyCommands(this, this.inspectorService, this.pluginConfig));
        getLogger().info("Registered Cloud 2.0 command manager and /croppy commands.");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.pluginConfig.load(getConfig());
        this.core.getSpeciesRegistry().clear();
        setupSpeciesDefinitions();
        getLogger().info("Croppy configuration and species definitions reloaded successfully.");
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

        flushActiveCrops();

        if (this.storage != null) {
            this.storage.shutdown();
            this.storage = null;
        }
        if (this.core != null) {
            this.core.shutdown();
        }
        getLogger().info("Croppy disabled cleanly.");
    }

    private void flushActiveCrops() {
        if (this.storage == null || this.core == null) {
            return;
        }

        Map<ChunkPosition, List<CropInstance>> chunkMap = new HashMap<>();

        for (CropInstance crop : core.getCropManager().getAllCrops()) {
            ChunkPosition coord = crop.getPosition().toChunkPosition();
            chunkMap.computeIfAbsent(coord, k -> new ArrayList<>()).add(crop);
        }

        for (Map.Entry<ChunkPosition, List<CropInstance>> entry : chunkMap.entrySet()) {
            storage.saveChunk(entry.getKey().worldName(), entry.getKey().x(), entry.getKey().z(), entry.getValue());
        }
        getLogger().info("Flushed " + core.getCropManager().count() + " active crop instances across " + chunkMap.size() + " chunks.");
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

    /**
     * @return the active crop storage engine
     */
    public CropStorage getStorage() {
        return storage;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public CropInspectorService getInspectorService() {
        return inspectorService;
    }

    public PaperDisplaySynchronizer getDisplaySynchronizer() {
        return displaySynchronizer;
    }

    public PaperEnvironmentProvider getEnvironmentProvider() {
        return environmentProvider;
    }
}
