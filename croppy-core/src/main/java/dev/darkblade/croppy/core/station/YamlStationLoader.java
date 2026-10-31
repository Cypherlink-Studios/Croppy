package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StationRegistry;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and loads YAML configuration files into {@link StationDefinition} templates.
 */
public final class YamlStationLoader {

    private final Yaml yaml;

    public YamlStationLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single station definition from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic logging
     * @return the parsed station definition
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    @SuppressWarnings("unchecked")
    public StationDefinition load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. ID / Key
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'id' field in station definition " + sourceName);
        }
        String idStr = rawId.toString().trim();
        Key key = idStr.contains(":") ? Key.parse(idStr) : Key.of("croppy", idStr);

        // 2. Display Name
        String displayName = getString(root, "name", getString(root, "display_name", getString(root, "display-name", key.value())));

        // 3. Base Block Material
        String baseBlock = getString(root, "block", getString(root, "material", getString(root, "base_block", "minecraft:barrel")));
        if (!baseBlock.contains(":")) {
            baseBlock = "minecraft:" + baseBlock.toLowerCase(Locale.ROOT);
        }

        // 4. Interaction Type
        String interactionStr = getString(root, "interaction", getString(root, "interaction_type", "HYBRID")).toUpperCase(Locale.ROOT);
        StationInteractionType interactionType;
        try {
            interactionType = StationInteractionType.valueOf(interactionStr);
        } catch (IllegalArgumentException ex) {
            interactionType = StationInteractionType.HYBRID;
        }

        // 5. Capacities
        int inputCapacity = getInt(root, "input_capacity", getInt(root, "inputs", 1));
        int outputCapacity = getInt(root, "output_capacity", getInt(root, "outputs", 1));

        // 6. Menu Key
        Key menuKey = StationDefinition.DEFAULT_MENU_KEY;
        Object rawMenu = root.get("menu");
        if (rawMenu != null && !rawMenu.toString().isBlank()) {
            String menuStr = rawMenu.toString().trim();
            menuKey = menuStr.contains(":") ? Key.parse(menuStr) : Key.of("croppy", menuStr);
        }

        // 7. Display Flag
        boolean hasDisplay = getBoolean(root, "has_display", getBoolean(root, "display", true));

        // 8. Allowed Recipes
        List<Key> allowedRecipes = new ArrayList<>();
        Object rawRecipes = root.get("allowed_recipes");
        if (rawRecipes == null) {
            rawRecipes = root.get("recipes");
        }
        if (rawRecipes instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String str = item.toString().trim();
                    allowedRecipes.add(str.contains(":") ? Key.parse(str) : Key.of("croppy", str));
                }
            }
        }

        // 9. Properties
        Map<String, String> properties = new HashMap<>();
        Object rawProps = root.get("properties");
        if (rawProps instanceof Map<?, ?> pMap) {
            for (Map.Entry<?, ?> entry : pMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    properties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new StationDefinition(
                key,
                displayName,
                baseBlock,
                interactionType,
                inputCapacity,
                outputCapacity,
                menuKey,
                hasDisplay,
                allowedRecipes,
                properties
        );
    }

    /**
     * Loads a station definition directly from a file path.
     *
     * @param path the file path
     * @return parsed definition
     * @throws IOException if an I/O error occurs
     */
    public StationDefinition load(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        try (InputStream in = Files.newInputStream(path)) {
            return load(in, path.getFileName().toString());
        }
    }

    /**
     * Recursively loads all station YAML files from a directory into the given registry.
     *
     * @param directory directory to search
     * @param registry  registry to populate
     * @param logger    logger for status messages
     * @return count of loaded stations
     */
    public int loadAll(Path directory, StationRegistry registry, Logger logger) {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        if (!Files.isDirectory(directory)) {
            return 0;
        }

        int loadedCount = 0;
        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml") || p.getFileName().toString().endsWith(".yaml"))
                    .toList();

            for (Path file : files) {
                try {
                    StationDefinition def = load(file);
                    registry.register(def);
                    loadedCount++;
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.log(Level.SEVERE, "Failed to load station definition from " + file, ex);
                    }
                }
            }
        } catch (IOException ex) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error reading station directory " + directory, ex);
            }
        }

        return loadedCount;
    }

    private String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return (val != null) ? val.toString().trim() : def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val != null) {
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val != null) {
            return Boolean.parseBoolean(val.toString().trim());
        }
        return def;
    }
}
