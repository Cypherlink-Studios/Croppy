package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.interaction.DefaultInteractionRegistry;
import dev.darkblade.croppy.core.interaction.InteractionRouter;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.rule.DefaultRuleRegistry;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.DefaultStationRegistry;

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

    private final Logger logger;
    private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();
    private final DefaultSpeciesRegistry speciesRegistry = new DefaultSpeciesRegistry();
    private final DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
    private final DefaultStationRegistry stationRegistry = new DefaultStationRegistry();
    private final DefaultProcessRegistry processRegistry = new DefaultProcessRegistry();
    private final DefaultInteractionRegistry interactionRegistry = new DefaultInteractionRegistry();
    private final DefaultRuleRegistry ruleRegistry = new DefaultRuleRegistry();
    private final CropManager cropManager = new CropManager();
    private final DefaultStationManager stationManager = new DefaultStationManager();
    private final GrowthEngine growthEngine = new GrowthEngine();
    private final ProcessEngine processEngine = new ProcessEngine(processRegistry);
    private final InteractionRouter interactionRouter = new InteractionRouter(interactionRegistry);
    private final RuleEngine ruleEngine = new RuleEngine(ruleRegistry);
    private CropStorage storage;
    private final AtomicBoolean active = new AtomicBoolean(false);

    public CroppyCore(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
    }

    public CroppyCore() {
        this(Logger.getLogger(CroppyCore.class.getName()));
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
        logger.info("Initializing Croppy core domain services...");
        speciesRegistry.clear();
        itemRegistry.clear();
        stationRegistry.clear();
        processRegistry.clear();
        interactionRegistry.clear();
        ruleRegistry.clear();
        cropManager.clear();
        stationManager.clear();
        registerService(SpeciesRegistry.class, speciesRegistry);
        registerService(ItemRegistry.class, itemRegistry);
        registerService(StationRegistry.class, stationRegistry);
        registerService(ProcessRegistry.class, processRegistry);
        registerService(InteractionRegistry.class, interactionRegistry);
        registerService(RuleRegistry.class, ruleRegistry);
        registerService(CropManager.class, cropManager);
        registerService(GrowthEngine.class, growthEngine);
        active.set(true);
        logger.info("Croppy core domain services initialized successfully.");
    }

    /**
     * Shuts down all active core services and clears state.
     */
    public synchronized void shutdown() {
        logger.info("Shutting down Croppy core domain services...");
        speciesRegistry.clear();
        itemRegistry.clear();
        stationRegistry.clear();
        processRegistry.clear();
        interactionRegistry.clear();
        ruleRegistry.clear();
        cropManager.clear();
        stationManager.clear();
        growthEngine.shutdown();
        if (storage != null) {
            storage.shutdown();
            storage = null;
        }
        services.clear();
        active.set(false);
        logger.info("Croppy core domain services shut down cleanly.");
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

    public Logger getLogger() {
        return logger;
    }
}
