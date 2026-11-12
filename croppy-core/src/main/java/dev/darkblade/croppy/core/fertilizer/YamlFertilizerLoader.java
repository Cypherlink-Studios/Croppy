package dev.darkblade.croppy.core.fertilizer;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and strictly validates YAML definitions for soil fertilizers.
 */
public class YamlFertilizerLoader {

    private final Yaml yaml;

    public YamlFertilizerLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a fertilizer definition from an input stream.
     *
     * @param in input stream containing YAML
     * @param sourceName name of the source (for error reporting)
     * @return the parsed Fertilizer instance
     * @throws IllegalArgumentException if the YAML is invalid or violates constraints
     */
    @SuppressWarnings("unchecked")
    public Fertilizer load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "Input stream cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. Key and Name
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'id' field in " + sourceName);
        }
        Key key = Key.parse(rawId.toString().trim());

        Object rawName = root.get("name");
        if (rawName == null || rawName.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'name' field in " + sourceName);
        }
        String name = rawName.toString();

        // 2. Description
        List<String> description = new ArrayList<>();
        Object rawDesc = root.get("description");
        if (rawDesc instanceof List<?> list) {
            for (Object line : list) {
                if (line != null) {
                    description.add(line.toString());
                }
            }
        }

        // 3. Type
        Object rawType = root.get("type");
        if (rawType == null || rawType.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'type' field in " + sourceName);
        }
        FertilizerType type;
        try {
            type = FertilizerType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown fertilizer type '" + rawType + "' in " + sourceName, e);
        }

        // 4. Metrics
        double speedMultiplier = getDouble(root, "speed_multiplier", getDouble(root, "multiplier", 1.0));
        double retentionChance = getDouble(root, "retention_chance", 0.0);
        double qualityBoost = getDouble(root, "quality_boost", 0.0);

        // 5. Item representation
        String material = "BONE_MEAL";
        int customModelData = 0;
        Object rawItem = root.get("item");
        Map<?, ?> itemMap = (rawItem instanceof Map<?, ?> map) ? map : null;

        Object rawMat = (itemMap != null) ? itemMap.get("material") : root.get("material");
        if (rawMat != null && !rawMat.toString().isBlank()) {
            material = rawMat.toString().trim().toUpperCase(Locale.ROOT);
        }
        Object rawCmd = (itemMap != null) ? itemMap.get("custom_model_data") : root.get("custom_model_data");
        if (rawCmd == null && itemMap != null) {
            rawCmd = itemMap.get("custom-model-data");
        }
        if (rawCmd == null) {
            rawCmd = root.get("custom-model-data");
        }
        if (rawCmd instanceof Number num) {
            customModelData = num.intValue();
        }

        return new Fertilizer(key, name, description, type, speedMultiplier, retentionChance, qualityBoost, material, customModelData);
    }

    /**
     * Loads all .yml and .yaml files in the specified directory into the target registry.
     */
    public int loadDirectory(Path dir, Logger logger, FertilizerRegistry registry) {
        return loadDirectory(dir, logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE, registry);
    }

    /**
     * Loads all .yml and .yaml files in the specified directory into the target registry.
     */
    public int loadDirectory(Path dir, CroppyLogger logger, FertilizerRegistry registry) {
        Objects.requireNonNull(dir, "Directory path cannot be null");
        Objects.requireNonNull(registry, "Registry cannot be null");
        CroppyLogger log = logger != null ? logger : NoOpCroppyLogger.INSTANCE;

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return 0;
        }

        int loadedCount = 0;
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                    .toList();

            for (Path path : files) {
                try (InputStream in = Files.newInputStream(path)) {
                    Fertilizer fertilizer = load(in, path.getFileName().toString());
                    registry.register(fertilizer);
                    loadedCount++;
                    log.debug(LogChannel.LOADER, () -> "Loaded fertilizer: " + fertilizer.getKey().asString());
                } catch (Exception e) {
                    log.error(LogChannel.LOADER, "Failed to load fertilizer from " + path.getFileName(), e);
                }
            }
        } catch (IOException e) {
            log.error(LogChannel.LOADER, "Failed to scan fertilizer directory: " + dir, e);
        }

        return loadedCount;
    }

    private static double getDouble(Map<String, Object> map, String key, double fallback) {
        Object val = map.get(key);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        return fallback;
    }
}
