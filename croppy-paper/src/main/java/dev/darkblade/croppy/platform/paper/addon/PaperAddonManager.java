package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.AddonState;
import dev.darkblade.croppy.api.addon.CroppyAddon;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.addon.AddonGraphValidator;
import dev.darkblade.croppy.core.addon.DefaultAddonManager;
import dev.darkblade.croppy.core.item.YamlItemLoader;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.resourcepack.ResourcePackMerger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper implementation of AddonManager coordinating ClassLoaders,
 * YAML content loading, DAG ordering, and 5-phase hot-swapping.
 */
public class PaperAddonManager extends DefaultAddonManager {

    private final CroppyPlugin plugin;
    private final CroppyCore core;
    private final ResourcePackMerger resourcePackMerger;

    private final Map<String, AddonClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final Map<String, CroppyAddon> addonInstances = new ConcurrentHashMap<>();
    private final Map<String, PaperAddonContext> addonContexts = new ConcurrentHashMap<>();

    public PaperAddonManager(CroppyPlugin plugin, CroppyCore core, Path addonsDir, ResourcePackMerger resourcePackMerger) {
        super(addonsDir, plugin != null ? plugin.getLogger() : Logger.getLogger(PaperAddonManager.class.getName()));
        this.plugin = plugin;
        this.core = core;
        this.resourcePackMerger = resourcePackMerger;
    }

    public synchronized void enableAll() {
        discoverAddons();
        List<AddonDescription> order;
        try {
            order = AddonGraphValidator.resolveOrder(getDiscoveredAddons());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to resolve addon dependency graph: " + e.getMessage(), e);
            return;
        }

        for (AddonDescription desc : order) {
            try {
                enableAddon(desc.id());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to enable addon '" + desc.id() + "': " + e.getMessage(), e);
            }
        }
    }

    public synchronized void disableAll() {
        List<AddonDescription> enabled = new ArrayList<>();
        for (AddonDescription desc : getDiscoveredAddons()) {
            if (getState(desc.id()) == AddonState.ENABLED) {
                enabled.add(desc);
            }
        }
        // Disable in reverse order
        Collections.reverse(enabled);
        for (AddonDescription desc : enabled) {
            disableAddon(desc.id());
        }
    }

    @Override
    public synchronized boolean loadAddon(String addonId) {
        AddonDescription desc = discoveredAddons.get(addonId);
        if (desc == null || getState(addonId) != AddonState.ON_DISK) {
            return false;
        }

        Path path = addonPaths.get(addonId);
        if (path == null || !Files.exists(path)) {
            return false;
        }

        // Prepare context
        Path dataFolder = addonsDirectory.resolve(desc.id());
        PaperAddonContext context = new PaperAddonContext(
                desc, dataFolder,
                Logger.getLogger("Croppy-" + desc.id()),
                plugin, core
        );
        addonContexts.put(addonId, context);

        // If Java code addon, instantiate AddonClassLoader
        if (desc.isCodeAddon()) {
            try {
                URL jarUrl = path.toUri().toURL();
                ClassLoader parentLoader = plugin != null ? plugin.getClass().getClassLoader() : getClass().getClassLoader();
                AddonClassLoader loader = new AddonClassLoader(new URL[]{jarUrl}, parentLoader, desc);

                // Link dependencies
                for (String depId : desc.dependencies()) {
                    AddonClassLoader depLoader = classLoaders.get(depId);
                    if (depLoader != null) {
                        loader.addDependencyLoader(depLoader);
                    }
                }
                for (String softDepId : desc.softDependencies()) {
                    AddonClassLoader depLoader = classLoaders.get(softDepId);
                    if (depLoader != null) {
                        loader.addDependencyLoader(depLoader);
                    }
                }

                classLoaders.put(addonId, loader);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create class loader for addon '" + addonId + "'", e);
                return false;
            }
        }

        return super.loadAddon(addonId);
    }

    @Override
    public synchronized boolean enableAddon(String addonId) {
        AddonDescription desc = discoveredAddons.get(addonId);
        if (desc == null) {
            return false;
        }

        if (getState(addonId) == AddonState.ON_DISK) {
            if (!loadAddon(addonId)) {
                return false;
            }
        }

        PaperAddonContext context = addonContexts.get(addonId);
        Path path = addonPaths.get(addonId);

        // 1. Load declarative YAML contents if path is directory
        if (path != null && Files.isDirectory(path) && core != null) {
            loadDirectoryContent(context, path);
        }

        // 2. If code addon, instantiate and invoke onEnable
        if (desc.isCodeAddon()) {
            AddonClassLoader loader = classLoaders.get(addonId);
            if (loader != null && desc.main().isPresent()) {
                try {
                    Class<?> mainClass = loader.loadClass(desc.main().get());
                    if (CroppyAddon.class.isAssignableFrom(mainClass)) {
                        CroppyAddon instance = (CroppyAddon) mainClass.getConstructor().newInstance();
                        addonInstances.put(addonId, instance);
                        instance.onEnable(context);
                    } else {
                        logger.severe("Main class " + desc.main().get() + " does not implement CroppyAddon");
                        return false;
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error initializing code addon '" + addonId + "'", e);
                    return false;
                }
            }
        }

        // 3. Merge resource pack assets if merger is available
        if (resourcePackMerger != null && path != null) {
            try {
                resourcePackMerger.mergeAddonAssets(desc, path);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to merge assets for addon '" + addonId + "'", e);
            }
        }

        return super.enableAddon(addonId);
    }

    private void loadDirectoryContent(PaperAddonContext context, Path root) {
        // Load items
        Path itemsDir = root.resolve("items");
        if (Files.exists(itemsDir) && Files.isDirectory(itemsDir)) {
            YamlItemLoader itemLoader = new YamlItemLoader();
            try (java.util.stream.Stream<Path> stream = Files.walk(itemsDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try {
                        String category = file.getParent().getFileName().toString();
                        dev.darkblade.croppy.api.item.ItemDefinition def = itemLoader.load(file, category);
                        core.getItemRegistry().register(def);
                        context.trackItem(def.getKey());
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to load item from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error scanning addon items directory: " + itemsDir, e);
            }
        }

        // Load crops
        Path cropsDir = root.resolve("crops");
        if (Files.exists(cropsDir) && Files.isDirectory(cropsDir)) {
            YamlSpeciesLoader speciesLoader = new YamlSpeciesLoader();
            List<dev.darkblade.croppy.api.species.Species> loaded = speciesLoader.loadDirectory(cropsDir, logger);
            for (dev.darkblade.croppy.api.species.Species species : loaded) {
                core.getSpeciesRegistry().register(species);
                context.trackSpecies(species.getKey());
            }
        }
    }

    @Override
    public synchronized boolean disableAddon(String addonId) {
        if (getState(addonId) != AddonState.ENABLED) {
            return false;
        }

        CroppyAddon instance = addonInstances.get(addonId);
        PaperAddonContext context = addonContexts.get(addonId);
        AddonClassLoader loader = classLoaders.get(addonId);

        if (context != null) {
            AddonDecommissioner.decommission(instance, context, loader, core, logger, false);
        }

        addonInstances.remove(addonId);
        return super.disableAddon(addonId);
    }

    @Override
    public synchronized boolean unloadAddon(String addonId) {
        CroppyAddon instance = addonInstances.get(addonId);
        PaperAddonContext context = addonContexts.get(addonId);
        AddonClassLoader loader = classLoaders.get(addonId);

        if (context != null) {
            AddonDecommissioner.decommission(instance, context, loader, core, logger, true);
        }

        addonInstances.remove(addonId);
        classLoaders.remove(addonId);
        addonContexts.remove(addonId);

        return super.unloadAddon(addonId);
    }

    public Optional<PaperAddonContext> getContext(String addonId) {
        return Optional.ofNullable(addonContexts.get(addonId));
    }

    public Optional<AddonClassLoader> getClassLoader(String addonId) {
        return Optional.ofNullable(classLoaders.get(addonId));
    }
}
