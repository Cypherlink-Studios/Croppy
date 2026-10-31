package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.rule.RuleSeverity;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;
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
 * Parses and loads YAML configuration files into {@link RuleDefinition} instances,
 * pre-compiling Pratt AST condition expressions for zero-allocation runtime evaluation.
 */
public final class YamlRuleLoader {

    private final Yaml yaml;

    public YamlRuleLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single rule definition from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic logging
     * @return the parsed rule definition
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    @SuppressWarnings("unchecked")
    public RuleDefinition load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. ID / Key
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'id' field in rule definition " + sourceName);
        }
        String idStr = rawId.toString().trim();
        Key key = idStr.contains(":") ? Key.parse(idStr) : Key.of("croppy", idStr);

        // 2. Name
        String name = getString(root, "name", key.value());

        // 3. Trigger
        String triggerStr = getString(root, "trigger", "STATION_TICK").toUpperCase(Locale.ROOT);
        RuleTrigger trigger;
        try {
            trigger = RuleTrigger.valueOf(triggerStr);
        } catch (IllegalArgumentException ex) {
            trigger = RuleTrigger.STATION_TICK;
        }

        // 4. Severity
        String severityStr = getString(root, "severity", "PENALTY").toUpperCase(Locale.ROOT);
        RuleSeverity severity;
        try {
            severity = RuleSeverity.valueOf(severityStr);
        } catch (IllegalArgumentException ex) {
            severity = RuleSeverity.PENALTY;
        }

        // 5. Priority
        int priority = getInt(root, "priority", 0);

        // 6. Conditions & Pre-compilation
        List<String> conditions = new ArrayList<>();
        Object rawConditions = root.get("conditions");
        if (rawConditions instanceof List<?> cList) {
            for (Object c : cList) {
                if (c != null && !c.toString().isBlank()) {
                    String condStr = c.toString().trim();
                    // Pre-compile and validate condition syntax fail-fast
                    try {
                        ExpressionCompiler.compile(condStr);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid condition expression '" + condStr + "' in " + sourceName, ex);
                    }
                    conditions.add(condStr);
                }
            }
        }

        // 7. Actions
        List<RuleAction> actions = new ArrayList<>();
        Object rawActions = root.get("actions");
        if (rawActions instanceof List<?> aList) {
            for (Object a : aList) {
                if (a instanceof String aStr && !aStr.isBlank()) {
                    actions.add(RuleAction.of(aStr.trim().toUpperCase(Locale.ROOT)));
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
                        actions.add(RuleAction.of(type, params));
                    }
                }
            }
        }

        return new RuleDefinition(
                key,
                name,
                trigger,
                severity,
                priority,
                conditions,
                actions
        );
    }

    /**
     * Loads a rule definition directly from a file path.
     *
     * @param path the file path
     * @return parsed definition
     * @throws IOException if an I/O error occurs
     */
    public RuleDefinition load(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        try (InputStream in = Files.newInputStream(path)) {
            return load(in, path.getFileName().toString());
        }
    }

    /**
     * Recursively loads all rule YAML files from a directory into the given registry.
     *
     * @param directory directory to search
     * @param registry  registry to populate
     * @param logger    logger for status messages
     * @return count of loaded rules
     */
    public int loadAll(Path directory, RuleRegistry registry, Logger logger) {
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
                    RuleDefinition def = load(file);
                    registry.register(def);
                    loadedCount++;
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.log(Level.SEVERE, "Failed to load rule definition from " + file, ex);
                    }
                }
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed to scan rule directory " + directory, e);
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
}
