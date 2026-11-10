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
import dev.darkblade.croppy.platform.paper.command.parser.KeyParser;
import dev.darkblade.croppy.platform.paper.command.parser.SpeciesParser;
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
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Main Paper plugin adapter bridging server lifecycle events to the decoupled
 * core.
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

    // Station & Process services
    private dev.darkblade.croppy.api.storage.StationStorage stationStorage;
    private dev.darkblade.croppy.platform.paper.station.PaperStationTicker stationTicker;
    private dev.darkblade.croppy.platform.paper.station.display.PacketEventsStationRenderer packetStationRenderer;
    private dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker stationViewerTracker;
    private dev.darkblade.croppy.platform.paper.menu.PaperMenuRenderer menuRenderer;
    private dev.darkblade.croppy.platform.paper.menu.PaperMenuTicker menuTicker;
    private dev.darkblade.croppy.platform.paper.menu.PaperMenuListener menuListener;
    private dev.darkblade.croppy.platform.paper.menu.PaperMenuService menuService;
    private dev.darkblade.croppy.platform.paper.prompt.ChatPromptService chatPromptService;
    private dev.darkblade.croppy.platform.paper.fuel.PaperFuelService fuelService;
    private BukkitTask menuTickerTask;
    private dev.darkblade.croppy.platform.paper.station.listener.StationInteractListener stationInteractListener;
    private dev.darkblade.croppy.platform.paper.station.listener.StationBlockListener stationBlockListener;
    private dev.darkblade.croppy.platform.paper.i18n.PaperMessageService messageService;

    // Recipe & Discovery services
    private dev.darkblade.croppy.api.storage.RecipeKnowledgeStorage recipeStorage;
    private dev.darkblade.croppy.platform.paper.menu.CompendiumMenuController compendiumController;
    private dev.darkblade.croppy.platform.paper.menu.ItemCatalogController itemCatalogController;
    private dev.darkblade.croppy.platform.paper.menu.RecipeDetailMenuController recipeDetailController;
    private dev.darkblade.croppy.platform.paper.discovery.StationExperimentationHandler experimentationHandler;
    private dev.darkblade.croppy.platform.paper.discovery.DiscoveryFeedbackHelper feedbackHelper;

    // 3D Virtual Block Display services
    private dev.darkblade.croppy.platform.paper.display.PacketEventsBlockRenderer packetBlockRenderer;
    private dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker virtualBlockDisplayTracker;
    private dev.darkblade.croppy.platform.paper.display.VirtualInteractionPacketListener virtualInteractionListener;
    private dev.darkblade.croppy.platform.paper.station.listener.StationVirtualInteractionPacketListener stationVirtualInteractionListener;
    private BukkitTask displayTrackerTask;

    // External Integrations
    private dev.darkblade.croppy.integration.common.IntegrationManager integrationManager;
    private dev.darkblade.croppy.api.protection.ProtectionPort protectionPort = dev.darkblade.croppy.integration.common.NoOpProtectionAdapter.INSTANCE;
    private dev.darkblade.croppy.api.economy.EconomyPort economyPort = dev.darkblade.croppy.integration.common.NoOpEconomyAdapter.INSTANCE;

    // Addon System
    private dev.darkblade.croppy.platform.paper.addon.PaperAddonManager addonManager;
    private dev.darkblade.croppy.platform.paper.resourcepack.PaperResourcePackService resourcePackService;
    private dev.darkblade.croppy.platform.paper.update.PaperUpdateService updateService;
    private dev.darkblade.croppy.platform.paper.crafting.PaperCraftingRecipeService craftingRecipeService;

    public dev.darkblade.croppy.platform.paper.crafting.PaperCraftingRecipeService getCraftingRecipeService() {
        return craftingRecipeService;
    }

    public dev.darkblade.croppy.platform.paper.addon.PaperAddonManager getAddonManager() {
        return addonManager;
    }

    public dev.darkblade.croppy.platform.paper.resourcepack.PaperResourcePackService getResourcePackService() {
        return resourcePackService;
    }

    public dev.darkblade.croppy.api.update.UpdateService getUpdateService() {
        return updateService;
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig();
        this.pluginConfig.load(getConfig());

        this.integrationManager = new dev.darkblade.croppy.integration.common.IntegrationManager(getLogger());

        // Register WorldGuard integration early in onLoad for custom flag registration
        dev.darkblade.croppy.integration.worldguard.WorldGuardIntegration wg = new dev.darkblade.croppy.integration.worldguard.WorldGuardIntegration(
                getLogger(),
                this.pluginConfig.isWorldGuardEnabled(),
                this.pluginConfig.isWorldGuardUseCustomFlags());
        this.integrationManager.register(wg);
        this.integrationManager.onLoad();
    }

    @Override
    public void onEnable() {
        getLogger().info("Enabling Croppy plugin...");
        saveDefaultConfig();

        if (this.pluginConfig == null) {
            this.pluginConfig = new PluginConfig();
            this.pluginConfig.load(getConfig());
        }

        this.core = new CroppyCore(getLogger());
        try {
            preloadShutdownClasses();
            this.core.initialize();

            setupIntegrations();

            this.libbyLoader = new LibbyDependencyLoader(this);
            this.storage = CropStorageFactory.create(this, libbyLoader);
            this.core.setStorage(storage);
            this.stationStorage = new dev.darkblade.croppy.platform.paper.storage.PdcStationStorage(this,
                    core.getStationRegistry());

            setupLocalization();
            setupItemDefinitions();
            setupFertilizers();
            setupCropDefinitions();
            setupProcessDefinitions();
            setupRecipeDefinitions();
            setupMenuDefinitions();
            setupStationDefinitions();
            setupInteractionDefinitions();
            setupRuleDefinitions();
            setupCraftingRecipeDefinitions();
            setupMenuServices();
            setupGrowthSimulation();
            loadActiveChunkCrops();
            loadActiveChunkStations();
            setupAddonSystem();
            setupResourcePackPipeline();
            setupUpdateSubsystem();
            setupCommands();
            registerListeners();
            getLogger().info("Croppy enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize Croppy", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupAddonSystem() {
        Path addonsDir = getDataFolder().toPath().resolve("addons");
        if (!Files.exists(addonsDir)) {
            try {
                Files.createDirectories(addonsDir);
            } catch (Exception ignored) {
            }
        }
        Path resourcePackDir = getDataFolder().toPath().resolve("resourcepack");
        dev.darkblade.croppy.platform.paper.resourcepack.ResourcePackMerger merger = new dev.darkblade.croppy.platform.paper.resourcepack.ResourcePackMerger(
                resourcePackDir, getLogger());
        this.addonManager = new dev.darkblade.croppy.platform.paper.addon.PaperAddonManager(this, this.core, addonsDir,
                merger);
        this.addonManager.enableAll();
    }

    private void setupResourcePackPipeline() {
        this.resourcePackService = new dev.darkblade.croppy.platform.paper.resourcepack.PaperResourcePackService(this,
                this.pluginConfig);
        this.resourcePackService.start();
    }

    private void setupUpdateSubsystem() {
        dev.darkblade.croppy.api.update.SemanticVersion currentVersion;
        try {
            currentVersion = dev.darkblade.croppy.api.update.SemanticVersion.parse(getPluginMeta().getVersion());
        } catch (Exception e) {
            currentVersion = dev.darkblade.croppy.api.update.SemanticVersion.of(0, 0, 0);
        }

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();

        dev.darkblade.croppy.platform.paper.update.GitHubReleaseClient releaseClient = new dev.darkblade.croppy.platform.paper.update.GitHubReleaseClient(
                httpClient, currentVersion);

        java.nio.file.Path updateDir = getDataFolder().getParentFile().toPath().resolve("update");
        java.util.function.Supplier<String> jarNameSupplier = () -> {
            try {
                return new java.io.File(CroppyPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .getName();
            } catch (Exception e) {
                return "Croppy.jar";
            }
        };

        this.updateService = new dev.darkblade.croppy.platform.paper.update.PaperUpdateService(
                releaseClient,
                updateDir,
                jarNameSupplier,
                this.pluginConfig.getUpdatesChannel(),
                this.pluginConfig.isUpdatesEnabled(),
                getLogger());

        if (this.pluginConfig.isUpdatesEnabled() && this.pluginConfig.isUpdatesCheckOnStartup()) {
            this.updateService.checkForUpdatesAsync(false).thenAccept(result -> {
                if (result.updateAvailable()) {
                    getLogger().warning("A new update for Croppy is available: v"
                            + result.latestVersion().map(dev.darkblade.croppy.api.update.SemanticVersion::asString)
                                    .orElse("?")
                            + " (current: v" + result.currentVersion().asString() + ") ["
                            + this.pluginConfig.getUpdatesChannel() + "]");
                    getLogger().warning(
                            "Run '/croppy update apply' to download and stage the update for the next server restart.");
                }
            });
        }
    }

    private void loadActiveChunkCrops() {
        int loadedCount = 0;
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Collection<CropInstance> crops = storage.loadChunk(world.getName(), chunk.getX(), chunk.getZ());
                for (CropInstance crop : crops) {
                    if (!core.getCropManager().hasCrop(crop.getPosition())) {
                        dev.darkblade.croppy.api.species.Species species = core.getSpeciesRegistry()
                                .get(crop.getSpeciesKey()).orElse(null);
                        int height = (species != null) ? species.getHeight() : 1;
                        core.getCropManager().registerCrop(crop, height);
                        loadedCount++;

                        if (species != null && this.displaySynchronizer != null && !species.getStages().isEmpty()) {
                            int stageIdx = Math.min(crop.getStageIndex(), species.getStages().size() - 1);
                            this.displaySynchronizer.synchronizeDisplay(crop, species,
                                    species.getStages().get(stageIdx));
                        }
                    }
                }
            }
        }
        getLogger().info("Restored " + loadedCount + " crop instances from currently loaded chunks.");
    }

    private void setupMenuServices() {
        this.recipeStorage = new dev.darkblade.croppy.platform.paper.storage.PdcRecipeKnowledgeStorage(this);
        this.feedbackHelper = new dev.darkblade.croppy.platform.paper.discovery.DiscoveryFeedbackHelper(
                this.messageService);
        this.experimentationHandler = new dev.darkblade.croppy.platform.paper.discovery.StationExperimentationHandler(
                core.getRecipeRegistry(),
                core.getRecipeDiscoveryEngine(),
                this.recipeStorage,
                this.feedbackHelper);
        this.fuelService = new dev.darkblade.croppy.platform.paper.fuel.PaperFuelService(
                new dev.darkblade.croppy.core.fuel.DefaultFuelRegistry());
        this.chatPromptService = new dev.darkblade.croppy.platform.paper.prompt.ChatPromptService(this,
                this.messageService);
        this.menuRenderer = new dev.darkblade.croppy.platform.paper.menu.PaperMenuRenderer(this.itemFactory);
        this.menuTicker = new dev.darkblade.croppy.platform.paper.menu.PaperMenuTicker(this.menuRenderer,
                core.getProcessRegistry());
        this.menuService = new dev.darkblade.croppy.platform.paper.menu.PaperMenuService(core.getMenuRegistry(),
                this.menuRenderer, core.getProcessRegistry(), this.menuTicker, getLogger());
        this.menuListener = new dev.darkblade.croppy.platform.paper.menu.PaperMenuListener(this,
                core.getProcessEngine(), this.itemFactory, this.menuRenderer, this.chatPromptService, this.menuService);
        this.menuListener.setExperimentationHandler(this.experimentationHandler);
        this.compendiumController = new dev.darkblade.croppy.platform.paper.menu.CompendiumMenuController(
                core.getMenuRegistry(),
                core.getRecipeRegistry(),
                core.getRecipeDiscoveryEngine(),
                this.menuRenderer,
                this.itemFactory,
                this.messageService,
                core.getRecipeCategoryRegistry(),
                core.getProcessRegistry(),
                core.getStationRegistry(),
                core.getStationManager(),
                this.pluginConfig.isCompendiumShowUnknownRecipes(),
                this.pluginConfig.getCompendiumStationProximityDistance());
        this.recipeDetailController = new dev.darkblade.croppy.platform.paper.menu.RecipeDetailMenuController(
                core.getMenuRegistry(),
                core.getProcessRegistry(),
                core.getStationRegistry(),
                this.itemFactory,
                this.menuRenderer,
                this.messageService,
                this.compendiumController,
                this.pluginConfig.getCompendiumStationProximityDistance());
        this.menuListener.setRecipeRegistry(core.getRecipeRegistry());
        this.menuListener.setDiscoveryEngine(core.getRecipeDiscoveryEngine());
        this.menuListener.setRecipeDetailController(this.recipeDetailController);
        this.menuListener.setCompendiumController(this.compendiumController);
        this.itemCatalogController = new dev.darkblade.croppy.platform.paper.menu.ItemCatalogController(
                core.getMenuRegistry(),
                core.getItemRegistry(),
                core.getItemCategoryRegistry(),
                this.itemFactory,
                this.menuRenderer,
                this.messageService,
                this.chatPromptService);
        this.menuListener.setItemCatalogController(this.itemCatalogController);
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ChunkLifecycleListener(core.getCropManager(), storage, core.getSpeciesRegistry(),
                this.displaySynchronizer, this.virtualBlockDisplayTracker, core.getStationManager(),
                this.stationStorage, this.stationViewerTracker, getLogger()), this);
        pm.registerEvents(new CropPlantListener(core, this.fertilizerService, this.displaySynchronizer,
                getProtectionPort(), this.messageService), this);
        pm.registerEvents(new CropGrowthListener(core), this);
        pm.registerEvents(new CropBreakListener(core, this.displaySynchronizer, getProtectionPort(), this.itemFactory),
                this);
        pm.registerEvents(new CropPhysicsListener(core, this.virtualBlockDisplayTracker), this);
        pm.registerEvents(new InspectionToolListener(this, this.inspectorService, this.pluginConfig), this);
        pm.registerEvents(
                new WateringListener(this.wateringCanService, core.getCropManager(), this.messageService,
                        getProtectionPort()),
                this);
        pm.registerEvents(new TimeSkipListener(this.dailyTransitionService), this);
        pm.registerEvents(
                new FertilizerListener(this.fertilizerService, core.getCropManager(), this.messageService,
                        getProtectionPort()),
                this);

        // Station & Process listeners
        this.stationInteractListener = new dev.darkblade.croppy.platform.paper.station.listener.StationInteractListener(
                core.getStationManager(), core.getProcessEngine(), this.itemFactory, this.menuService,
                core.getInteractionRouter(), core.getRuleEngine(), this.messageService, this);
        this.stationBlockListener = new dev.darkblade.croppy.platform.paper.station.listener.StationBlockListener(
                core.getStationManager(), core.getStationRegistry(), this.stationStorage, this.itemFactory,
                this.stationViewerTracker, getProtectionPort());

        pm.registerEvents(this.chatPromptService, this);
        pm.registerEvents(this.menuListener, this);
        pm.registerEvents(this.stationInteractListener, this);
        pm.registerEvents(this.stationBlockListener, this);
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.station.listener.StationPlayerConnectionListener(
                this.stationViewerTracker, this.virtualBlockDisplayTracker), this);

        this.virtualInteractionListener = new dev.darkblade.croppy.platform.paper.display.VirtualInteractionPacketListener(
                this, core, this.virtualBlockDisplayTracker, getLogger(), this.displaySynchronizer, getProtectionPort(),
                this.itemFactory);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .registerListener(this.virtualInteractionListener);

        this.stationVirtualInteractionListener = new dev.darkblade.croppy.platform.paper.station.listener.StationVirtualInteractionPacketListener(
                this, this.stationViewerTracker, this.stationBlockListener, this.stationInteractListener, getLogger(),
                getProtectionPort());
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .registerListener(this.stationVirtualInteractionListener);
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.listener.BukkitRecipeDiscoveryListener(
                core.getRecipeDiscoveryEngine(),
                core.getRecipeRegistry(),
                this.recipeStorage,
                this.feedbackHelper,
                getLogger()), this);
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.listener.RecipeBookItemListener(
                this.itemFactory,
                this.compendiumController), this);
        if (this.updateService != null) {
            pm.registerEvents(new dev.darkblade.croppy.platform.paper.update.listener.UpdateNotificationListener(
                    this.updateService, this.messageService), this);
        }
        if (this.craftingRecipeService != null) {
            pm.registerEvents(new dev.darkblade.croppy.platform.paper.crafting.CraftingRecipeDiscoveryListener(
                    this.craftingRecipeService, this.pluginConfig), this);
        }

        getLogger().info(
                "Registered crop lifecycle, chunk, inspection, watering, fertilizer, and crafting station listeners.");
    }

    private void setupMenuDefinitions() {
        Path menusDir = getDataFolder().toPath().resolve("menus");
        if (!Files.exists(menusDir)) {
            try {
                Files.createDirectories(menusDir);
            } catch (Exception ignored) {
            }
        }
        saveResourceIfNotExists("menus/default_station.yml");
        saveResourceIfNotExists("menus/recipes_menu.yml");
        saveResourceIfNotExists("menus/items_menu.yml");
        saveResourceIfNotExists("menus/recipe_detail_menu.yml");
        saveResourceIfNotExists("menus/stations/cooking_pot.yml");
        saveResourceIfNotExists("menus/stations/skillet.yml");
        saveResourceIfNotExists("menus/stations/stove_menu.yml");

        dev.darkblade.croppy.core.menu.YamlMenuLoader menuLoader = new dev.darkblade.croppy.core.menu.YamlMenuLoader();
        int loaded = menuLoader.loadAll(menusDir, core.getMenuRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " menu definitions into MenuRegistry.");
    }

    private void setupProcessDefinitions() {
        Path processesDir = getDataFolder().toPath().resolve("processes");
        if (!Files.exists(processesDir)) {
            try {
                Files.createDirectories(processesDir);
            } catch (Exception ignored) {
            }
        }
        saveResourceIfNotExists("processes/tomato_soup_cook.yml");
        saveResourceIfNotExists("processes/vegetable_stew_cook.yml");
        saveResourceIfNotExists("processes/fried_egg_cook.yml");

        dev.darkblade.croppy.core.process.YamlProcessLoader processLoader = new dev.darkblade.croppy.core.process.YamlProcessLoader();
        int loaded = processLoader.loadAll(processesDir, core.getProcessRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " process definitions into ProcessRegistry.");
    }

    private void setupRecipeDefinitions() {
        Path recipesDir = getDataFolder().toPath().resolve("recipes");
        if (!Files.exists(recipesDir)) {
            try {
                Files.createDirectories(recipesDir);
            } catch (Exception ignored) {
            }
        }
        saveResourceIfNotExists("recipes/tomato_soup.yml");
        saveResourceIfNotExists("recipes/vegetable_stew.yml");
        saveResourceIfNotExists("recipes/fried_egg.yml");

        dev.darkblade.croppy.core.recipe.YamlRecipeLoader recipeLoader = new dev.darkblade.croppy.core.recipe.YamlRecipeLoader();
        int loaded = recipeLoader.loadAll(recipesDir, core.getRecipeRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " recipe definitions into RecipeRegistry.");
    }

    private void setupCraftingRecipeDefinitions() {
        Path craftingDir = getDataFolder().toPath().resolve("crafting");
        if (!Files.exists(craftingDir)) {
            try {
                Files.createDirectories(craftingDir);
            } catch (Exception ignored) {
            }
        }
        // Stations
        saveResourceIfNotExists("crafting/stations/cooking_pot.yml");
        saveResourceIfNotExists("crafting/stations/stove.yml");
        saveResourceIfNotExists("crafting/stations/skillet.yml");
        // Tools
        saveResourceIfNotExists("crafting/tools/inspector.yml");
        saveResourceIfNotExists("crafting/tools/thermometer.yml");
        // Watering Cans
        saveResourceIfNotExists("crafting/watering_cans/wooden_can.yml");
        saveResourceIfNotExists("crafting/watering_cans/copper_can.yml");
        saveResourceIfNotExists("crafting/watering_cans/iron_can.yml");
        saveResourceIfNotExists("crafting/watering_cans/golden_can.yml");
        saveResourceIfNotExists("crafting/watering_cans/diamond_can.yml");
        saveResourceIfNotExists("crafting/watering_cans/netherite_can.yml");
        // Fertilizers
        saveResourceIfNotExists("crafting/fertilizers/growth_fertilizer.yml");
        saveResourceIfNotExists("crafting/fertilizers/hydrating_fertilizer.yml");
        saveResourceIfNotExists("crafting/fertilizers/quality_fertilizer.yml");

        dev.darkblade.croppy.core.crafting.YamlCraftingRecipeLoader loader = new dev.darkblade.croppy.core.crafting.YamlCraftingRecipeLoader();
        int loaded = loader.loadAll(craftingDir, core.getCraftingRecipeRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " crafting recipe definitions into CraftingRecipeRegistry.");

        dev.darkblade.croppy.platform.paper.crafting.CraftingRecipeChoiceResolver choiceResolver =
                new dev.darkblade.croppy.platform.paper.crafting.CraftingRecipeChoiceResolver(this.itemFactory);
        this.craftingRecipeService = new dev.darkblade.croppy.platform.paper.crafting.PaperCraftingRecipeService(
                getServer(),
                core.getCraftingRecipeRegistry(),
                this.itemFactory,
                choiceResolver,
                getLogger()
        );
        this.craftingRecipeService.registerAll();
    }

    private void setupStationDefinitions() {
        Path stationsDir = getDataFolder().toPath().resolve("stations");
        if (!Files.exists(stationsDir)) {
            try {
                Files.createDirectories(stationsDir);
            } catch (Exception ignored) {
            }
            saveResourceIfNotExists("stations/cooking_pot.yml");
            saveResourceIfNotExists("stations/skillet.yml");
            saveResourceIfNotExists("stations/stove.yml");
        }

        dev.darkblade.croppy.core.station.YamlStationLoader stationLoader = new dev.darkblade.croppy.core.station.YamlStationLoader();
        int loaded = stationLoader.loadAll(stationsDir, core.getStationRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " station definitions into StationRegistry.");
    }

    private void setupInteractionDefinitions() {
        Path dir = getDataFolder().toPath().resolve("interactions");
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (Exception ignored) {
            }
            saveResourceIfNotExists("interactions/cooking_pot_interactions.yml");
            saveResourceIfNotExists("interactions/skillet_interactions.yml");
            saveResourceIfNotExists("interactions/thermometer_inspect.yml");
            saveResourceIfNotExists("interactions/crops/rice.yml");
        }

        dev.darkblade.croppy.core.interaction.YamlInteractionLoader loader = new dev.darkblade.croppy.core.interaction.YamlInteractionLoader();
        int loaded = loader.loadAll(dir, core.getInteractionRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " interaction definitions into InteractionRegistry.");
    }

    private void setupRuleDefinitions() {
        Path dir = getDataFolder().toPath().resolve("rules");
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (Exception ignored) {
            }
            saveResourceIfNotExists("rules/overcooking_burn.yml");
            saveResourceIfNotExists("rules/thermal_burn.yml");
        }

        dev.darkblade.croppy.core.rule.YamlRuleLoader loader = new dev.darkblade.croppy.core.rule.YamlRuleLoader();
        int loaded = loader.loadAll(dir, core.getRuleRegistry(), getLogger());
        getLogger().info("Registered " + loaded + " rule definitions into RuleRegistry.");
    }

    private void loadActiveChunkStations() {
        if (this.stationStorage == null)
            return;
        int loadedCount = 0;
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Collection<dev.darkblade.croppy.api.station.StationInstance> stations = stationStorage
                        .loadChunk(world.getName(), chunk.getX(), chunk.getZ());
                for (dev.darkblade.croppy.api.station.StationInstance station : stations) {
                    if (core.getStationManager().getStation(station.getPosition()).isEmpty()) {
                        core.getStationManager().addStation(station);
                        loadedCount++;
                    }
                }
            }
        }
        getLogger().info("Restored " + loadedCount + " station instances from currently loaded chunks.");
    }

    private void setupItemDefinitions() {
        Path itemsDir = getDataFolder().toPath().resolve("items");
        // Seeds
        saveResourceIfNotExists("items/seeds/wheat_seeds.yml");
        saveResourceIfNotExists("items/seeds/carrot_seeds.yml");
        saveResourceIfNotExists("items/seeds/potato_seeds.yml");
        saveResourceIfNotExists("items/seeds/tomato_seeds.yml");
        saveResourceIfNotExists("items/seeds/rice_seeds.yml");
        // Tools
        saveResourceIfNotExists("items/tools/inspector.yml");
        saveResourceIfNotExists("items/tools/thermometer.yml");
        // Misc
        saveResourceIfNotExists("items/misc/burnt_food.yml");
        saveResourceIfNotExists("items/misc/recipe_book.yml");
        // Ingredients
        saveResourceIfNotExists("items/ingredients/tomato.yml");
        saveResourceIfNotExists("items/ingredients/tomato_soup.yml");
        saveResourceIfNotExists("items/ingredients/vegetable_stew.yml");
        saveResourceIfNotExists("items/ingredients/fried_egg.yml");
        saveResourceIfNotExists("items/ingredients/rice_panicle.yml");
        // Watering Cans
        saveResourceIfNotExists("items/watering_cans/wooden_can.yml");
        saveResourceIfNotExists("items/watering_cans/copper_can.yml");
        saveResourceIfNotExists("items/watering_cans/iron_can.yml");
        saveResourceIfNotExists("items/watering_cans/golden_can.yml");
        saveResourceIfNotExists("items/watering_cans/diamond_can.yml");
        saveResourceIfNotExists("items/watering_cans/netherite_can.yml");
        // Fertilizers
        saveResourceIfNotExists("items/fertilizers/growth_fertilizer.yml");
        saveResourceIfNotExists("items/fertilizers/hydrating_fertilizer.yml");
        saveResourceIfNotExists("items/fertilizers/quality_fertilizer.yml");
        // Stations
        saveResourceIfNotExists("items/stations/cooking_pot.yml");
        saveResourceIfNotExists("items/stations/skillet.yml");
        saveResourceIfNotExists("items/stations/stove.yml");

        YamlItemLoader itemLoader = new YamlItemLoader();
        int loaded = itemLoader.loadAllCategories(itemsDir, getLogger(), core.getItemRegistry());
        this.itemFactory = new ItemFactory(this, core.getItemRegistry());
        getLogger().info("Registered " + loaded + " custom items across "
                + core.getItemRegistry().getCategories().size() + " categories.");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        Path target = getDataFolder().toPath().resolve(resourcePath);
        if (!Files.exists(target)) {
            try {
                if (target.getParent() != null && !Files.exists(target.getParent())) {
                    Files.createDirectories(target.getParent());
                }
                saveResource(resourcePath, false);
            } catch (Exception e) {
                getLogger().warning("Could not extract default resource '" + resourcePath + "': " + e.getMessage());
            }
        }
    }

    private void setupFertilizers() {
        this.fertilizerRegistry = new DefaultFertilizerRegistry();
        this.fertilizerService = new FertilizerService(this, this.fertilizerRegistry, this.itemFactory);
        this.wateringCanService = new WateringCanService(this, this.itemFactory, this.messageService);
        this.dailyTransitionService = new DailyTransitionService(core.getCropManager(), this.fertilizerRegistry);

        Path fertDir = getDataFolder().toPath().resolve("items").resolve("fertilizers");
        if (!Files.exists(fertDir)) {
            saveResourceIfNotExists("items/fertilizers/growth_fertilizer.yml");
            saveResourceIfNotExists("items/fertilizers/hydrating_fertilizer.yml");
            saveResourceIfNotExists("items/fertilizers/quality_fertilizer.yml");
        }

        YamlFertilizerLoader fertLoader = new YamlFertilizerLoader();
        int loaded = fertLoader.loadDirectory(fertDir, getLogger(), this.fertilizerRegistry);
        getLogger().info("Registered " + loaded + " fertilizer definitions into FertilizerRegistry.");
    }

    private void setupGrowthSimulation() {
        long tickInterval = this.pluginConfig.getGrowthTickInterval();
        double baseProgress = this.pluginConfig.getGrowthBaseProgress();
        int waterSearchRadius = this.pluginConfig.getGrowthWaterSearchRadius();

        GrowthEngine growthEngine = core.getGrowthEngine();
        growthEngine.setBaseProgress(baseProgress);
        growthEngine.addModifier(new FertilizerGrowthModifier(this.fertilizerRegistry));

        this.environmentProvider = new PaperEnvironmentProvider(getServer(), waterSearchRadius);
        this.packetBlockRenderer = new dev.darkblade.croppy.platform.paper.display.PacketEventsBlockRenderer(
                getLogger());
        this.virtualBlockDisplayTracker = new dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker(
                getServer(), this.packetBlockRenderer, this.itemFactory, getLogger());
        this.displaySynchronizer = new PaperDisplaySynchronizer(getServer(), getLogger(),
                this.virtualBlockDisplayTracker);

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

        this.growthTicker = new PaperGrowthTicker(this, core, this.environmentProvider, this.dailyTransitionService,
                this.virtualBlockDisplayTracker);
        this.growthTicker.start(tickInterval);

        // Schedule periodic rain hydration task (every 5 seconds / 100 ticks)
        this.rainTask = getServer().getScheduler().runTaskTimer(this,
                new RainHydrationTask(getServer(), core.getCropManager()), 100L, 100L);

        // Schedule periodic 3D display viewer proximity tracking (every 1 second / 20
        // ticks)
        this.displayTrackerTask = getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                if (this.virtualBlockDisplayTracker != null) {
                    this.virtualBlockDisplayTracker.tickViewers();
                }
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error during virtual display viewer tracking tick", t);
            }
        }, 20L, 20L);

        this.packetStationRenderer = new dev.darkblade.croppy.platform.paper.station.display.PacketEventsStationRenderer(
                getLogger());
        this.stationViewerTracker = new dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker(
                getServer(), this.packetStationRenderer, this.itemFactory, getLogger(), core.getProcessRegistry());

        this.stationTicker = new dev.darkblade.croppy.platform.paper.station.PaperStationTicker(
                this,
                core.getStationManager(),
                core.getProcessEngine(),
                new dev.darkblade.croppy.platform.paper.station.PaperThermalEvaluator(core.getStationManager()),
                core.getRuleEngine(),
                this.displaySynchronizer,
                this.itemFactory,
                this.stationViewerTracker,
                this.fuelService,
                core.getProcessRegistry(),
                20L);
        this.stationTicker.start();

        this.menuTickerTask = getServer().getScheduler().runTaskTimer(this,
                this.menuTicker, 20L, 10L);

        getLogger().info("Started PaperGrowthTicker with interval of " + tickInterval + " ticks (base progress: "
                + baseProgress + ").");
        getLogger().info("Started PaperStationTicker with interval of 20 ticks.");
        getLogger().info("Started PaperMenuTicker with interval of 10 ticks.");
    }

    private void setupCommands() {
        this.inspectorService = new CropInspectorService(
                core.getCropManager(),
                core.getSpeciesRegistry(),
                this.environmentProvider,
                this.fertilizerRegistry,
                getServer(),
                this.messageService);

        PaperCommandManager<Source> commandManager = PaperCommandManager
                .builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(this);

        commandManager.exceptionController().registerHandler(NoPermissionException.class, ctx -> {
            CommandSender sender = ctx.context().sender().source();
            this.messageService.sendPrefixedMessage(sender, "general.no-permission");
        });

        commandManager.exceptionController().registerHandler(InvalidSyntaxException.class, ctx -> {
            CommandSender sender = ctx.context().sender().source();
            String syntax = ctx.exception().correctSyntax();
            String formattedUsage = syntax.startsWith("/") ? syntax : "/" + syntax;
            this.messageService.sendPrefixedMessage(sender, "command.invalid-syntax", "usage", formattedUsage);
        });

        commandManager.exceptionController().registerHandler(InvalidCommandSenderException.class, ctx -> {
            CommandSender sender = ctx.context().sender().source();
            this.messageService.sendPrefixedMessage(sender, "command.player-only");
        });

        commandManager.exceptionController().registerHandler(ArgumentParseException.class, ctx -> {
            CommandSender sender = ctx.context().sender().source();
            Throwable cause = ctx.exception().getCause();
            String message = cause != null && cause.getMessage() != null ? cause.getMessage()
                    : ctx.exception().getMessage();
            if (message == null || message.isBlank()) {
                message = "Invalid argument.";
            }
            this.messageService.sendPrefixedMessage(sender, "command.invalid-syntax", "usage", message);
        });

        if (commandManager.hasBrigadierManager()) {
            commandManager.brigadierManager().registerMapping(
                    new TypeToken<SpeciesParser<Source>>() {
                    },
                    builder -> builder.toConstant(ArgumentTypes.key()).cloudSuggestions());
            commandManager.brigadierManager().registerMapping(
                    new TypeToken<KeyParser<Source>>() {
                    },
                    builder -> builder.toConstant(ArgumentTypes.key()).cloudSuggestions());
        }

        commandManager.parserRegistry().registerParser(SpeciesParser.speciesParser(core.getSpeciesRegistry()));
        commandManager.parserRegistry().registerParser(KeyParser.keyParser());

        AnnotationParser<Source> annotationParser = new AnnotationParser<>(
                commandManager,
                Source.class);
        CroppyCommands commands = new CroppyCommands(
                this,
                this.inspectorService,
                this.pluginConfig,
                this.wateringCanService,
                this.fertilizerService,
                this.fertilizerRegistry,
                this.itemFactory,
                this.core.getItemRegistry());
        commands.setRecipeServices(
                core.getRecipeRegistry(),
                core.getRecipeDiscoveryEngine(),
                this.recipeStorage,
                this.compendiumController,
                core.getRecipeCategoryRegistry());
        commands.setChatPromptService(this.chatPromptService);
        commands.setItemCatalogController(this.itemCatalogController);
        commands.setItemCategoryRegistry(this.core.getItemCategoryRegistry());
        commands.setAddonManager(this.addonManager);
        commands.setResourcePackService(this.resourcePackService);
        commands.setUpdateService(this.updateService);
        annotationParser.parse(commands);
        getLogger().info("Registered Cloud 2.0 command manager and /croppy commands.");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.pluginConfig.load(getConfig());
        setupLocalization();
        this.core.getSpeciesRegistry().clear();
        this.core.getItemRegistry().clear();
        this.core.getItemCategoryRegistry().clear();
        this.core.getItemCategoryRegistry().registerDefaults();
        this.core.getMenuRegistry().clear();
        this.core.getStationRegistry().clear();
        this.core.getInteractionRegistry().clear();
        this.core.getRuleRegistry().clear();
        this.core.getRecipeRegistry().clear();
        this.core.getProcessRegistry().clear();
        if (this.craftingRecipeService != null) {
            this.craftingRecipeService.unregisterAll();
        }
        this.core.getCraftingRecipeRegistry().clear();
        this.fertilizerRegistry.clear();
        setupItemDefinitions();
        setupFertilizers();
        setupCropDefinitions();
        setupProcessDefinitions();
        setupRecipeDefinitions();
        setupCraftingRecipeDefinitions();
        setupMenuDefinitions();
        setupStationDefinitions();
        setupInteractionDefinitions();
        setupRuleDefinitions();
        if (this.addonManager != null) {
            this.addonManager.disableAll();
            this.addonManager.enableAll();
        }
        getLogger().info(
                "Croppy configuration, items, fertilizers, menus, stations, interactions, rules, and crop definitions reloaded successfully.");
    }

    private void setupCropDefinitions() {
        Path cropsDir = getDataFolder().toPath().resolve("crops");
        saveResourceIfNotExists("crops/wheat.yml");
        saveResourceIfNotExists("crops/carrot.yml");
        saveResourceIfNotExists("crops/potato.yml");
        saveResourceIfNotExists("crops/tomato.yml");
        saveResourceIfNotExists("crops/rice.yml");

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        List<Species> loaded = loader.loadDirectory(cropsDir, getLogger());
        for (Species species : loaded) {
            core.getSpeciesRegistry().register(species);
        }
        getLogger().info("Registered " + core.getSpeciesRegistry().size() + " crop species into SpeciesRegistry.");
    }

    private void preloadShutdownClasses() {
        try {
            Class.forName(ChunkPosition.class.getName());
        } catch (ClassNotFoundException e) {
            getLogger().log(Level.WARNING, "Failed to preload ChunkPosition class for shutdown safety", e);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Croppy plugin...");
        if (this.addonManager != null) {
            try {
                this.addonManager.disableAll();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error disabling addons during shutdown", t);
            }
            this.addonManager = null;
        }

        if (this.resourcePackService != null) {
            try {
                this.resourcePackService.shutdown();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error stopping resource pack service during shutdown", t);
            }
            this.resourcePackService = null;
        }

        if (this.growthTicker != null) {
            try {
                this.growthTicker.stop();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error stopping growth ticker during shutdown", t);
            }
            this.growthTicker = null;
        }

        if (this.rainTask != null && !this.rainTask.isCancelled()) {
            try {
                this.rainTask.cancel();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error cancelling rain task during shutdown", t);
            }
            this.rainTask = null;
        }

        if (this.stationTicker != null) {
            try {
                this.stationTicker.stop();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error stopping station ticker during shutdown", t);
            }
            this.stationTicker = null;
        }

        if (this.menuTickerTask != null && !this.menuTickerTask.isCancelled()) {
            try {
                this.menuTickerTask.cancel();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error cancelling menu ticker task during shutdown", t);
            }
            this.menuTickerTask = null;
        }

        if (this.displayTrackerTask != null && !this.displayTrackerTask.isCancelled()) {
            try {
                this.displayTrackerTask.cancel();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error cancelling display tracker task during shutdown", t);
            }
            this.displayTrackerTask = null;
        }

        flushActiveCrops();
        flushActiveStations();

        if (this.virtualBlockDisplayTracker != null) {
            try {
                this.virtualBlockDisplayTracker.clearAll();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error clearing virtual block displays", t);
            }
        }
        if (this.virtualInteractionListener != null) {
            try {
                com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                        .unregisterListener(this.virtualInteractionListener);
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error unregistering virtual interaction listener", t);
            }
        }
        if (this.stationVirtualInteractionListener != null) {
            try {
                com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                        .unregisterListener(this.stationVirtualInteractionListener);
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error unregistering station virtual interaction listener", t);
            }
        }

        if (this.craftingRecipeService != null) {
            try {
                this.craftingRecipeService.unregisterAll();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error unregistering crafting recipes during shutdown", t);
            }
            this.craftingRecipeService = null;
        }

        if (this.storage != null) {
            try {
                this.storage.shutdown();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error shutting down storage backend", t);
            }
            this.storage = null;
        }
        if (this.core != null) {
            try {
                this.core.shutdown();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error shutting down core", t);
            }
        }
        if (this.integrationManager != null) {
            try {
                this.integrationManager.onDisable();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error disabling integrations", t);
            }
        }
        getLogger().info("Croppy disabled cleanly.");
    }

    private void flushActiveCrops() {
        if (this.storage == null || this.core == null) {
            return;
        }

        try {
            Map<ChunkPosition, List<CropInstance>> chunkMap = new HashMap<>();

            for (CropInstance crop : core.getCropManager().getAllCrops()) {
                ChunkPosition coord = crop.getPosition().toChunkPosition();
                chunkMap.computeIfAbsent(coord, k -> new ArrayList<>()).add(crop);
            }

            for (Map.Entry<ChunkPosition, List<CropInstance>> entry : chunkMap.entrySet()) {
                storage.saveChunk(entry.getKey().worldName(), entry.getKey().x(), entry.getKey().z(), entry.getValue());
            }
            getLogger().info("Flushed " + core.getCropManager().count() + " active crop instances across "
                    + chunkMap.size() + " chunks.");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to flush active crops during plugin shutdown", t);
        }
    }

    private void flushActiveStations() {
        if (this.stationStorage == null || this.core == null) {
            return;
        }

        try {
            Map<ChunkPosition, List<dev.darkblade.croppy.api.station.StationInstance>> chunkMap = new HashMap<>();

            for (dev.darkblade.croppy.api.station.StationInstance station : core.getStationManager().getAllStations()) {
                ChunkPosition coord = station.getPosition().toChunkPosition();
                chunkMap.computeIfAbsent(coord, k -> new ArrayList<>()).add(station);
            }

            for (Map.Entry<ChunkPosition, List<dev.darkblade.croppy.api.station.StationInstance>> entry : chunkMap
                    .entrySet()) {
                stationStorage.saveChunk(entry.getKey().worldName(), entry.getKey().x(), entry.getKey().z(),
                        entry.getValue());
            }
            getLogger().info("Flushed " + core.getStationManager().getStationCount() + " active stations across "
                    + chunkMap.size() + " chunks.");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to flush active stations during plugin shutdown", t);
        }
    }

    public CroppyCore getCore() {
        return core;
    }

    public dev.darkblade.croppy.platform.paper.menu.PaperMenuService getMenuService() {
        return menuService;
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

    public dev.darkblade.croppy.platform.paper.menu.ItemCatalogController getItemCatalogController() {
        return itemCatalogController;
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

    private void setupLocalization() {
        Path localesDir = getDataFolder().toPath().resolve("locales");
        if (!Files.exists(localesDir)) {
            try {
                Files.createDirectories(localesDir);
            } catch (Exception ignored) {
            }
        }
        saveResourceIfNotExists("locales/en-US.yml");
        saveResourceIfNotExists("locales/es-ES.yml");

        String defaultLocaleStr = this.pluginConfig.getDefaultLocale();
        dev.darkblade.croppy.api.i18n.LocaleRef defaultLocale = dev.darkblade.croppy.api.i18n.LocaleRef
                .parse(defaultLocaleStr);
        dev.darkblade.croppy.core.i18n.DefaultMessageCatalog catalog = new dev.darkblade.croppy.core.i18n.DefaultMessageCatalog(
                defaultLocale, dev.darkblade.croppy.api.i18n.LocaleRef.EN_US);

        dev.darkblade.croppy.core.i18n.YamlLocaleLoader loader = new dev.darkblade.croppy.core.i18n.YamlLocaleLoader();
        Map<dev.darkblade.croppy.api.i18n.LocaleRef, Map<String, String>> loaded = loader.loadDirectory(localesDir,
                getLogger());
        catalog.setCatalogs(loaded);

        this.messageService = new dev.darkblade.croppy.platform.paper.i18n.PaperMessageService(catalog, getLogger());
        getLogger().info("Initialized localization service with " + loaded.size() + " locales (default: "
                + defaultLocale + ").");
    }

    public dev.darkblade.croppy.platform.paper.i18n.PaperMessageService getMessageService() {
        return messageService;
    }

    public dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker getStationViewerTracker() {
        return stationViewerTracker;
    }

    public dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker getVirtualBlockDisplayTracker() {
        return virtualBlockDisplayTracker;
    }

    private void setupIntegrations() {
        if (this.integrationManager == null) {
            this.integrationManager = new dev.darkblade.croppy.integration.common.IntegrationManager(getLogger());
            dev.darkblade.croppy.integration.worldguard.WorldGuardIntegration wg = new dev.darkblade.croppy.integration.worldguard.WorldGuardIntegration(
                    getLogger(),
                    this.pluginConfig.isWorldGuardEnabled(),
                    this.pluginConfig.isWorldGuardUseCustomFlags());
            this.integrationManager.register(wg);
        }

        // Register PAPI integration
        dev.darkblade.croppy.integration.papi.PapiIntegration papi = new dev.darkblade.croppy.integration.papi.PapiIntegration(
                getLogger(),
                this.core,
                getDescription().getVersion(),
                this.pluginConfig.isPapiEnabled(),
                this.pluginConfig.getPapiRaycastDistance());
        this.integrationManager.register(papi);

        // Register Vault integration
        dev.darkblade.croppy.integration.vault.VaultIntegration vault = new dev.darkblade.croppy.integration.vault.VaultIntegration(
                getLogger(),
                this.pluginConfig.isVaultEnabled());
        this.integrationManager.register(vault);

        // Enable all integrations
        this.integrationManager.onEnable();

        // Resolve active ProtectionPort
        var wgOpt = this.integrationManager.getIntegration("worldguard");
        if (wgOpt.isPresent() && wgOpt
                .get() instanceof dev.darkblade.croppy.integration.worldguard.WorldGuardIntegration wgIntegration
                && wgIntegration.getProtectionAdapter() != null) {
            this.protectionPort = wgIntegration.getProtectionAdapter();
        } else {
            this.protectionPort = dev.darkblade.croppy.integration.common.NoOpProtectionAdapter.INSTANCE;
        }

        // Resolve active EconomyPort
        var vaultOpt = this.integrationManager.getIntegration("vault");
        if (vaultOpt.isPresent()
                && vaultOpt.get() instanceof dev.darkblade.croppy.integration.vault.VaultIntegration vaultIntegration
                && vaultIntegration.getEconomyAdapter() != null) {
            this.economyPort = vaultIntegration.getEconomyAdapter();
        } else {
            this.economyPort = dev.darkblade.croppy.integration.common.NoOpEconomyAdapter.INSTANCE;
        }
    }

    public dev.darkblade.croppy.api.protection.ProtectionPort getProtectionPort() {
        return protectionPort != null ? protectionPort
                : dev.darkblade.croppy.integration.common.NoOpProtectionAdapter.INSTANCE;
    }

    public dev.darkblade.croppy.api.economy.EconomyPort getEconomyPort() {
        return economyPort != null ? economyPort : dev.darkblade.croppy.integration.common.NoOpEconomyAdapter.INSTANCE;
    }

    public dev.darkblade.croppy.integration.common.IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
}
