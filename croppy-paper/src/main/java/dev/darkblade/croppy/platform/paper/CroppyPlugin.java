package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.world.ChunkPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import dev.darkblade.croppy.core.fertilizer.YamlFertilizerLoader;
import dev.darkblade.croppy.core.growth.FertilizerGrowthModifier;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.item.YamlItemLoader;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import dev.darkblade.croppy.platform.paper.command.CroppyCommands;
import dev.darkblade.croppy.platform.paper.command.parser.SpeciesParser;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.event.CropGrowEvent;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.growth.PaperDisplaySynchronizer;
import dev.darkblade.croppy.platform.paper.growth.PaperEnvironmentProvider;
import dev.darkblade.croppy.platform.paper.growth.PaperGrowthTicker;
import dev.darkblade.croppy.platform.paper.growth.RainHydrationTask;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.listener.ChunkLifecycleListener;
import dev.darkblade.croppy.platform.paper.listener.CropBreakListener;
import dev.darkblade.croppy.platform.paper.listener.CropGrowthListener;
import dev.darkblade.croppy.platform.paper.listener.CropPhysicsListener;
import dev.darkblade.croppy.platform.paper.listener.CropPlantListener;
import dev.darkblade.croppy.platform.paper.listener.FertilizerListener;
import dev.darkblade.croppy.platform.paper.listener.InspectionToolListener;
import dev.darkblade.croppy.platform.paper.listener.TimeSkipListener;
import dev.darkblade.croppy.platform.paper.listener.WateringListener;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.storage.CropStorageFactory;
import dev.darkblade.croppy.platform.paper.storage.LibbyDependencyLoader;
import dev.darkblade.croppy.platform.paper.watering.DailyTransitionService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
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

    // Soil moisture & fertilizer services
    private ItemFactory itemFactory;
    private FertilizerRegistry fertilizerRegistry;
    private FertilizerService fertilizerService;
    private WateringCanService wateringCanService;
    private DailyTransitionService dailyTransitionService;
    private BukkitTask rainTask;

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

            setupItemDefinitions();
            setupFertilizers();
            setupCropDefinitions();
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
        pm.registerEvents(new CropPlantListener(core, this.fertilizerService), this);
        pm.registerEvents(new CropGrowthListener(core), this);
        pm.registerEvents(new CropBreakListener(core), this);
        pm.registerEvents(new CropPhysicsListener(core), this);
        pm.registerEvents(new InspectionToolListener(this, this.inspectorService, this.pluginConfig), this);
        pm.registerEvents(new WateringListener(this.wateringCanService, core.getCropManager()), this);
        pm.registerEvents(new TimeSkipListener(this.dailyTransitionService), this);
        pm.registerEvents(new FertilizerListener(this.fertilizerService, core.getCropManager()), this);
        getLogger().info("Registered crop lifecycle, chunk, inspection, watering, and fertilizer listeners.");
    }

    private void setupItemDefinitions() {
        Path itemsDir = getDataFolder().toPath().resolve("items");
        if (!Files.exists(itemsDir)) {
            // Seeds
            saveResourceIfNotExists("items/seeds/wheat_seeds.yml");
            saveResourceIfNotExists("items/seeds/carrot_seeds.yml");
            saveResourceIfNotExists("items/seeds/potato_seeds.yml");
            // Tools
            saveResourceIfNotExists("items/tools/inspector.yml");
            // Watering Cans
            saveResourceIfNotExists("items/watering_cans/rusty_can.yml");
            saveResourceIfNotExists("items/watering_cans/copper_can.yml");
            saveResourceIfNotExists("items/watering_cans/silver_can.yml");
            saveResourceIfNotExists("items/watering_cans/gold_can.yml");
            saveResourceIfNotExists("items/watering_cans/iridium_can.yml");
            // Fertilizers
            saveResourceIfNotExists("items/fertilizers/speed_gro.yml");
            saveResourceIfNotExists("items/fertilizers/retaining_soil.yml");
            saveResourceIfNotExists("items/fertilizers/quality_fertilizer.yml");
        }

        YamlItemLoader itemLoader = new YamlItemLoader();
        int loaded = itemLoader.loadAllCategories(itemsDir, getLogger(), core.getItemRegistry());
        this.itemFactory = new ItemFactory(this, core.getItemRegistry());
        getLogger().info("Registered " + loaded + " custom items across " + core.getItemRegistry().getCategories().size() + " categories.");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        Path target = getDataFolder().toPath().resolve(resourcePath);
        if (!Files.exists(target)) {
            try {
                saveResource(resourcePath, false);
            } catch (Exception e) {
                getLogger().warning("Could not extract default resource '" + resourcePath + "': " + e.getMessage());
            }
        }
    }

    private void setupFertilizers() {
        this.fertilizerRegistry = new DefaultFertilizerRegistry();
        this.fertilizerService = new FertilizerService(this, this.fertilizerRegistry, this.itemFactory);
        this.wateringCanService = new WateringCanService(this, this.itemFactory);
        this.dailyTransitionService = new DailyTransitionService(core.getCropManager(), this.fertilizerRegistry);

        Path fertDir = getDataFolder().toPath().resolve("items").resolve("fertilizers");
        if (!Files.exists(fertDir)) {
            saveResourceIfNotExists("items/fertilizers/speed_gro.yml");
            saveResourceIfNotExists("items/fertilizers/retaining_soil.yml");
            saveResourceIfNotExists("items/fertilizers/quality_fertilizer.yml");
        }

        YamlFertilizerLoader fertLoader = new YamlFertilizerLoader();
        int loaded = fertLoader.loadDirectory(fertDir, getLogger(), this.fertilizerRegistry);
        getLogger().info("Registered " + loaded + " fertilizer definitions into FertilizerRegistry.");
    }

    private void setupGrowthSimulation() {
        long tickInterval = getConfig().getLong("growth.tick-interval", 20L);
        double baseProgress = getConfig().getDouble("growth.base-progress", 1.0);
        int waterSearchRadius = getConfig().getInt("growth.water-search-radius", 4);

        GrowthEngine growthEngine = core.getGrowthEngine();
        growthEngine.setBaseProgress(baseProgress);
        growthEngine.addModifier(new FertilizerGrowthModifier(this.fertilizerRegistry));

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

        this.growthTicker = new PaperGrowthTicker(this, core, this.environmentProvider, this.dailyTransitionService);
        this.growthTicker.start(tickInterval);

        // Schedule periodic rain hydration task (every 5 seconds / 100 ticks)
        this.rainTask = getServer().getScheduler().runTaskTimer(this, new RainHydrationTask(getServer(), core.getCropManager()), 100L, 100L);

        getLogger().info("Started PaperGrowthTicker with interval of " + tickInterval + " ticks (base progress: " + baseProgress + ").");
    }

    private void setupCommands() {
        this.inspectorService = new CropInspectorService(
                core.getCropManager(),
                core.getSpeciesRegistry(),
                this.environmentProvider,
                this.fertilizerRegistry,
                getServer()
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
        annotationParser.parse(new CroppyCommands(
                this,
                this.inspectorService,
                this.pluginConfig,
                this.wateringCanService,
                this.fertilizerService,
                this.fertilizerRegistry,
                this.itemFactory,
                this.core.getItemRegistry()
        ));
        getLogger().info("Registered Cloud 2.0 command manager and /croppy commands.");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.pluginConfig.load(getConfig());
        this.core.getSpeciesRegistry().clear();
        this.core.getItemRegistry().clear();
        this.fertilizerRegistry.clear();
        setupItemDefinitions();
        setupFertilizers();
        setupCropDefinitions();
        getLogger().info("Croppy configuration, items, fertilizers, and crop definitions reloaded successfully.");
    }

    private void setupCropDefinitions() {
        Path cropsDir = getDataFolder().toPath().resolve("crops");
        if (!Files.exists(cropsDir)) {
            saveResourceIfNotExists("crops/wheat.yml");
            saveResourceIfNotExists("crops/carrot.yml");
            saveResourceIfNotExists("crops/potato.yml");
        }

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        List<Species> loaded = loader.loadDirectory(cropsDir, getLogger());
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

        if (this.rainTask != null && !this.rainTask.isCancelled()) {
            this.rainTask.cancel();
            this.rainTask = null;
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

    public CroppyCore getCore() {
        return core;
    }

    public PaperGrowthTicker getGrowthTicker() {
        return growthTicker;
    }

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

    public FertilizerRegistry getFertilizerRegistry() {
        return fertilizerRegistry;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public FertilizerService getFertilizerService() {
        return fertilizerService;
    }

    public WateringCanService getWateringCanService() {
        return wateringCanService;
    }

    public DailyTransitionService getDailyTransitionService() {
        return dailyTransitionService;
    }
}
