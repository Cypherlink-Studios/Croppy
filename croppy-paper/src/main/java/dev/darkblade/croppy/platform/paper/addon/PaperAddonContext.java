package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonContext;
import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.core.CroppyCore;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper implementation of {@link AddonContext} tracking Bukkit listeners,
 * scheduled tasks, and registered domain keys for deterministic lifecycle cleanup.
 */
public class PaperAddonContext implements AddonContext {

    private final AddonDescription description;
    private final Path dataFolder;
    private final Path addonSourcePath;
    private ClassLoader addonClassLoader;
    private final Logger logger;
    private final Plugin hostPlugin;
    private final CroppyCore core;
    private final ExecutorService managedExecutor;
    private dev.darkblade.croppy.api.addon.AddonConfig config;

    // Tracked resources for leak-proof cleanup
    private final Set<Listener> registeredListeners = ConcurrentHashMap.newKeySet();
    private final Set<BukkitTask> registeredTasks = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredSpecies = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredItems = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredRecipes = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredCraftingRecipes = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredCategories = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredItemCategories = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredProcesses = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredStations = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredMenus = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredInteractions = ConcurrentHashMap.newKeySet();
    private final Set<GrowthModifier> registeredGrowthModifiers = ConcurrentHashMap.newKeySet();

    public PaperAddonContext(AddonDescription description,
                             Path dataFolder,
                             Logger logger,
                             Plugin hostPlugin,
                             CroppyCore core) {
        this(description, dataFolder, null, null, logger, hostPlugin, core);
    }

    public PaperAddonContext(AddonDescription description,
                             Path dataFolder,
                             Path addonSourcePath,
                             ClassLoader addonClassLoader,
                             Logger logger,
                             Plugin hostPlugin,
                             CroppyCore core) {
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder cannot be null");
        this.addonSourcePath = addonSourcePath;
        this.addonClassLoader = addonClassLoader;
        this.logger = logger != null ? logger : Logger.getLogger("Croppy-" + description.id());
        this.hostPlugin = hostPlugin;
        this.core = core;
        this.managedExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CroppyAddon-" + description.id() + "-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setAddonClassLoader(ClassLoader addonClassLoader) {
        this.addonClassLoader = addonClassLoader;
    }

    @Override
    public AddonDescription getDescription() {
        return description;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Path getDataFolder() {
        return dataFolder;
    }

    @Override
    public SpeciesRegistry getSpeciesRegistry() {
        return core != null ? core.getSpeciesRegistry() : null;
    }

    @Override
    public ItemRegistry getItemRegistry() {
        return core != null ? core.getItemRegistry() : null;
    }

    @Override
    public ItemCategoryRegistry getItemCategoryRegistry() {
        return core != null ? core.getItemCategoryRegistry() : null;
    }

    @Override
    public FertilizerRegistry getFertilizerRegistry() {
        return null;
    }

    @Override
    public RecipeRegistry getRecipeRegistry() {
        return core != null ? core.getRecipeRegistry() : null;
    }

    @Override
    public CraftingRecipeRegistry getCraftingRecipeRegistry() {
        return core != null ? core.getCraftingRecipeRegistry() : null;
    }

    @Override
    public RecipeCategoryRegistry getRecipeCategoryRegistry() {
        return core != null ? core.getRecipeCategoryRegistry() : null;
    }

    @Override
    public ProcessRegistry getProcessRegistry() {
        return core != null ? core.getProcessRegistry() : null;
    }

    @Override
    public StationRegistry getStationRegistry() {
        return core != null ? core.getStationRegistry() : null;
    }

    @Override
    public InteractionRegistry getInteractionRegistry() {
        return core != null ? core.getInteractionRegistry() : null;
    }

    @Override
    public RuleRegistry getRuleRegistry() {
        return core != null ? core.getRuleRegistry() : null;
    }

    @Override
    public MenuRegistry getMenuRegistry() {
        return core != null ? core.getMenuRegistry() : null;
    }

    @Override
    public void runAsync(Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        if (!managedExecutor.isShutdown()) {
            managedExecutor.submit(task);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getPlatformAdapter(Class<T> adapterType) {
        if (adapterType.isInstance(this)) {
            return Optional.of((T) this);
        }
        if (hostPlugin != null && adapterType.isInstance(hostPlugin)) {
            return Optional.of((T) hostPlugin);
        }
        if (config instanceof PaperAddonConfig paperConfig) {
            if (adapterType.isInstance(paperConfig.getConfigurationSection())) {
                return Optional.of((T) paperConfig.getConfigurationSection());
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized dev.darkblade.croppy.api.addon.AddonConfig getConfig() {
        if (config == null) {
            Path configFile = dataFolder.resolve("config.yml");
            try {
                this.config = new PaperAddonConfig(configFile, logger);
            } catch (Throwable t) {
                // Fallback to pure core YamlAddonConfig if Bukkit runtime is absent in tests
                this.config = new dev.darkblade.croppy.core.addon.YamlAddonConfig(configFile);
            }
        }
        return config;
    }

    @Override
    public synchronized void reloadConfig() {
        if (config != null) {
            config.reload();
        } else {
            getConfig();
        }
    }

    @Override
    public synchronized void saveConfig() {
        if (config != null) {
            config.save();
        }
    }

    @Override
    public synchronized void saveDefaultConfig() {
        Path targetFile = dataFolder.resolve("config.yml");
        if (java.nio.file.Files.exists(targetFile)) {
            return;
        }

        java.io.InputStream in = null;

        // 1. Check direct addon source archive/directory first
        if (addonSourcePath != null) {
            if (java.nio.file.Files.isRegularFile(addonSourcePath)
                    && (addonSourcePath.toString().endsWith(".jar") || addonSourcePath.toString().endsWith(".zip"))) {
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(addonSourcePath.toFile())) {
                    java.util.jar.JarEntry entry = jar.getJarEntry("config.yml");
                    if (entry != null) {
                        byte[] bytes = jar.getInputStream(entry).readAllBytes();
                        in = new java.io.ByteArrayInputStream(bytes);
                    }
                } catch (java.io.IOException e) {
                    logger.log(Level.WARNING, "Failed to read config.yml from JAR " + addonSourcePath, e);
                }
            } else if (java.nio.file.Files.isDirectory(addonSourcePath) && !addonSourcePath.equals(dataFolder)) {
                Path src = addonSourcePath.resolve("config.yml");
                if (java.nio.file.Files.exists(src)) {
                    try {
                        byte[] bytes = java.nio.file.Files.readAllBytes(src);
                        in = new java.io.ByteArrayInputStream(bytes);
                    } catch (java.io.IOException e) {
                        logger.log(Level.WARNING, "Failed to read config.yml from " + src, e);
                    }
                }
            }
        }

        // 2. Fallback to addon class loader (which enforces child-first and isolates host config.yml)
        if (in == null && addonClassLoader != null) {
            in = addonClassLoader.getResourceAsStream("config.yml");
        }

        if (in != null) {
            try (java.io.InputStream stream = in) {
                if (targetFile.getParent() != null && !java.nio.file.Files.exists(targetFile.getParent())) {
                    java.nio.file.Files.createDirectories(targetFile.getParent());
                }
                java.nio.file.Files.copy(stream, targetFile);
                logger.info("Extracted default config.yml for addon '" + description.id() + "' to " + targetFile);
                if (config != null) {
                    config.reload();
                }
            } catch (java.io.IOException e) {
                logger.log(Level.SEVERE, "Failed to extract default config.yml for addon '" + description.id() + "'", e);
            }
        }
    }

    /**
     * Registers a Bukkit listener and tracks it for automated removal on disable.
     *
     * @param listener the Bukkit event listener
     */
    public void registerListener(Listener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        if (hostPlugin != null && hostPlugin.getServer() != null) {
            hostPlugin.getServer().getPluginManager().registerEvents(listener, hostPlugin);
        }
        registeredListeners.add(listener);
    }

    public void trackTask(BukkitTask task) {
        if (task != null) {
            registeredTasks.add(task);
        }
    }

    public void trackSpecies(Key key) {
        if (key != null) registeredSpecies.add(key);
    }

    public void trackItem(Key key) {
        if (key != null) registeredItems.add(key);
    }

    public void trackRecipe(Key key) {
        if (key != null) registeredRecipes.add(key);
    }

    public void trackCraftingRecipe(Key key) {
        if (key != null) registeredCraftingRecipes.add(key);
    }

    public void trackCategory(Key key) {
        if (key != null) registeredCategories.add(key);
    }

    public void trackItemCategory(Key key) {
        if (key != null) registeredItemCategories.add(key);
    }

    public void trackProcess(Key key) {
        if (key != null) registeredProcesses.add(key);
    }

    public void trackStation(Key key) {
        if (key != null) registeredStations.add(key);
    }

    public void trackMenu(Key key) {
        if (key != null) registeredMenus.add(key);
    }

    public void trackInteraction(Key key) {
        if (key != null) registeredInteractions.add(key);
    }

    public void trackGrowthModifier(GrowthModifier modifier) {
        if (modifier != null) registeredGrowthModifiers.add(modifier);
    }

    public Set<Listener> getRegisteredListeners() {
        return Collections.unmodifiableSet(registeredListeners);
    }

    public Set<BukkitTask> getRegisteredTasks() {
        return Collections.unmodifiableSet(registeredTasks);
    }

    public Set<Key> getRegisteredSpecies() {
        return Collections.unmodifiableSet(registeredSpecies);
    }

    public Set<Key> getRegisteredItems() {
        return Collections.unmodifiableSet(registeredItems);
    }

    public Set<Key> getRegisteredRecipes() {
        return Collections.unmodifiableSet(registeredRecipes);
    }

    public Set<Key> getRegisteredCraftingRecipes() {
        return Collections.unmodifiableSet(registeredCraftingRecipes);
    }

    public Set<Key> getRegisteredCategories() {
        return Collections.unmodifiableSet(registeredCategories);
    }

    public Set<Key> getRegisteredItemCategories() {
        return Collections.unmodifiableSet(registeredItemCategories);
    }

    public Set<Key> getRegisteredProcesses() {
        return Collections.unmodifiableSet(registeredProcesses);
    }

    public Set<Key> getRegisteredStations() {
        return Collections.unmodifiableSet(registeredStations);
    }

    public Set<Key> getRegisteredMenus() {
        return Collections.unmodifiableSet(registeredMenus);
    }

    public Set<Key> getRegisteredInteractions() {
        return Collections.unmodifiableSet(registeredInteractions);
    }

    public Set<GrowthModifier> getRegisteredGrowthModifiers() {
        return Collections.unmodifiableSet(registeredGrowthModifiers);
    }

    public ExecutorService getManagedExecutor() {
        return managedExecutor;
    }

    /**
     * Shuts down the managed executor and awaits brief termination.
     */
    public void shutdownExecutor() {
        managedExecutor.shutdownNow();
        try {
            if (!managedExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.warning("Managed executor for addon '" + description.id() + "' did not terminate within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
