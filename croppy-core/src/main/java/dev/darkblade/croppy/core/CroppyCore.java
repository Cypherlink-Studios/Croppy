package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.growth.GrowthEngine;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;

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
    private final CropManager cropManager = new CropManager();
    private final GrowthEngine growthEngine = new GrowthEngine();
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
        cropManager.clear();
        registerService(SpeciesRegistry.class, speciesRegistry);
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
        cropManager.clear();
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
