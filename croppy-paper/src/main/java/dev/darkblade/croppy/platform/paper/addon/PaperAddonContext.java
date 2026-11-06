package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonContext;
import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
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
    private final Logger logger;
    private final Plugin hostPlugin;
    private final CroppyCore core;
    private final ExecutorService managedExecutor;

    // Tracked resources for leak-proof cleanup
    private final Set<Listener> registeredListeners = ConcurrentHashMap.newKeySet();
    private final Set<BukkitTask> registeredTasks = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredSpecies = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredItems = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredRecipes = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredProcesses = ConcurrentHashMap.newKeySet();
    private final Set<Key> registeredStations = ConcurrentHashMap.newKeySet();
    private final Set<GrowthModifier> registeredGrowthModifiers = ConcurrentHashMap.newKeySet();

    public PaperAddonContext(AddonDescription description,
                             Path dataFolder,
                             Logger logger,
                             Plugin hostPlugin,
                             CroppyCore core) {
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger("Croppy-" + description.id());
        this.hostPlugin = hostPlugin;
        this.core = core;
        this.managedExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CroppyAddon-" + description.id() + "-Worker");
            t.setDaemon(true);
            return t;
        });
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
    public FertilizerRegistry getFertilizerRegistry() {
        return null;
    }

    @Override
    public RecipeRegistry getRecipeRegistry() {
        return core != null ? core.getRecipeRegistry() : null;
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
        return Optional.empty();
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

    public void trackProcess(Key key) {
        if (key != null) registeredProcesses.add(key);
    }

    public void trackStation(Key key) {
        if (key != null) registeredStations.add(key);
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

    public Set<Key> getRegisteredProcesses() {
        return Collections.unmodifiableSet(registeredProcesses);
    }

    public Set<Key> getRegisteredStations() {
        return Collections.unmodifiableSet(registeredStations);
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
