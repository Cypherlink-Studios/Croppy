package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ConsumableProperties;
import dev.darkblade.croppy.api.item.ConsumeCooldown;
import dev.darkblade.croppy.api.item.ConsumeEffectSpec;
import dev.darkblade.croppy.api.item.FoodProperties;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.item.QualityScaling;
import dev.darkblade.croppy.api.item.UseAnimation;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;
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
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and validates YAML configuration files for {@link ItemDefinition}s.
 */
public class YamlItemLoader {

    private final Yaml yaml;
    private final CroppyLogger defaultLogger;

    public YamlItemLoader() {
        this(NoOpCroppyLogger.INSTANCE);
    }

    public YamlItemLoader(CroppyLogger defaultLogger) {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
        this.defaultLogger = defaultLogger != null ? defaultLogger : NoOpCroppyLogger.INSTANCE;
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
    public ItemDefinition load(InputStream in, String defaultCategory, String sourceName) {
        return load(in, defaultCategory, sourceName, defaultLogger);
    }

    @SuppressWarnings("unchecked")
    public ItemDefinition load(InputStream in, String defaultCategory, String sourceName, CroppyLogger logger) {
        CroppyLogger log = logger != null ? logger : defaultLogger;
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
                log.warn(LogChannel.LOADER,
                        "Invalid item-model key '" + trimmedModel + "' in " + sourceName + ": " + e.getMessage());
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

        // 8. Optional Consumable & Food Properties
        ConsumableProperties consumableProperties = parseConsumableProperties(root, itemMap, material, sourceName);

        return new ItemDefinition(
                key,
                category.toLowerCase(Locale.ROOT),
                material,
                customModelData,
                itemModel,
                texture,
                displayName,
                lore,
                pdcProperties,
                consumableProperties
        );
    }

    /**
     * Loads an ItemDefinition from a file on disk.
     */
    public ItemDefinition load(Path filePath, String defaultCategory) throws IOException {
        return load(filePath, defaultCategory, defaultLogger);
    }

    public ItemDefinition load(Path filePath, String defaultCategory, CroppyLogger logger) throws IOException {
        try (InputStream in = Files.newInputStream(filePath)) {
            return load(in, defaultCategory, filePath.getFileName().toString(), logger);
        }
    }

    /**
     * Loads all YAML item definitions in a directory for a specific category.
     */
    public int loadDirectory(Path dir, String category, Logger logger, ItemRegistry registry) {
        return loadDirectory(dir, category, logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE, registry);
    }

    public int loadDirectory(Path dir, String category, CroppyLogger logger, ItemRegistry registry) {
        CroppyLogger log = logger != null ? logger : defaultLogger;
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
                    ItemDefinition def = load(file, category, log);
                    registry.register(def);
                    count++;
                    log.debug(LogChannel.LOADER, () -> "Loaded item: " + def.getKey() + " from " + file.getFileName());
                } catch (Exception e) {
                    log.error(LogChannel.LOADER, "Failed to load item from " + file.getFileName() + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            log.error(LogChannel.LOADER, "Failed to read item directory: " + dir, e);
        }
        return count;
    }

    /**
     * Scans the root items directory, treating each sub-folder as a category.
     */
    public int loadAllCategories(Path itemsRootDir, Logger logger, ItemRegistry registry) {
        return loadAllCategories(itemsRootDir, logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE, registry);
    }

    /**
     * Scans the root items directory, treating each sub-folder as a category.
     */
    public int loadAllCategories(Path itemsRootDir, CroppyLogger logger, ItemRegistry registry) {
        CroppyLogger log = logger != null ? logger : defaultLogger;
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
                int loaded = loadDirectory(subDir, category, log, registry);
                totalLoaded += loaded;
                log.debug(LogChannel.LOADER, () -> "Loaded " + loaded + " items from category '" + category + "'.");
            }
        } catch (IOException e) {
            log.error(LogChannel.LOADER, "Failed to scan items directory: " + itemsRootDir, e);
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

    private double getDouble(Map<String, Object> map, String key, double def) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.doubleValue();
        } else if (val != null) {
            try {
                return Double.parseDouble(val.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean b) {
            return b;
        } else if (val != null) {
            String s = val.toString().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(s)) return true;
            if ("false".equals(s)) return false;
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findMap(Map<String, Object> root, Map<String, Object> itemMap, String... keys) {
        for (String key : keys) {
            Object raw = root.get(key);
            if (raw instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        }
        if (itemMap != null) {
            for (String key : keys) {
                Object raw = itemMap.get(key);
                if (raw instanceof Map<?, ?> map) {
                    return (Map<String, Object>) map;
                }
            }
        }
        return null;
    }

    private Object findRaw(Map<String, Object> root, Map<String, Object> itemMap, String... keys) {
        for (String key : keys) {
            if (root.containsKey(key)) {
                return root.get(key);
            }
        }
        if (itemMap != null) {
            for (String key : keys) {
                if (itemMap.containsKey(key)) {
                    return itemMap.get(key);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ConsumableProperties parseConsumableProperties(
            Map<String, Object> root,
            Map<String, Object> itemMap,
            String material,
            String sourceName
    ) {
        Map<String, Object> foodMap = findMap(root, itemMap, "food", "nutrition");
        Map<String, Object> consumableMap = findMap(root, itemMap, "consumable");
        Object rawEffects = findRaw(root, itemMap, "effects");

        if (foodMap == null && consumableMap == null && rawEffects == null) {
            return null;
        }

        FoodProperties food = null;
        if (foodMap != null) {
            int hunger = getInt(foodMap, "hunger", 0);
            float saturation = (float) getDouble(foodMap, "saturation", 0.0);
            boolean canAlwaysEat = getBoolean(foodMap, "can-always-eat",
                    getBoolean(foodMap, "can_always_eat", false));

            QualityScaling scaling = QualityScaling.NONE;
            Map<String, Object> scalingMap = findMap(foodMap, null, "quality-scaling", "quality_scaling");
            if (scalingMap != null) {
                double hScale = getDouble(scalingMap, "hunger-per-level",
                        getDouble(scalingMap, "hunger_per_level", 0.0));
                double sScale = getDouble(scalingMap, "saturation-per-level",
                        getDouble(scalingMap, "saturation_per_level", 0.0));
                double dScale = getDouble(scalingMap, "duration-per-level",
                        getDouble(scalingMap, "duration_per_level", 0.0));
                scaling = new QualityScaling(hScale, sScale, dScale);
            }
            food = new FoodProperties(hunger, saturation, canAlwaysEat, scaling);
        }

        float consumeSeconds = ConsumableProperties.DEFAULT_CONSUME_SECONDS;
        UseAnimation animation = defaultAnimationForMaterial(material);
        Key sound = null;
        boolean particles = true;
        Key remainder = null;
        ConsumeCooldown cooldown = null;

        if (consumableMap != null) {
            consumeSeconds = (float) getDouble(consumableMap, "consume-seconds",
                    getDouble(consumableMap, "consume_seconds", ConsumableProperties.DEFAULT_CONSUME_SECONDS));

            String rawAnim = getString(consumableMap, "animation", null);
            if (rawAnim != null && !rawAnim.isBlank()) {
                try {
                    animation = UseAnimation.valueOf(rawAnim.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid consumable animation '" + rawAnim + "' in " + sourceName);
                }
            }

            String rawSound = getString(consumableMap, "sound", null);
            if (rawSound != null && !rawSound.isBlank()) {
                sound = parseNamespacedKey(rawSound, "minecraft");
            }

            particles = getBoolean(consumableMap, "particles",
                    getBoolean(consumableMap, "has-consume-particles",
                            getBoolean(consumableMap, "has_consume_particles", true)));

            String rawRemainder = getString(consumableMap, "remainder",
                    getString(consumableMap, "use-remainder",
                            getString(consumableMap, "use_remainder", null)));
            if (rawRemainder != null && !rawRemainder.isBlank()) {
                remainder = parseNamespacedKey(rawRemainder, "croppy");
            }

            Map<String, Object> cooldownMap = findMap(consumableMap, null, "cooldown");
            if (cooldownMap != null) {
                float cdSeconds = (float) getDouble(cooldownMap, "seconds", 0.0);
                String rawGroup = getString(cooldownMap, "group", null);
                Key group = (rawGroup != null && !rawGroup.isBlank())
                        ? parseNamespacedKey(rawGroup, "croppy")
                        : null;
                cooldown = new ConsumeCooldown(cdSeconds, group);
            }
        }

        List<ConsumeEffectSpec> effects = new ArrayList<>();
        if (rawEffects instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> rawEffectMap)) {
                    throw new IllegalArgumentException("Effect entry in " + sourceName + " must be a mapping");
                }
                Map<String, Object> effectMap = (Map<String, Object>) rawEffectMap;
                String rawType = getString(effectMap, "type", null);
                if (rawType == null || rawType.isBlank()) {
                    throw new IllegalArgumentException("Effect entry missing 'type' in " + sourceName);
                }
                Key typeKey = parseNamespacedKey(rawType, "croppy");
                double chance = getDouble(effectMap, "chance", 1.0);
                int minQuality = getInt(effectMap, "min-quality", getInt(effectMap, "min_quality", 0));
                String condition = getString(effectMap, "condition", null);
                if (condition != null && !condition.isBlank()) {
                    ExpressionCompiler.compile(condition);
                }

                Map<String, Object> params = new HashMap<>();
                for (Map.Entry<String, Object> entry : effectMap.entrySet()) {
                    String k = entry.getKey();
                    if (k == null || entry.getValue() == null) continue;
                    if (k.equals("type") || k.equals("chance") || k.equals("min-quality")
                            || k.equals("min_quality") || k.equals("condition")) {
                        continue;
                    }
                    params.put(k, entry.getValue());
                }
                effects.add(new ConsumeEffectSpec(typeKey, chance, minQuality, condition, params));
            }
        }

        return new ConsumableProperties(food, consumeSeconds, animation, sound, particles, remainder, cooldown, effects);
    }

    private static UseAnimation defaultAnimationForMaterial(String material) {
        if (material == null) {
            return UseAnimation.EAT;
        }
        String norm = material.trim().toUpperCase(Locale.ROOT);
        if (norm.startsWith("MINECRAFT:")) {
            norm = norm.substring("MINECRAFT:".length());
        }
        if ("POTION".equals(norm) || "MILK_BUCKET".equals(norm) || "HONEY_BOTTLE".equals(norm)) {
            return UseAnimation.DRINK;
        }
        return UseAnimation.EAT;
    }

    private static Key parseNamespacedKey(String raw, String defaultNamespace) {
        String trimmed = raw.trim();
        return trimmed.contains(":") ? Key.parse(trimmed) : Key.of(defaultNamespace, trimmed.toLowerCase(Locale.ROOT));
    }
}
