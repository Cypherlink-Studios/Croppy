package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and validates YAML configuration files for {@link ItemDefinition}s.
 */
public class YamlItemLoader {

    private final Yaml yaml;

    public YamlItemLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses an ItemDefinition from an input stream.
     *
     * @param in input stream
     * @param defaultCategory category to fallback to if not declared in the YAML
     * @param sourceName name of the source file for logging
     * @return the parsed ItemDefinition
     * @throws IllegalArgumentException if the configuration is invalid
     */
    @SuppressWarnings("unchecked")
    public ItemDefinition load(InputStream in, String defaultCategory, String sourceName) {
        Objects.requireNonNull(in, "Input stream cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. Key / ID
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'id' field in " + sourceName);
        }
        String idStr = rawId.toString().trim();
        Key key = idStr.contains(":") ? Key.parse(idStr) : Key.of("croppy", idStr);

        // 2. Category
        String category = defaultCategory;
        Object rawCat = root.get("category");
        if (rawCat != null && !rawCat.toString().isBlank()) {
            category = rawCat.toString().trim();
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Missing category for item " + key + " in " + sourceName);
        }

        // 3. Material & Custom Model Data (support flat or nested 'item' map)
        Map<String, Object> itemMap = null;
        Object rawItem = root.get("item");
        if (rawItem instanceof Map<?, ?> map) {
            itemMap = (Map<String, Object>) map;
        }

        String material = getString(root, "material", null);
        if (material == null && itemMap != null) {
            material = getString(itemMap, "material", null);
        }
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("Missing 'material' for item " + key + " in " + sourceName);
        }

        int customModelData = getInt(root, "custom-model-data", getInt(root, "custom_model_data", 0));
        if (customModelData == 0 && itemMap != null) {
            customModelData = getInt(itemMap, "custom-model-data", getInt(itemMap, "custom_model_data", 0));
        }

        // 4. Native Item Model (Minecraft 1.21.4+)
        String itemModelStr = getString(root, "item-model", getString(root, "item_model", null));
        if (itemModelStr == null && itemMap != null) {
            itemModelStr = getString(itemMap, "item-model", getString(itemMap, "item_model", null));
        }
        Key itemModel = null;
        if (itemModelStr != null && !itemModelStr.isBlank()) {
            String trimmedModel = itemModelStr.trim();
            try {
                itemModel = trimmedModel.contains(":") ? Key.parse(trimmedModel) : Key.of("croppy", trimmedModel);
            } catch (Exception e) {
                Logger.getLogger(YamlItemLoader.class.getName())
                        .warning("Invalid item-model key '" + trimmedModel + "' in " + sourceName + ": " + e.getMessage());
            }
        }

        // 4b. Skull Texture (Base64 or Mojang URL)
        String texture = getString(root, "texture", getString(root, "skull-texture", getString(root, "skull_texture", null)));
        if (texture == null && itemMap != null) {
            texture = getString(itemMap, "texture", getString(itemMap, "skull-texture", getString(itemMap, "skull_texture", null)));
        }
        if (texture != null && texture.isBlank()) {
            texture = null;
        } else if (texture != null) {
            texture = texture.trim();
        }

        // 5. Display Name
        String displayName = getString(root, "name", getString(root, "display_name", getString(root, "display-name", null)));
        if (displayName == null && itemMap != null) {
            displayName = getString(itemMap, "name", getString(itemMap, "display_name", getString(itemMap, "display-name", null)));
        }
        if (displayName == null) {
            displayName = key.value();
        }

        // 6. Lore
        List<String> lore = new ArrayList<>();
        Object rawLore = root.get("lore");
        if (rawLore == null && itemMap != null) {
            rawLore = itemMap.get("lore");
        }
        if (rawLore instanceof List<?> list) {
            for (Object line : list) {
                if (line != null) {
                    lore.add(line.toString());
                }
            }
        }

        // 7. PDC Properties
        Map<String, String> pdcProperties = new HashMap<>();
        Object rawPdc = root.get("pdc");
        if (rawPdc instanceof Map<?, ?> pdcMap) {
            for (Map.Entry<?, ?> entry : pdcMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    pdcProperties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new ItemDefinition(
                key,
                category.toLowerCase(Locale.ROOT),
                material,
                customModelData,
                itemModel,
                texture,
                displayName,
                lore,
                pdcProperties
        );
    }

    /**
     * Loads an ItemDefinition from a file on disk.
     */
    public ItemDefinition load(Path filePath, String defaultCategory) throws IOException {
        try (InputStream in = Files.newInputStream(filePath)) {
            return load(in, defaultCategory, filePath.getFileName().toString());
        }
    }

    /**
     * Loads all YAML item definitions in a directory for a specific category.
     */
    public int loadDirectory(Path dir, String category, Logger logger, ItemRegistry registry) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return 0;
        }

        int count = 0;
        try (Stream<Path> stream = Files.walk(dir, 1)) {
            List<Path> files = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".yml"))
                    .toList();

            for (Path file : files) {
                try {
                    ItemDefinition def = load(file, category);
                    registry.register(def);
                    count++;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load item from " + file.getFileName() + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read item directory: " + dir, e);
        }
        return count;
    }

    /**
     * Scans the root items directory, treating each sub-folder as a category.
     */
    public int loadAllCategories(Path itemsRootDir, Logger logger, ItemRegistry registry) {
        if (!Files.exists(itemsRootDir) || !Files.isDirectory(itemsRootDir)) {
            return 0;
        }

        int totalLoaded = 0;
        try (Stream<Path> stream = Files.list(itemsRootDir)) {
            List<Path> subDirs = stream
                    .filter(Files::isDirectory)
                    .toList();

            for (Path subDir : subDirs) {
                String category = subDir.getFileName().toString();
                int loaded = loadDirectory(subDir, category, logger, registry);
                totalLoaded += loaded;
                logger.info("Loaded " + loaded + " items from category '" + category + "'.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to scan items directory: " + itemsRootDir, e);
        }
        return totalLoaded;
    }

    private String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        } else if (val != null) {
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }
}
