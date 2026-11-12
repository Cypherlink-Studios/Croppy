package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.process.*;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;
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
 * Parses and loads YAML configuration files into {@link ProcessDefinition} models.
 */
public final class YamlProcessLoader {

    private final Yaml yaml;

    public YamlProcessLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single process definition from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic messages
     * @return the parsed process definition
     */
    @SuppressWarnings("unchecked")
    public ProcessDefinition load(InputStream in, String sourceName) {
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
        String displayName = getString(root, "name");
        if (displayName == null || displayName.isBlank()) {
            displayName = key.value();
        }

        // 3. Station Key (optional / wildcard fallback)
        String stationStr = getString(root, "station");
        Key stationKey = (stationStr != null && !stationStr.isBlank())
                ? Key.parse(stationStr)
                : Key.of("croppy", "default_station");

        // 4. Start conditions
        Expression startConditions = parseConditions(root.get("start_conditions"));

        // 5. Phases
        List<PhaseDefinition> phases = new ArrayList<>();
        if (root.get("phases") instanceof List<?> rawPhases) {
            for (Object phaseObj : rawPhases) {
                if (phaseObj instanceof Map<?, ?> phaseMap) {
                    phases.add(parsePhase((Map<String, Object>) phaseMap, sourceName));
                }
            }
        }

        if (phases.isEmpty()) {
            phases.add(PhaseDefinition.timed("default", 200, Expression.ALWAYS_TRUE));
        }

        // 6. Properties
        Map<String, String> properties = new HashMap<>();
        if (root.get("properties") instanceof Map<?, ?> rawProps) {
            for (Map.Entry<?, ?> entry : rawProps.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    properties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new ProcessDefinition(
                key,
                displayName,
                stationKey,
                List.of(),
                startConditions,
                phases,
                null,
                null,
                1,
                properties
        );
    }

    /**
     * Discovers and registers all process configuration files in a folder into the given registry.
     *
     * @param directory directory containing YAML process definitions
     * @param registry  target registry
     * @param logger    logger for diagnostics
     * @return total count of registered processes
     */
    public int loadAll(Path directory, ProcessRegistry registry, Logger logger) {
        Objects.requireNonNull(logger, "logger cannot be null");
        return loadAll(directory, registry, new JulCroppyLogger(logger));
    }

    public int loadAll(Path directory, ProcessRegistry registry, CroppyLogger logger) {
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
                    ProcessDefinition process = load(in, file.getFileName().toString());
                    registry.register(process);
                    loadedCount++;
                    logger.debug(LogChannel.LOADER, () -> "Loaded process definition: " + process.getKey() + " from " + file.getFileName());
                } catch (Exception e) {
                    logger.error(LogChannel.LOADER, "Failed to load process YAML definition from: " + file, e);
                }
            }
        } catch (IOException e) {
            logger.error(LogChannel.LOADER, "Failed to walk process definitions directory: " + directory, e);
        }

        return loadedCount;
    }

    @SuppressWarnings("unchecked")
    private PhaseDefinition parsePhase(Map<String, Object> map, String sourceName) {
        String id = Objects.requireNonNull(getString(map, "id"), "Phase in " + sourceName + " requires 'id'");

        // Type
        String typeStr = getString(map, "type");
        PhaseType type = PhaseType.TIMED;
        if (typeStr != null) {
            try {
                type = PhaseType.valueOf(typeStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid phase type '" + typeStr + "' in " + sourceName);
            }
        }

        // Durations & Timeouts
        long durationTicks = parseDurationTicks(map.get("duration"), type == PhaseType.TIMED ? 200 : 0);
        long timeoutTicks = parseDurationTicks(map.get("timeout"), 0);

        // Condition
        Expression condition = parseConditions(map.get("condition"));

        // Pause on condition failure
        boolean pauseOnCondition = true;
        if (map.containsKey("pause_on_condition_failure")) {
            pauseOnCondition = Boolean.parseBoolean(String.valueOf(map.get("pause_on_condition_failure")));
        } else if (map.containsKey("on_condition_pause")) {
            pauseOnCondition = Boolean.parseBoolean(String.valueOf(map.get("on_condition_pause")));
        }

        // Required item
        Key requiredItem = null;
        String reqItemStr = getString(map, "required_item");
        if (reqItemStr != null && !reqItemStr.isBlank()) {
            requiredItem = Key.parse(reqItemStr);
        }
        int requiredAmount = 1;
        if (map.get("required_amount") instanceof Number num) {
            requiredAmount = num.intValue();
        }

        // Failure policy & byproduct
        FailurePolicy failurePolicy = FailurePolicy.CANCEL;
        String policyStr = getString(map, "failure_policy");
        if (policyStr != null) {
            try {
                failurePolicy = FailurePolicy.valueOf(policyStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        } else if (type == PhaseType.MANUAL_INTERACTION) {
            failurePolicy = FailurePolicy.SPOIL;
        }

        Key byproduct = null;
        String byproductStr = getString(map, "byproduct");
        if (byproductStr != null && !byproductStr.isBlank()) {
            byproduct = Key.parse(byproductStr);
        }

        // Prompt message
        String prompt = getString(map, "prompt");

        // Display
        PhaseDisplay display = null;
        if (map.get("display") instanceof Map<?, ?> rawDisplay) {
            Map<String, Object> dispMap = (Map<String, Object>) rawDisplay;
            String template = getString(dispMap, "template");
            int barLength = 10;
            String barFilled = "<green>■</green>";
            String barEmpty = "<dark_gray>■</dark_gray>";

            if (dispMap.get("bar") instanceof Map<?, ?> rawBar) {
                Map<String, Object> barMap = (Map<String, Object>) rawBar;
                if (barMap.get("length") instanceof Number num) {
                    barLength = num.intValue();
                }
                String filled = getString(barMap, "filled");
                if (filled != null) barFilled = filled;
                String empty = getString(barMap, "empty");
                if (empty != null) barEmpty = empty;
            }

            if (template != null) {
                display = new PhaseDisplay(template, barLength, barFilled, barEmpty);
            }
        }

        // Effects
        PhaseEffects effects = parseEffects(map.get("effects"));

        // Properties
        Map<String, String> properties = new HashMap<>();
        String name = getString(map, "name");
        if (name != null) {
            properties.put("name", name);
        }
        if (map.get("properties") instanceof Map<?, ?> rawProps) {
            for (Map.Entry<?, ?> entry : rawProps.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    properties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new PhaseDefinition(
                id,
                type,
                durationTicks,
                timeoutTicks,
                condition,
                requiredItem,
                requiredAmount,
                failurePolicy,
                byproduct,
                prompt,
                display,
                effects,
                pauseOnCondition,
                properties
        );
    }

    @SuppressWarnings("unchecked")
    private PhaseEffects parseEffects(Object effectsObj) {
        if (!(effectsObj instanceof Map<?, ?> rawEffects)) {
            return PhaseEffects.EMPTY;
        }

        Map<String, Object> effectsMap = (Map<String, Object>) rawEffects;
        PhaseParticle particle = null;
        PhaseSound sound = null;

        if (effectsMap.get("particles") instanceof Map<?, ?> rawPart) {
            Map<String, Object> partMap = (Map<String, Object>) rawPart;
            String type = getString(partMap, "particle");
            if (type == null) type = getString(partMap, "type");
            if (type != null) {
                int count = 2;
                if (partMap.get("count") instanceof Number num) count = num.intValue();
                double ox = 0.15, oy = 0.05, oz = 0.15, speed = 0.02;
                if (partMap.get("offset") instanceof List<?> rawOffset && rawOffset.size() >= 3) {
                    ox = ((Number) rawOffset.get(0)).doubleValue();
                    oy = ((Number) rawOffset.get(1)).doubleValue();
                    oz = ((Number) rawOffset.get(2)).doubleValue();
                }
                if (partMap.get("speed") instanceof Number num) speed = num.doubleValue();
                particle = new PhaseParticle(type, count, ox, oy, oz, speed);
            }
        }

        if (effectsMap.get("sound") instanceof Map<?, ?> rawSound) {
            Map<String, Object> soundMap = (Map<String, Object>) rawSound;
            String soundName = getString(soundMap, "sound");
            if (soundName == null) soundName = getString(soundMap, "name");
            if (soundName != null) {
                int interval = (int) parseDurationTicks(soundMap.get("interval_ticks"), 40);
                if (soundMap.containsKey("interval")) {
                    interval = (int) parseDurationTicks(soundMap.get("interval"), 40);
                }
                float volume = 1.0f;
                float pitch = 1.0f;
                if (soundMap.get("volume") instanceof Number num) volume = num.floatValue();
                if (soundMap.get("pitch") instanceof Number num) pitch = num.floatValue();
                sound = new PhaseSound(soundName, interval, volume, pitch);
            }
        }

        return new PhaseEffects(particle, sound);
    }

    private Expression parseConditions(Object obj) {
        if (obj == null) {
            return Expression.ALWAYS_TRUE;
        }
        if (obj instanceof String str) {
            String trimmed = str.trim();
            return trimmed.isEmpty() ? Expression.ALWAYS_TRUE : ExpressionCompiler.compile(trimmed);
        }
        if (obj instanceof List<?> list) {
            List<String> validConds = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) {
                    validConds.add("(" + s.trim() + ")");
                }
            }
            if (validConds.isEmpty()) {
                return Expression.ALWAYS_TRUE;
            }
            return ExpressionCompiler.compile(String.join(" && ", validConds));
        }
        return Expression.ALWAYS_TRUE;
    }

    public static long parseDurationTicks(Object obj, long defaultTicks) {
        if (obj == null) {
            return defaultTicks;
        }
        if (obj instanceof Number num) {
            return num.longValue();
        }
        String str = obj.toString().trim().toLowerCase(Locale.ROOT);
        if (str.isEmpty()) {
            return defaultTicks;
        }
        try {
            if (str.endsWith("ms")) {
                double ms = Double.parseDouble(str.substring(0, str.length() - 2).trim());
                return Math.max(1, Math.round(ms / 50.0));
            }
            if (str.endsWith("s")) {
                double s = Double.parseDouble(str.substring(0, str.length() - 1).trim());
                return Math.max(1, Math.round(s * 20.0));
            }
            if (str.endsWith("m")) {
                double m = Double.parseDouble(str.substring(0, str.length() - 1).trim());
                return Math.max(1, Math.round(m * 60.0 * 20.0));
            }
            if (str.endsWith("t")) {
                return Long.parseLong(str.substring(0, str.length() - 1).trim());
            }
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultTicks;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val != null) ? val.toString().trim() : null;
    }
}
