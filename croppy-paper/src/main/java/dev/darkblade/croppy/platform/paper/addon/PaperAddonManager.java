package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.AddonState;
import dev.darkblade.croppy.api.addon.CroppyAddon;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.addon.AddonGraphValidator;
import dev.darkblade.croppy.core.addon.DefaultAddonManager;
import dev.darkblade.croppy.core.crafting.YamlCraftingRecipeLoader;
import dev.darkblade.croppy.core.interaction.YamlInteractionLoader;
import dev.darkblade.croppy.core.item.YamlItemLoader;
import dev.darkblade.croppy.core.menu.YamlMenuLoader;
import dev.darkblade.croppy.core.process.YamlProcessLoader;
import dev.darkblade.croppy.core.recipe.YamlRecipeLoader;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import dev.darkblade.croppy.core.station.YamlStationLoader;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.resourcepack.ResourcePackMerger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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
        this(plugin, core, addonsDir, resourcePackMerger, resolvePluginLogger(plugin));
    }

    private static CroppyLogger resolvePluginLogger(CroppyPlugin plugin) {
        if (plugin == null) {
            return NoOpCroppyLogger.INSTANCE;
        }
        CroppyLogger paperLogger = plugin.getCroppyLogger();
        if (paperLogger != null) {
            return paperLogger;
        }
        return plugin.getLogger() != null ? new JulCroppyLogger(plugin.getLogger()) : NoOpCroppyLogger.INSTANCE;
    }

    public PaperAddonManager(CroppyPlugin plugin,
                             CroppyCore core,
                             Path addonsDir,
                             ResourcePackMerger resourcePackMerger,
                             CroppyLogger logger) {
        super(addonsDir, logger);
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
            logger.error(LogChannel.ADDON, "Failed to resolve addon dependency graph: " + e.getMessage(), e);
            return;
        }

        for (AddonDescription desc : order) {
            try {
                enableAddon(desc.id());
            } catch (Exception e) {
                logger.error(LogChannel.ADDON, "Failed to enable addon '" + desc.id() + "': " + e.getMessage(), e);
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

        // 1. Delegate to super.loadAddon for Path Ownership Gate validation and state change
        if (!super.loadAddon(addonId)) {
            return false;
        }

        // 2. Prepare context with dataFolder resolved by DefaultAddonManager
        Path dataFolder = getDataFolder(addonId).orElseGet(() -> addonsDirectory.resolve(desc.id()));
        PaperAddonContext context = new PaperAddonContext(
                desc, dataFolder, path, null,
                logger,
                plugin, core
        );
        addonContexts.put(addonId, context);

        // 3. If Java code addon, instantiate AddonClassLoader
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
                context.setAddonClassLoader(loader);
            } catch (Exception e) {
                logger.error(LogChannel.ADDON, "Failed to create class loader for addon '" + addonId + "'", e);
                return false;
            }
        }

        return true;
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

        // 1. Load declarative YAML contents from directory or JAR archive
        if (path != null && core != null) {
            if (Files.isDirectory(path)) {
                loadDirectoryContent(context, path);
            } else if (Files.isRegularFile(path) && (path.toString().endsWith(".jar") || path.toString().endsWith(".zip"))) {
                loadJarContent(context, path);
            }
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
                        logger.error(LogChannel.ADDON, "Main class " + desc.main().get() + " does not implement CroppyAddon");
                        return false;
                    }
                } catch (Exception e) {
                    logger.error(LogChannel.ADDON, "Error initializing code addon '" + addonId + "'", e);
                    return false;
                }
            }
        }

        // 3. Merge resource pack assets if merger is available
        if (resourcePackMerger != null && path != null) {
            try {
                resourcePackMerger.mergeAddonAssets(desc, path);
            } catch (Exception e) {
                logger.warn(LogChannel.ADDON, "Failed to merge assets for addon '" + addonId + "'", e);
            }
        }

        boolean enabled = super.enableAddon(addonId);
        if (enabled && context != null) {
            logger.info(LogChannel.ADDON, () ->
                    "Enabled addon '" + desc.id() + "' v" + desc.version() + formatAddonSummary(context));
        }
        return enabled;
    }

    private String formatAddonSummary(PaperAddonContext context) {
        List<String> parts = new ArrayList<>();
        appendSummaryCount(parts, context.getRegisteredSpecies().size(), "crop", "crops");
        appendSummaryCount(parts, context.getRegisteredItems().size(), "item", "items");
        appendSummaryCount(parts, context.getRegisteredRecipes().size(), "recipe", "recipes");
        appendSummaryCount(parts, context.getRegisteredCraftingRecipes().size(), "crafting recipe", "crafting recipes");
        appendSummaryCount(parts, context.getRegisteredStations().size(), "station", "stations");
        appendSummaryCount(parts, context.getRegisteredProcesses().size(), "process", "processes");
        appendSummaryCount(parts, context.getRegisteredMenus().size(), "menu", "menus");
        appendSummaryCount(parts, context.getRegisteredInteractions().size(), "interaction", "interactions");
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    private static void appendSummaryCount(List<String> parts, int count, String singular, String plural) {
        if (count > 0) {
            parts.add(count + " " + (count == 1 ? singular : plural));
        }
    }

    private void loadDirectoryContent(PaperAddonContext context, Path root) {
        CroppyLogger addonLogger = context.getCroppyLogger();

        // 1. Load items
        Path itemsDir = root.resolve("items");
        if (Files.exists(itemsDir) && Files.isDirectory(itemsDir)) {
            YamlItemLoader itemLoader = new YamlItemLoader(addonLogger);
            try (Stream<Path> stream = Files.walk(itemsDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try {
                        String category = (file.getParent() != null && !file.getParent().equals(itemsDir))
                                ? file.getParent().getFileName().toString()
                                : "misc";
                        dev.darkblade.croppy.api.item.ItemDefinition def = itemLoader.load(file, category);
                        core.getItemRegistry().register(def);
                        context.trackItem(def.getKey());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon item '" + def.getKey() + "' from " + file.getFileName());
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load item from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon items directory: " + itemsDir, e);
            }
        }

        // 2. Load crops
        Path cropsDir = root.resolve("crops");
        if (Files.exists(cropsDir) && Files.isDirectory(cropsDir)) {
            YamlSpeciesLoader speciesLoader = new YamlSpeciesLoader();
            List<dev.darkblade.croppy.api.species.Species> loaded = speciesLoader.loadDirectory(cropsDir, addonLogger);
            for (dev.darkblade.croppy.api.species.Species species : loaded) {
                core.getSpeciesRegistry().register(species);
                context.trackSpecies(species.getKey());
            }
        }

        // 3. Load recipes
        Path recipesDir = root.resolve("recipes");
        if (Files.exists(recipesDir) && Files.isDirectory(recipesDir)) {
            YamlRecipeLoader recipeLoader = new YamlRecipeLoader();
            try (Stream<Path> stream = Files.walk(recipesDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try (InputStream in = Files.newInputStream(file)) {
                        dev.darkblade.croppy.api.recipe.RecipeDefinition recipe = recipeLoader.load(in, file.getFileName().toString());
                        core.getRecipeRegistry().register(recipe);
                        context.trackRecipe(recipe.key());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon recipe '" + recipe.key() + "' from " + file.getFileName());
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load recipe from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon recipes directory: " + recipesDir, e);
            }
        }

        // 4. Load stations
        Path stationsDir = root.resolve("stations");
        if (Files.exists(stationsDir) && Files.isDirectory(stationsDir)) {
            YamlStationLoader stationLoader = new YamlStationLoader();
            try (Stream<Path> stream = Files.walk(stationsDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try (InputStream in = Files.newInputStream(file)) {
                        dev.darkblade.croppy.api.station.StationDefinition station = stationLoader.load(in, file.getFileName().toString());
                        core.getStationRegistry().register(station);
                        context.trackStation(station.key());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon station '" + station.key() + "' from " + file.getFileName());
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load station from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon stations directory: " + stationsDir, e);
            }
        }

        // 5. Load processes
        Path processesDir = root.resolve("processes");
        if (Files.exists(processesDir) && Files.isDirectory(processesDir)) {
            YamlProcessLoader processLoader = new YamlProcessLoader();
            try (Stream<Path> stream = Files.walk(processesDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try (InputStream in = Files.newInputStream(file)) {
                        dev.darkblade.croppy.api.process.ProcessDefinition process = processLoader.load(in, file.getFileName().toString());
                        core.getProcessRegistry().register(process);
                        context.trackProcess(process.key());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon process '" + process.key() + "' from " + file.getFileName());
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load process from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon processes directory: " + processesDir, e);
            }
        }

        // 6. Load menus
        Path menusDir = root.resolve("menus");
        if (Files.exists(menusDir) && Files.isDirectory(menusDir)) {
            YamlMenuLoader menuLoader = new YamlMenuLoader();
            try (Stream<Path> stream = Files.walk(menusDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try (InputStream in = Files.newInputStream(file)) {
                        dev.darkblade.croppy.api.menu.MenuDefinition menu = menuLoader.load(in, file.getFileName().toString());
                        core.getMenuRegistry().register(menu);
                        context.trackMenu(menu.getKey());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon menu '" + menu.getKey() + "' from " + file.getFileName());
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load menu from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon menus directory: " + menusDir, e);
            }
        }

        // 7. Load interactions
        Path interactionsDir = root.resolve("interactions");
        if (Files.exists(interactionsDir) && Files.isDirectory(interactionsDir)) {
            YamlInteractionLoader interactionLoader = new YamlInteractionLoader();
            try (Stream<Path> stream = Files.walk(interactionsDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try (InputStream in = Files.newInputStream(file)) {
                        List<dev.darkblade.croppy.api.interaction.InteractionDefinition> interactions =
                                interactionLoader.loadMultiple(in, file.getFileName().toString());
                        for (dev.darkblade.croppy.api.interaction.InteractionDefinition interaction : interactions) {
                            core.getInteractionRegistry().register(interaction);
                            context.trackInteraction(interaction.key());
                            addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon interaction '" + interaction.key() + "' from " + file.getFileName());
                        }
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load interaction from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon interactions directory: " + interactionsDir, e);
            }
        }

        // 8. Load crafting recipes
        Path craftingDir = root.resolve("crafting");
        if (Files.exists(craftingDir) && Files.isDirectory(craftingDir)) {
            YamlCraftingRecipeLoader craftingLoader = new YamlCraftingRecipeLoader();
            try (Stream<Path> stream = Files.walk(craftingDir)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))::iterator) {
                    try (InputStream in = Files.newInputStream(file)) {
                        dev.darkblade.croppy.api.crafting.CraftingRecipe recipe = craftingLoader.load(in, file.getFileName().toString());
                        core.getCraftingRecipeRegistry().register(recipe);
                        context.trackCraftingRecipe(recipe.getKey());
                        if (plugin != null && plugin.getCraftingRecipeService() != null) {
                            plugin.getCraftingRecipeService().registerRecipe(recipe);
                        }
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon crafting recipe '" + recipe.getKey() + "' from " + file.getFileName());
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load crafting recipe from " + file, e);
                    }
                }
            } catch (IOException e) {
                logger.warn(LogChannel.ADDON, "Error scanning addon crafting directory: " + craftingDir, e);
            }
        }
    }

    private void loadJarContent(PaperAddonContext context, Path jarPath) {
        CroppyLogger addonLogger = context.getCroppyLogger();
        YamlSpeciesLoader speciesLoader = new YamlSpeciesLoader();
        YamlItemLoader itemLoader = new YamlItemLoader(addonLogger);
        YamlRecipeLoader recipeLoader = new YamlRecipeLoader();
        YamlStationLoader stationLoader = new YamlStationLoader();
        YamlProcessLoader processLoader = new YamlProcessLoader();
        YamlMenuLoader menuLoader = new YamlMenuLoader();
        YamlInteractionLoader interactionLoader = new YamlInteractionLoader();
        YamlCraftingRecipeLoader craftingLoader = new YamlCraftingRecipeLoader();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || (!name.endsWith(".yml") && !name.endsWith(".yaml"))) {
                    continue;
                }

                // 1. Crops
                if (name.startsWith("crops/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        dev.darkblade.croppy.api.species.Species species = speciesLoader.load(in, name);
                        core.getSpeciesRegistry().register(species);
                        context.trackSpecies(species.getKey());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon crop species '" + species.getKey() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load crop species from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 2. Items
                else if (name.startsWith("items/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        String[] parts = name.split("/");
                        String category = parts.length > 2 ? parts[1] : "misc";
                        dev.darkblade.croppy.api.item.ItemDefinition def = itemLoader.load(in, category, name);
                        core.getItemRegistry().register(def);
                        context.trackItem(def.getKey());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon item '" + def.getKey() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load item from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 3. Recipes
                else if (name.startsWith("recipes/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        dev.darkblade.croppy.api.recipe.RecipeDefinition recipe = recipeLoader.load(in, name);
                        core.getRecipeRegistry().register(recipe);
                        context.trackRecipe(recipe.key());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon recipe '" + recipe.key() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load recipe from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 4. Stations
                else if (name.startsWith("stations/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        dev.darkblade.croppy.api.station.StationDefinition station = stationLoader.load(in, name);
                        core.getStationRegistry().register(station);
                        context.trackStation(station.key());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon station '" + station.key() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load station from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 5. Processes
                else if (name.startsWith("processes/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        dev.darkblade.croppy.api.process.ProcessDefinition process = processLoader.load(in, name);
                        core.getProcessRegistry().register(process);
                        context.trackProcess(process.key());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon process '" + process.key() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load process from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 6. Menus
                else if (name.startsWith("menus/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        dev.darkblade.croppy.api.menu.MenuDefinition menu = menuLoader.load(in, name);
                        core.getMenuRegistry().register(menu);
                        context.trackMenu(menu.getKey());
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon menu '" + menu.getKey() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load menu from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 7. Interactions
                else if (name.startsWith("interactions/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        List<dev.darkblade.croppy.api.interaction.InteractionDefinition> interactions =
                                interactionLoader.loadMultiple(in, name);
                        for (dev.darkblade.croppy.api.interaction.InteractionDefinition interaction : interactions) {
                            core.getInteractionRegistry().register(interaction);
                            context.trackInteraction(interaction.key());
                            addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon interaction '" + interaction.key() + "' from " + name);
                        }
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load interaction from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
                // 8. Crafting recipes
                else if (name.startsWith("crafting/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        dev.darkblade.croppy.api.crafting.CraftingRecipe recipe = craftingLoader.load(in, name);
                        core.getCraftingRecipeRegistry().register(recipe);
                        context.trackCraftingRecipe(recipe.getKey());
                        if (plugin != null && plugin.getCraftingRecipeService() != null) {
                            plugin.getCraftingRecipeService().registerRecipe(recipe);
                        }
                        addonLogger.debug(LogChannel.LOADER, () -> "Loaded addon crafting recipe '" + recipe.getKey() + "' from " + name);
                    } catch (Exception e) {
                        logger.warn(LogChannel.ADDON, "Failed to load crafting recipe from " + name + " in " + jarPath.getFileName(), e);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn(LogChannel.ADDON, "Error reading JAR file " + jarPath, e);
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
