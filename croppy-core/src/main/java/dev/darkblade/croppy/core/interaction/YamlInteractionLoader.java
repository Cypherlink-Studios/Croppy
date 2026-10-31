package dev.darkblade.croppy.core.interaction;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionAction;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and loads YAML configuration files into {@link InteractionDefinition} models.
 */
public final class YamlInteractionLoader {

    private final Yaml yaml;

    public YamlInteractionLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single interaction definition from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic logging
     * @return the parsed interaction definition
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    @SuppressWarnings("unchecked")
    public InteractionDefinition load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. ID / Key
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'id' field in interaction definition " + sourceName);
        }
        String idStr = rawId.toString().trim();
        Key key = idStr.contains(":") ? Key.parse(idStr) : Key.of("croppy", idStr);

        // 2. Target Station
        Optional<Key> targetStation = Optional.empty();
        Object rawStation = root.get("station");
        if (rawStation == null) {
            rawStation = root.get("target_station");
        }
        if (rawStation != null && !rawStation.toString().isBlank()) {
            String s = rawStation.toString().trim();
            targetStation = Optional.of(s.contains(":") ? Key.parse(s) : Key.of("croppy", s));
        }

        // 3. Trigger
        String triggerStr = getString(root, "trigger", "RIGHT_CLICK").toUpperCase(Locale.ROOT);
        InteractionTrigger trigger;
        try {
            trigger = InteractionTrigger.valueOf(triggerStr);
        } catch (IllegalArgumentException ex) {
            trigger = InteractionTrigger.RIGHT_CLICK;
        }

        // 4. Match Item
        Optional<Key> matchItem = Optional.empty();
        Object rawMatchItem = root.get("match_item");
        if (rawMatchItem == null) {
            rawMatchItem = root.get("item");
        }
        if (rawMatchItem != null && !rawMatchItem.toString().isBlank()) {
            String s = rawMatchItem.toString().trim();
            matchItem = Optional.of(s.contains(":") ? Key.parse(s) : Key.of("croppy", s));
        }

        // 5. Match Material
        Optional<String> matchMaterial = Optional.empty();
        Object rawMaterial = root.get("match_material");
        if (rawMaterial == null) {
            rawMaterial = root.get("material");
        }
        if (rawMaterial != null && !rawMaterial.toString().isBlank()) {
            String s = rawMaterial.toString().trim().toLowerCase(Locale.ROOT);
            if (!s.contains(":")) {
                s = "minecraft:" + s;
            }
            matchMaterial = Optional.of(s);
        }

        // 6. Require Empty Hand
        boolean requireEmptyHand = getBoolean(root, "require_empty_hand", getBoolean(root, "empty_hand", false));

        // 7. Priority
        int priority = getInt(root, "priority", 0);

        // 8. Conditions
        List<String> conditions = new ArrayList<>();
        Object rawConditions = root.get("conditions");
        if (rawConditions instanceof List<?> cList) {
            for (Object c : cList) {
                if (c != null && !c.toString().isBlank()) {
                    conditions.add(c.toString().trim());
                }
            }
        }

        // 9. Actions
        List<InteractionAction> actions = new ArrayList<>();
        Object rawActions = root.get("actions");
        if (rawActions instanceof List<?> aList) {
            for (Object a : aList) {
                if (a instanceof String aStr && !aStr.isBlank()) {
                    actions.add(InteractionAction.of(aStr.trim().toUpperCase(Locale.ROOT)));
                } else if (a instanceof Map<?, ?> aMap) {
                    Object rawType = aMap.get("type");
                    if (rawType != null && !rawType.toString().isBlank()) {
                        String type = rawType.toString().trim().toUpperCase(Locale.ROOT);
                        Map<String, Object> params = new HashMap<>();
                        for (Map.Entry<?, ?> entry : aMap.entrySet()) {
                            if (entry.getKey() != null && !"type".equalsIgnoreCase(entry.getKey().toString())) {
                                params.put(entry.getKey().toString(), entry.getValue());
                            }
                        }
                        actions.add(InteractionAction.of(type, params));
                    }
                }
            }
        }

        return new InteractionDefinition(
                key,
                targetStation,
                trigger,
                matchItem,
                matchMaterial,
                requireEmptyHand,
                conditions,
                actions,
                priority
        );
    }

    /**
     * Loads an interaction definition directly from a file path.
     *
     * @param path the file path
     * @return parsed definition
     * @throws IOException if an I/O error occurs
     */
    public InteractionDefinition load(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        try (InputStream in = Files.newInputStream(path)) {
            return load(in, path.getFileName().toString());
        }
    }

    /**
     * Recursively loads all interaction YAML files from a directory into the given registry.
     *
     * @param directory directory to search
     * @param registry  registry to populate
     * @param logger    logger for status messages
     * @return count of loaded interactions
     */
    public int loadAll(Path directory, InteractionRegistry registry, Logger logger) {
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
                    InteractionDefinition def = load(file);
                    registry.register(def);
                    loadedCount++;
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.log(Level.SEVERE, "Failed to load interaction definition from " + file, ex);
                    }
                }
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed to scan interaction directory " + directory, e);
            }
        }

        return loadedCount;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        } else if (val != null) {
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean b) {
            return b;
        } else if (val != null) {
            return Boolean.parseBoolean(val.toString().trim());
        }
        return defaultValue;
    }
}
