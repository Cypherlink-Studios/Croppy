package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.core.crafting.DefaultCraftingRecipeRegistry;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.discovery.DefaultRecipeDiscoveryEngine;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.interaction.DefaultInteractionRegistry;
import dev.darkblade.croppy.core.interaction.InteractionRouter;
import dev.darkblade.croppy.core.item.DefaultItemCategoryRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.menu.DefaultMenuRegistry;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import dev.darkblade.croppy.core.rule.DefaultRuleRegistry;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.DefaultStationRegistry;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Central orchestrator managing Croppy's core domain services and lifecycle.
 * Completely decoupled from Minecraft/Paper platform classes.
 */
public class CroppyCore {

    private final CroppyLogger logger;
    private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();
    private final DefaultSpeciesRegistry speciesRegistry = new DefaultSpeciesRegistry();
    private final DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
    private final DefaultItemCategoryRegistry itemCategoryRegistry = new DefaultItemCategoryRegistry();
    private final DefaultStationRegistry stationRegistry = new DefaultStationRegistry();
    private final DefaultProcessRegistry processRegistry = new DefaultProcessRegistry();
    private final DefaultInteractionRegistry interactionRegistry = new DefaultInteractionRegistry();
    private final DefaultRuleRegistry ruleRegistry = new DefaultRuleRegistry();
    private final DefaultMenuRegistry menuRegistry = new DefaultMenuRegistry();
    private final DefaultRecipeRegistry recipeRegistry = new DefaultRecipeRegistry();
    private final DefaultRecipeCategoryRegistry recipeCategoryRegistry = new DefaultRecipeCategoryRegistry();
    private final DefaultCraftingRecipeRegistry craftingRecipeRegistry = new DefaultCraftingRecipeRegistry();
    private final DefaultRecipeDiscoveryEngine recipeDiscoveryEngine = new DefaultRecipeDiscoveryEngine();
    private final CropManager cropManager = new CropManager();
    private final DefaultStationManager stationManager = new DefaultStationManager();
    private final GrowthEngine growthEngine = new GrowthEngine();
    private final ProcessEngine processEngine = new ProcessEngine(processRegistry, recipeRegistry);
    private final InteractionRouter interactionRouter = new InteractionRouter(interactionRegistry);
    private final RuleEngine ruleEngine = new RuleEngine(ruleRegistry);
    private CropStorage storage;
    private final AtomicBoolean active = new AtomicBoolean(false);

    public CroppyCore(CroppyLogger logger) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
    }

    public CroppyCore(Logger logger) {
        this(new JulCroppyLogger(Objects.requireNonNull(logger, "Logger cannot be null")));
    }

    public CroppyCore() {
        this(NoOpCroppyLogger.INSTANCE);
    }

    /**
     * Initializes the core services and domain state.
     *
     * @throws IllegalStateException if the core is already active
     */
    public synchronized void initialize() {
        if (active.get()) {
            throw new IllegalStateException("CroppyCore is already initialized");
        }
        logger.debug(LogChannel.CORE, "Initializing Croppy core domain services...");
        speciesRegistry.clear();
        itemRegistry.clear();
        itemCategoryRegistry.clear();
        itemCategoryRegistry.registerDefaults();
        stationRegistry.clear();
        processRegistry.clear();
        interactionRegistry.clear();
        ruleRegistry.clear();
        menuRegistry.clear();
        recipeRegistry.clear();
        recipeCategoryRegistry.clear();
        recipeCategoryRegistry.registerDefaults();
        craftingRecipeRegistry.clear();
        cropManager.clear();
        stationManager.clear();
        registerService(SpeciesRegistry.class, speciesRegistry);
        registerService(ItemRegistry.class, itemRegistry);
        registerService(ItemCategoryRegistry.class, itemCategoryRegistry);
        registerService(StationRegistry.class, stationRegistry);
        registerService(ProcessRegistry.class, processRegistry);
        registerService(RecipeRegistry.class, recipeRegistry);
        registerService(RecipeCategoryRegistry.class, recipeCategoryRegistry);
        registerService(CraftingRecipeRegistry.class, craftingRecipeRegistry);
        registerService(RecipeDiscoveryEngine.class, recipeDiscoveryEngine);
        registerService(InteractionRegistry.class, interactionRegistry);
        registerService(RuleRegistry.class, ruleRegistry);
        registerService(MenuRegistry.class, menuRegistry);
        registerService(CropManager.class, cropManager);
        registerService(GrowthEngine.class, growthEngine);
        active.set(true);
        logger.debug(LogChannel.CORE, "Croppy core domain services initialized successfully.");
    }

    /**
     * Shuts down all active core services and clears state.
     */
    public synchronized void shutdown() {
        logger.debug(LogChannel.CORE, "Shutting down Croppy core domain services...");
        speciesRegistry.clear();
        itemRegistry.clear();
        itemCategoryRegistry.clear();
        itemCategoryRegistry.registerDefaults();
        stationRegistry.clear();
        processRegistry.clear();
        interactionRegistry.clear();
        ruleRegistry.clear();
        menuRegistry.clear();
        recipeRegistry.clear();
        recipeCategoryRegistry.clear();
        craftingRecipeRegistry.clear();
        cropManager.clear();
        stationManager.clear();
        growthEngine.shutdown();
        if (storage != null) {
            storage.shutdown();
            storage = null;
        }
        services.clear();
        active.set(false);
        logger.debug(LogChannel.CORE, "Croppy core domain services shut down cleanly.");
    }

    /**
     * Sets and registers the persistent crop storage provider.
     *
     * @param storage storage implementation
     */
    public void setStorage(CropStorage storage) {
        this.storage = storage;
        if (storage != null) {
            registerService(CropStorage.class, storage);
        }
    }

    /**
     * @return the active crop storage provider if registered
     */
    public Optional<CropStorage> getStorage() {
        return Optional.ofNullable(storage);
    }

    /**
     * @return the central species registry
     */
    public SpeciesRegistry getSpeciesRegistry() {
        return speciesRegistry;
    }

    /**
     * @return the central item registry
     */
    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    /**
     * @return the central item category registry
     */
    public ItemCategoryRegistry getItemCategoryRegistry() {
        return itemCategoryRegistry;
    }

    /**
     * @return the central crop instance manager
     */
    public CropManager getCropManager() {
        return cropManager;
    }

    /**
     * @return the growth simulation engine
     */
    public GrowthEngine getGrowthEngine() {
        return growthEngine;
    }

    /**
     * @return the central station registry
     */
    public StationRegistry getStationRegistry() {
        return stationRegistry;
    }

    /**
     * @return the central process recipe registry
     */
    public ProcessRegistry getProcessRegistry() {
        return processRegistry;
    }

    /**
     * @return the central culinary recipe registry
     */
    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    /**
     * @return the central culinary recipe category registry
     */
    public RecipeCategoryRegistry getRecipeCategoryRegistry() {
        return recipeCategoryRegistry;
    }

    /**
     * @return the central crafting recipe registry
     */
    public CraftingRecipeRegistry getCraftingRecipeRegistry() {
        return craftingRecipeRegistry;
    }

    /**
     * @return the central recipe discovery engine
     */
    public RecipeDiscoveryEngine getRecipeDiscoveryEngine() {
        return recipeDiscoveryEngine;
    }

    /**
     * @return the central menu layout registry
     */
    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    /**
     * @return the interaction definition registry
     */
    public InteractionRegistry getInteractionRegistry() {
        return interactionRegistry;
    }

    /**
     * @return the rule definition registry
     */
    public RuleRegistry getRuleRegistry() {
        return ruleRegistry;
    }

    /**
     * @return the player interaction router
     */
    public InteractionRouter getInteractionRouter() {
        return interactionRouter;
    }

    /**
     * @return the configurable rule evaluation engine
     */
    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    /**
     * @return the spatial station manager
     */
    public DefaultStationManager getStationManager() {
        return stationManager;
    }

    /**
     * @return the sequential process execution engine
     */
    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    /**
     * @return true if the core is currently active
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * Registers a service into the core service container.
     */
    public <S extends Service> void registerService(Class<S> serviceClass, S serviceInstance) {
        Objects.requireNonNull(serviceClass, "Service class cannot be null");
        Objects.requireNonNull(serviceInstance, "Service instance cannot be null");
        services.put(serviceClass, serviceInstance);
    }

    /**
     * Queries a service from the service container.
     */
    @SuppressWarnings("unchecked")
    public <S extends Service> Optional<S> getService(Class<S> serviceClass) {
        return Optional.ofNullable((S) services.get(serviceClass));
    }

    public CroppyLogger getLogger() {
        return logger;
    }
}
