package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.process.PhaseEffects;
import dev.darkblade.croppy.api.process.PhaseParticle;
import dev.darkblade.croppy.api.process.PhaseSound;
import dev.darkblade.croppy.api.species.RotationMode;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.station.StationStateDefinition;
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayLayerConfig;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import dev.darkblade.croppy.api.station.visual.StationInteractionBox;
import dev.darkblade.croppy.api.station.visual.StationModelOverride;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import dev.darkblade.croppy.api.station.visual.TextDisplayTemplate;
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

        // 10. Visual Definition
        StationVisualDefinition visualDefinition = parseVisual(root);

        // 11. Rotation Mode
        String rotStr = getString(root, "rotation_mode", getString(root, "rotation-mode", getString(root, "rotation", null)));
        RotationMode rotationMode = RotationMode.CARDINAL_4;
        if (rotStr != null && !rotStr.isBlank()) {
            try {
                rotationMode = RotationMode.valueOf(rotStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                rotationMode = RotationMode.CARDINAL_4;
            }
        }

        // 12. States
        DisplayTransform baseTransform = (visualDefinition != null)
                ? visualDefinition.getBaseModel().map(DisplayLayerConfig::transform).orElse(DisplayTransform.DEFAULT)
                : DisplayTransform.DEFAULT;
        StationInteractionBox baseHitbox = (visualDefinition != null)
                ? visualDefinition.getInteractionBox().orElse(null)
                : null;
        List<StationStateDefinition> states = parseStates(root, baseTransform, baseHitbox);

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
                properties,
                visualDefinition,
                rotationMode,
                states
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
     * @param logger    logger for status messages (may be null)
     * @return count of loaded stations
     */
    public int loadAll(Path directory, StationRegistry registry, Logger logger) {
        return loadAll(directory, registry, logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE);
    }

    /**
     * Recursively loads all station YAML files from a directory into the given registry.
     *
     * @param directory directory to search
     * @param registry  registry to populate
     * @param logger    logger for status messages (may be null)
     * @return count of loaded stations
     */
    public int loadAll(Path directory, StationRegistry registry, CroppyLogger logger) {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        CroppyLogger log = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
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
                    log.debug(LogChannel.LOADER, () -> "Loaded station definition: " + def.getKey() + " from " + file.getFileName());
                } catch (Exception ex) {
                    log.error(LogChannel.LOADER, "Failed to load station definition from " + file, ex);
                }
            }
        } catch (IOException ex) {
            log.error(LogChannel.LOADER, "Error reading station directory " + directory, ex);
        }

        return loadedCount;
    }

    @SuppressWarnings("unchecked")
    private StationVisualDefinition parseVisual(Map<String, Object> root) {
        Object rawVisual = root.get("visual");
        if (!(rawVisual instanceof Map<?, ?> vMap)) {
            return null;
        }
        Map<String, Object> visualMap = (Map<String, Object>) vMap;

        DisplayLayerConfig baseModel = null;
        Object rawBase = visualMap.get("base_model");
        if (rawBase instanceof Map<?, ?> bMap) {
            baseModel = parseDisplayLayer((Map<String, Object>) bMap, 48);
        }

        DisplayLayerConfig processDisplay = null;
        Object rawProc = visualMap.get("process_display");
        if (rawProc instanceof Map<?, ?> pMap) {
            processDisplay = parseDisplayLayer((Map<String, Object>) pMap, 24);
        }

        TextDisplayTemplate textDisplay = null;
        Object rawText = visualMap.get("text_display");
        if (rawText instanceof Map<?, ?> tMap) {
            textDisplay = parseTextDisplay((Map<String, Object>) tMap);
        }

        boolean hideBaseBlock = getBoolean(visualMap, "hide_base_block", false)
                || getBoolean(visualMap, "hide-base-block", false);

        StationInteractionBox interactionBox = null;
        Object rawInteraction = visualMap.get("interaction");
        if (rawInteraction == null) {
            rawInteraction = visualMap.get("interaction_box");
        }
        if (rawInteraction instanceof Map<?, ?> iMap) {
            Map<String, Object> interactionMap = (Map<String, Object>) iMap;
            double width = getDouble(interactionMap, "width", 0.8);
            double height = getDouble(interactionMap, "height", 0.8);
            interactionBox = new StationInteractionBox((float) width, (float) height);
        }

        return new StationVisualDefinition(baseModel, processDisplay, textDisplay, hideBaseBlock, interactionBox);
    }

    private DisplayLayerConfig parseDisplayLayer(Map<String, Object> map, int defaultViewDistance) {
        if (map == null) {
            return null;
        }
        boolean enabled = getBoolean(map, "enabled", true);
        String type = getString(map, "type", "ITEM");

        Key itemKey = null;
        Object rawItem = map.get("item");
        if (rawItem != null && !rawItem.toString().isBlank()) {
            String itemStr = rawItem.toString().trim();
            itemKey = itemStr.contains(":") ? Key.parse(itemStr) : Key.of("croppy", itemStr);
        }

        String blockState = getString(map, "block_state", getString(map, "block", null));
        DisplayTransform transform = parseTransform(map);
        BillboardMode billboard = parseBillboard(map, "billboard", BillboardMode.FIXED);
        int viewDistance = getInt(map, "view_distance", defaultViewDistance);
        int interpolationDuration = getInt(map, "interpolation_duration", 0);

        return new DisplayLayerConfig(enabled, type, itemKey, blockState, transform, billboard, viewDistance, interpolationDuration);
    }

    private TextDisplayTemplate parseTextDisplay(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        boolean enabled = getBoolean(map, "enabled", true);
        String format = getString(map, "format", TextDisplayTemplate.DEFAULT_FORMAT);
        DisplayTransform transform = parseTransform(map);
        BillboardMode billboard = parseBillboard(map, "billboard", BillboardMode.CENTER);
        int viewDistance = getInt(map, "view_distance", 8);
        int lineWidth = getInt(map, "line_width", 200);
        int backgroundColor = getInt(map, "background_color", 0);
        boolean seeThrough = getBoolean(map, "see_through", false);
        boolean textShadow = getBoolean(map, "text_shadow", true);
        boolean hideWhenIdle = getBoolean(map, "hide_when_idle", false);

        return new TextDisplayTemplate(enabled, format, transform, billboard, viewDistance, lineWidth, backgroundColor, seeThrough, textShadow, hideWhenIdle);
    }

    private DisplayTransform parseTransform(Map<String, Object> map) {
        if (map == null) {
            return DisplayTransform.DEFAULT;
        }
        double tx = 0.0, ty = 0.0, tz = 0.0;
        float sx = 1.0f, sy = 1.0f, sz = 1.0f;
        float pitch = 0.0f, yaw = 0.0f, roll = 0.0f;

        Object transObj = map.get("translation");
        if (transObj instanceof List<?> list && list.size() >= 3) {
            tx = parseDouble(list.get(0), 0.0);
            ty = parseDouble(list.get(1), 0.0);
            tz = parseDouble(list.get(2), 0.0);
        } else {
            tx = parseDouble(map.get("translation_x"), parseDouble(map.get("tx"), 0.0));
            ty = parseDouble(map.get("translation_y"), parseDouble(map.get("ty"), 0.0));
            tz = parseDouble(map.get("translation_z"), parseDouble(map.get("tz"), 0.0));
        }

        Object scaleObj = map.get("scale");
        if (scaleObj instanceof List<?> list && list.size() >= 3) {
            sx = (float) parseDouble(list.get(0), 1.0);
            sy = (float) parseDouble(list.get(1), 1.0);
            sz = (float) parseDouble(list.get(2), 1.0);
        } else if (scaleObj instanceof Number n) {
            sx = sy = sz = n.floatValue();
        } else {
            sx = (float) parseDouble(map.get("scale_x"), 1.0);
            sy = (float) parseDouble(map.get("scale_y"), 1.0);
            sz = (float) parseDouble(map.get("scale_z"), 1.0);
        }

        Object rotObj = map.get("rotation");
        if (rotObj instanceof List<?> list && list.size() >= 3) {
            pitch = (float) Math.toRadians(parseDouble(list.get(0), 0.0));
            yaw = (float) Math.toRadians(parseDouble(list.get(1), 0.0));
            roll = (float) Math.toRadians(parseDouble(list.get(2), 0.0));
        } else {
            if (map.containsKey("rotation_pitch")) {
                pitch = (float) Math.toRadians(parseDouble(map.get("rotation_pitch"), 0.0));
            }
            if (map.containsKey("rotation_yaw")) {
                yaw = (float) Math.toRadians(parseDouble(map.get("rotation_yaw"), 0.0));
            }
            if (map.containsKey("rotation_roll")) {
                roll = (float) Math.toRadians(parseDouble(map.get("rotation_roll"), 0.0));
            }
        }

        return new DisplayTransform(tx, ty, tz, pitch, yaw, roll, sx, sy, sz);
    }

    private BillboardMode parseBillboard(Map<String, Object> map, String key, BillboardMode def) {
        Object val = map.get(key);
        if (val != null) {
            try {
                return BillboardMode.valueOf(val.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }
        return def;
    }

    private double getDouble(Map<String, Object> map, String key, double def) {
        return parseDouble(map.get(key), def);
    }

    private double parseDouble(Object obj, double def) {
        if (obj instanceof Number n) {
            return n.doubleValue();
        }
        if (obj != null) {
            try {
                return Double.parseDouble(obj.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
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

    List<StationStateDefinition> parseStates(Map<String, Object> root) {
        return parseStates(root, null, null);
    }

    @SuppressWarnings("unchecked")
    List<StationStateDefinition> parseStates(Map<String, Object> root, DisplayTransform baseTransform, StationInteractionBox baseHitbox) {
        Object rawStates = root.get("states");
        if (!(rawStates instanceof List<?> list)) {
            return List.of();
        }

        List<StationStateDefinition> states = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> sMap = (Map<String, Object>) rawMap;
            String id = getString(sMap, "id", null);
            if (id == null || id.isBlank()) {
                continue;
            }

            int priority = getInt(sMap, "priority", 0);
            String condition = getString(sMap, "when", getString(sMap, "condition", null));

            StationModelOverride model = null;
            Object rawModel = sMap.get("model");
            if (rawModel instanceof Map<?, ?> mMap) {
                Map<String, Object> modelMap = (Map<String, Object>) mMap;
                Key itemModel = null;
                Object rawItemModel = modelMap.get("item_model");
                if (rawItemModel == null) {
                    rawItemModel = modelMap.get("model");
                }
                if (rawItemModel != null && !rawItemModel.toString().isBlank()) {
                    String imStr = rawItemModel.toString().trim();
                    itemModel = imStr.contains(":") ? Key.parse(imStr) : Key.of("croppy", imStr);
                }
                int customModelData = getInt(modelMap, "custom_model_data", getInt(modelMap, "cmd", 0));
                DisplayTransform transform = parseTransformOverride(modelMap, baseTransform);
                if (itemModel != null || customModelData > 0 || transform != null) {
                    model = new StationModelOverride(itemModel, customModelData, transform);
                }
            }

            StationInteractionBox hitbox = parseHitboxOverride(
                    sMap.containsKey("hitbox") ? sMap.get("hitbox") : (sMap.containsKey("interaction") ? sMap.get("interaction") : sMap.get("interaction_box")),
                    baseHitbox
            );

            PhaseEffects effects = parseEffects(sMap.get("effects"));

            states.add(new StationStateDefinition(id, priority, condition, model, hitbox, effects));
        }

        return states;
    }

    private DisplayTransform parseTransformOverride(Map<String, Object> map, DisplayTransform base) {
        if (map == null) {
            return null;
        }

        boolean hasTranslation = map.containsKey("translation") || map.containsKey("translation_x")
                || map.containsKey("translation_y") || map.containsKey("translation_z")
                || map.containsKey("tx") || map.containsKey("ty") || map.containsKey("tz");

        boolean hasScale = map.containsKey("scale") || map.containsKey("scale_x")
                || map.containsKey("scale_y") || map.containsKey("scale_z");

        boolean hasRotation = map.containsKey("rotation") || map.containsKey("rotation_pitch")
                || map.containsKey("rotation_yaw") || map.containsKey("rotation_roll")
                || map.containsKey("pitch") || map.containsKey("yaw") || map.containsKey("roll");

        if (!hasTranslation && !hasScale && !hasRotation) {
            return null;
        }

        DisplayTransform fallback = (base != null) ? base : DisplayTransform.DEFAULT;

        double tx = fallback.translationX();
        double ty = fallback.translationY();
        double tz = fallback.translationZ();

        if (hasTranslation) {
            Object transObj = map.get("translation");
            if (transObj instanceof List<?> list && list.size() >= 3) {
                tx = parseDouble(list.get(0), tx);
                ty = parseDouble(list.get(1), ty);
                tz = parseDouble(list.get(2), tz);
            } else {
                tx = parseDouble(map.get("translation_x"), parseDouble(map.get("tx"), tx));
                ty = parseDouble(map.get("translation_y"), parseDouble(map.get("ty"), ty));
                tz = parseDouble(map.get("translation_z"), parseDouble(map.get("tz"), tz));
            }
        }

        float sx = fallback.scaleX();
        float sy = fallback.scaleY();
        float sz = fallback.scaleZ();

        if (hasScale) {
            Object scaleObj = map.get("scale");
            if (scaleObj instanceof List<?> list && list.size() >= 3) {
                sx = (float) parseDouble(list.get(0), sx);
                sy = (float) parseDouble(list.get(1), sy);
                sz = (float) parseDouble(list.get(2), sz);
            } else if (scaleObj instanceof Number n) {
                sx = sy = sz = n.floatValue();
            } else {
                sx = (float) parseDouble(map.get("scale_x"), sx);
                sy = (float) parseDouble(map.get("scale_y"), sy);
                sz = (float) parseDouble(map.get("scale_z"), sz);
            }
        }

        float pitch = fallback.pitch();
        float yaw = fallback.yaw();
        float roll = fallback.roll();

        if (hasRotation) {
            Object rotObj = map.get("rotation");
            if (rotObj instanceof List<?> list && list.size() >= 3) {
                pitch = (float) Math.toRadians(parseDouble(list.get(0), Math.toDegrees(pitch)));
                yaw = (float) Math.toRadians(parseDouble(list.get(1), Math.toDegrees(yaw)));
                roll = (float) Math.toRadians(parseDouble(list.get(2), Math.toDegrees(roll)));
            } else {
                if (map.containsKey("rotation_pitch")) {
                    pitch = (float) Math.toRadians(parseDouble(map.get("rotation_pitch"), Math.toDegrees(pitch)));
                } else if (map.containsKey("pitch")) {
                    pitch = (float) Math.toRadians(parseDouble(map.get("pitch"), Math.toDegrees(pitch)));
                }
                if (map.containsKey("rotation_yaw")) {
                    yaw = (float) Math.toRadians(parseDouble(map.get("rotation_yaw"), Math.toDegrees(yaw)));
                } else if (map.containsKey("yaw")) {
                    yaw = (float) Math.toRadians(parseDouble(map.get("yaw"), Math.toDegrees(yaw)));
                }
                if (map.containsKey("rotation_roll")) {
                    roll = (float) Math.toRadians(parseDouble(map.get("rotation_roll"), Math.toDegrees(roll)));
                } else if (map.containsKey("roll")) {
                    roll = (float) Math.toRadians(parseDouble(map.get("roll"), Math.toDegrees(roll)));
                }
            }
        }

        return new DisplayTransform(tx, ty, tz, pitch, yaw, roll, sx, sy, sz);
    }

    private StationInteractionBox parseHitboxOverride(Object rawHitbox, StationInteractionBox baseBox) {
        if (rawHitbox == null) {
            return null;
        }

        float defaultWidth = (baseBox != null) ? baseBox.width() : 0.8f;
        float defaultHeight = (baseBox != null) ? baseBox.height() : 0.8f;

        if (rawHitbox instanceof Map<?, ?> hMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hitboxMap = (Map<String, Object>) hMap;
            double width = getDouble(hitboxMap, "width", defaultWidth);
            double height = getDouble(hitboxMap, "height", defaultHeight);
            return new StationInteractionBox((float) width, (float) height);
        } else if (rawHitbox instanceof List<?> list && list.size() >= 2) {
            double width = parseDouble(list.get(0), defaultWidth);
            double height = parseDouble(list.get(1), defaultHeight);
            return new StationInteractionBox((float) width, (float) height);
        } else if (rawHitbox instanceof Number n) {
            float size = n.floatValue();
            return new StationInteractionBox(size, size);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private PhaseEffects parseEffects(Object effectsObj) {
        if (!(effectsObj instanceof Map<?, ?> rawEffects)) {
            return PhaseEffects.EMPTY;
        }

        Map<String, Object> effectsMap = (Map<String, Object>) rawEffects;
        PhaseParticle particle = null;
        PhaseSound sound = null;

        Object partObj = effectsMap.get("particle");
        if (partObj == null) {
            partObj = effectsMap.get("particles");
        }
        if (partObj instanceof Map<?, ?> rawPart) {
            Map<String, Object> partMap = (Map<String, Object>) rawPart;
            String type = getString(partMap, "type", getString(partMap, "particle", null));
            if (type != null && !type.isBlank()) {
                int count = getInt(partMap, "count", 2);
                double ox = 0.15, oy = 0.05, oz = 0.15, speed = 0.02;
                Object offsetObj = partMap.get("offset");
                if (offsetObj instanceof List<?> list && list.size() >= 3) {
                    ox = parseDouble(list.get(0), 0.15);
                    oy = parseDouble(list.get(1), 0.05);
                    oz = parseDouble(list.get(2), 0.15);
                }
                speed = parseDouble(partMap.get("speed"), 0.02);
                particle = new PhaseParticle(type, count, ox, oy, oz, speed);
            }
        }

        Object soundObj = effectsMap.get("sound");
        if (soundObj instanceof Map<?, ?> rawSound) {
            Map<String, Object> soundMap = (Map<String, Object>) rawSound;
            String soundName = getString(soundMap, "sound", getString(soundMap, "name", null));
            if (soundName != null && !soundName.isBlank()) {
                int interval = getInt(soundMap, "interval_ticks", getInt(soundMap, "interval", 40));
                float volume = (float) parseDouble(soundMap.get("volume"), 1.0);
                float pitch = (float) parseDouble(soundMap.get("pitch"), 1.0);
                sound = new PhaseSound(soundName, interval, volume, pitch);
            }
        }

        return new PhaseEffects(particle, sound);
    }
}
