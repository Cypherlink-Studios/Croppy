package dev.darkblade.croppy.core.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.crafting.CraftingType;
import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and loads YAML configuration files into {@link CraftingRecipe} domain models.
 */
public final class YamlCraftingRecipeLoader {

    private final Yaml yaml;

    public YamlCraftingRecipeLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single crafting recipe from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic messages
     * @return the parsed CraftingRecipe
     */
    @SuppressWarnings("unchecked")
    public CraftingRecipe load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. ID
        String idStr = getString(root, "id");
        if (idStr == null || idStr.isBlank()) {
            throw new IllegalArgumentException("Missing mandatory 'id' in " + sourceName);
        }
        Key key = Key.parse(idStr);

        // 2. Type
        String typeStr = getString(root, "type");
        CraftingType type;
        if (typeStr != null && !typeStr.isBlank()) {
            type = CraftingType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } else if (root.containsKey("shape")) {
            type = CraftingType.SHAPED;
        } else {
            type = CraftingType.SHAPELESS;
        }

        // 3. Metadata
        String group = getString(root, "group");
        String category = getString(root, "category");

        // 4. Result
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
        } else if (root.get("result") instanceof String resStr && !resStr.isBlank()) {
            resultItemKey = Key.parse(resStr);
        }

        // 5. Shaped vs Shapeless Ingredients
        List<String> shape = new ArrayList<>();
        Map<Character, IngredientChoice> shapedIngredients = new HashMap<>();
        List<IngredientChoice> shapelessIngredients = new ArrayList<>();

        if (type == CraftingType.SHAPED) {
            if (root.get("shape") instanceof List<?> rawShape) {
                for (Object row : rawShape) {
                    if (row != null) {
                        shape.add(row.toString());
                    }
                }
            }

            if (root.get("ingredients") instanceof Map<?, ?> rawIngs) {
                for (Map.Entry<?, ?> entry : rawIngs.entrySet()) {
                    String symbolStr = entry.getKey().toString();
                    if (symbolStr.length() != 1) {
                        throw new IllegalArgumentException("Shaped ingredient symbol must be a single character, got: '"
                                + symbolStr + "' in " + sourceName);
                    }
                    char symbol = symbolStr.charAt(0);
                    IngredientChoice choice = parseIngredientChoice(entry.getValue(), sourceName);
                    shapedIngredients.put(symbol, choice);
                }
            }

            return CraftingRecipe.shaped(key, group, category, resultItemKey, resultAmount, shape, shapedIngredients);
        } else {
            if (root.get("ingredients") instanceof List<?> rawIngs) {
                for (Object itemObj : rawIngs) {
                    shapelessIngredients.add(parseIngredientChoice(itemObj, sourceName));
                }
            }

            return CraftingRecipe.shapeless(key, group, category, resultItemKey, resultAmount, shapelessIngredients);
        }
    }

    /**
     * Parses a crafting recipe from a filesystem path.
     *
     * @param file path to YAML file
     * @return parsed CraftingRecipe
     * @throws IOException if read fails
     */
    public CraftingRecipe load(Path file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.getFileName().toString());
        }
    }

    /**
     * Recursively traverses a directory and loads all .yml and .yaml crafting recipes into the given registry.
     *
     * @param directory directory containing YAML crafting definitions
     * @param registry  registry to register definitions into
     * @param logger    logger for diagnostics
     * @return total number of recipes successfully loaded
     */
    public int loadAll(Path directory, CraftingRecipeRegistry registry, Logger logger) {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        Logger log = (logger != null) ? logger : Logger.getLogger(YamlCraftingRecipeLoader.class.getName());

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return 0;
        }

        int loadedCount = 0;
        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml") || p.getFileName().toString().endsWith(".yaml"))
                    .toList();

            for (Path file : files) {
                try {
                    CraftingRecipe recipe = load(file);
                    registry.register(recipe);
                    loadedCount++;
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to load crafting recipe from: " + file.toAbsolutePath(), e);
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to walk directory: " + directory.toAbsolutePath(), e);
        }

        return loadedCount;
    }

    private IngredientChoice parseIngredientChoice(Object obj, String sourceName) {
        if (obj instanceof String str) {
            return new IngredientChoice(str);
        }
        if (obj instanceof Map<?, ?> map) {
            Object itemObj = map.get("item");
            if (itemObj == null) {
                throw new IllegalArgumentException("Ingredient mapping missing mandatory 'item' attribute in " + sourceName);
            }
            int amount = 1;
            if (map.get("amount") instanceof Number num) {
                amount = num.intValue();
            }
            return new IngredientChoice(itemObj.toString(), amount);
        }
        throw new IllegalArgumentException("Unsupported ingredient format in " + sourceName + ": " + obj);
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
