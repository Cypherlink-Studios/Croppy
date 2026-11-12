package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and loads YAML configuration files into {@link RecipeDefinition} domain templates.
 */
public final class YamlRecipeLoader {

    private final Yaml yaml;

    public YamlRecipeLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single recipe definition from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic messages
     * @return the parsed recipe definition
     */
    @SuppressWarnings("unchecked")
    public RecipeDefinition load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. ID
        String idStr = Objects.requireNonNull(getString(root, "id"), "Missing mandatory 'id' in " + sourceName);
        Key key = Key.parse(idStr);

        // 2. Display Name
        String displayName = Objects.requireNonNull(getString(root, "name"), "Missing mandatory 'name' in " + sourceName);

        // 3. Category
        String categoryStr = getString(root, "category");
        Key categoryKey = (categoryStr != null && !categoryStr.isBlank())
                ? Key.parse(categoryStr)
                : RecipeCategory.ALL.getKey();

        // 4. Station
        String stationStr = Objects.requireNonNull(getString(root, "station"), "Missing mandatory 'station' in " + sourceName);
        Key stationKey = Key.parse(stationStr);

        // 5. Process key (defaults to recipe key)
        String processStr = getString(root, "process");
        Key processKey = (processStr != null && !processStr.isBlank())
                ? Key.parse(processStr)
                : key;

        // 6. Inputs
        List<StoredItem> inputRequirements = new ArrayList<>();
        if (root.get("inputs") instanceof List<?> rawInputs) {
            for (Object itemObj : rawInputs) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    String itemKeyStr = Objects.requireNonNull((String) itemMap.get("item"), "Input item requires 'item' key in " + sourceName);
                    int amount = 1;
                    if (itemMap.get("amount") instanceof Number num) {
                        amount = num.intValue();
                    }
                    inputRequirements.add(StoredItem.of(Key.parse(itemKeyStr), amount));
                }
            }
        }

        // 7. Result
        Key resultItemKey = key;
        int resultAmount = 1;
        if (root.get("result") instanceof Map<?, ?> resultMap) {
            String resKeyStr = (String) resultMap.get("item");
            if (resKeyStr != null && !resKeyStr.isBlank()) {
                resultItemKey = Key.parse(resKeyStr);
            }
            if (resultMap.get("amount") instanceof Number num) {
                resultAmount = num.intValue();
            }
        }

        // 8. Discovery configuration
        List<Key> enabledMethods = new ArrayList<>();
        boolean allowExcess = false;
        HintDefinition hint = HintDefinition.DEFAULT;

        if (root.get("discovery") instanceof Map<?, ?> rawDiscovery) {
            Map<String, Object> discoveryMap = (Map<String, Object>) rawDiscovery;

            if (discoveryMap.get("allow_excess_inputs") instanceof Boolean bool) {
                allowExcess = bool;
            }

            if (discoveryMap.get("methods") instanceof List<?> rawMethods) {
                for (Object mObj : rawMethods) {
                    if (mObj instanceof String mStr) {
                        enabledMethods.add(Key.parse(mStr));
                    }
                }
            }

            if (discoveryMap.get("hint") instanceof Map<?, ?> rawHint) {
                Map<String, Object> hintMap = (Map<String, Object>) rawHint;
                String hintDesc = getString(hintMap, "description");
                if (hintDesc == null) {
                    hintDesc = HintDefinition.DEFAULT.description();
                }

                boolean hideResult = true;
                if (hintMap.get("hide_result") instanceof Boolean bool) {
                    hideResult = bool;
                }

                List<Key> revealedIngredients = new ArrayList<>();
                if (hintMap.get("revealed_ingredients") instanceof List<?> rawRevealed) {
                    for (Object revObj : rawRevealed) {
                        if (revObj instanceof String revStr) {
                            revealedIngredients.add(Key.parse(revStr));
                        }
                    }
                }

                hint = new HintDefinition(hintDesc, revealedIngredients, hideResult, Map.of());
            }
        }

        // 9. Properties
        Map<String, String> properties = new HashMap<>();
        if (root.get("properties") instanceof Map<?, ?> rawProps) {
            for (Map.Entry<?, ?> entry : rawProps.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    properties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new RecipeDefinition(
                key,
                displayName,
                categoryKey,
                stationKey,
                processKey,
                inputRequirements,
                resultItemKey,
                resultAmount,
                hint,
                allowExcess,
                enabledMethods,
                properties
        );
    }

    /**
     * Discovers and registers all recipe configuration files in a folder into the given registry.
     *
     * @param directory directory containing YAML recipes
     * @param registry  target registry
     * @param logger    logger for diagnostics
     * @return total count of registered recipes
     */
    public int loadAll(Path directory, RecipeRegistry registry, Logger logger) {
        Objects.requireNonNull(logger, "logger cannot be null");
        return loadAll(directory, registry, new JulCroppyLogger(logger));
    }

    public int loadAll(Path directory, RecipeRegistry registry, CroppyLogger logger) {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        Objects.requireNonNull(logger, "logger cannot be null");

        if (!Files.isDirectory(directory)) {
            return 0;
        }

        int loadedCount = 0;
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml") || p.getFileName().toString().endsWith(".yaml"))
                    .toList();

            for (Path file : files) {
                try (InputStream in = Files.newInputStream(file)) {
                    RecipeDefinition recipe = load(in, file.getFileName().toString());
                    registry.register(recipe);
                    loadedCount++;
                    logger.debug(LogChannel.LOADER, () -> "Loaded recipe definition: " + recipe.getKey() + " from " + file.getFileName());
                } catch (Exception e) {
                    logger.error(LogChannel.LOADER, "Failed to load recipe YAML definition from: " + file, e);
                }
            }
        } catch (IOException e) {
            logger.error(LogChannel.LOADER, "Failed to walk recipe definitions directory: " + directory, e);
        }

        return loadedCount;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val != null) ? val.toString().trim() : null;
    }
}
