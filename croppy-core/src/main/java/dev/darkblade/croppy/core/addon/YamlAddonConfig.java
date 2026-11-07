package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pure Java 21 implementation of {@link AddonConfig} powered by SnakeYAML 2.2.
 * Fully decoupled from Paper and Bukkit APIs, supporting headless testing and offline execution.
 */
public class YamlAddonConfig implements AddonConfig {

    private static final Logger LOGGER = Logger.getLogger(YamlAddonConfig.class.getName());

    private final Path filePath;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Yaml yaml;

    public YamlAddonConfig(Path filePath) {
        this.filePath = filePath;
        this.yaml = createYaml();
        if (filePath != null && Files.exists(filePath)) {
            reload();
        }
    }

    public YamlAddonConfig(Path filePath, Map<String, Object> initialData) {
        this.filePath = filePath;
        this.yaml = createYaml();
        if (initialData != null) {
            this.data.putAll(deepCopy(initialData));
        }
    }

    private static Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(2);
        Representer representer = new Representer(dumperOptions);
        return new Yaml(new SafeConstructor(loaderOptions), representer, dumperOptions, loaderOptions);
    }

    public static YamlAddonConfig load(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        YamlAddonConfig config = new YamlAddonConfig(filePath);
        config.reload();
        return config;
    }

    public static YamlAddonConfig load(InputStream in) {
        Objects.requireNonNull(in, "in cannot be null");
        Yaml yaml = createYaml();
        Object loaded = yaml.load(in);
        Map<String, Object> initial = new LinkedHashMap<>();
        if (loaded instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    initial.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return new YamlAddonConfig(null, initial);
    }

    public static YamlAddonConfig empty() {
        return new YamlAddonConfig(null, Collections.emptyMap());
    }

    public Path getFilePath() {
        return filePath;
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    @Override
    public String getString(String path) {
        return getString(path, null);
    }

    @Override
    public String getString(String path, String def) {
        Object val = get(path);
        if (val == null) return def;
        return String.valueOf(val);
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    @Override
    public long getLong(String path, long def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.longValue();
        }
        if (val instanceof String str) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    @Override
    public double getDouble(String path, double def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String str) {
            String trimmed = str.trim().toLowerCase();
            if ("true".equals(trimmed) || "yes".equals(trimmed) || "1".equals(trimmed)) return true;
            if ("false".equals(trimmed) || "no".equals(trimmed) || "0".equals(trimmed)) return false;
        }
        return def;
    }

    @Override
    public List<String> getStringList(String path) {
        Object val = get(path);
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return Collections.unmodifiableList(result);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null;
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys("", data, keys, deep);
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Optional<AddonConfig> getSection(String path) {
        Object val = get(path);
        if (val instanceof Map<?, ?> map) {
            Map<String, Object> sectionMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    sectionMap.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return Optional.of(new YamlAddonConfig(null, sectionMap));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void set(String path, Object value) {
        Objects.requireNonNull(path, "path cannot be null");
        String[] parts = path.split("\\.");
        if (parts.length == 1) {
            if (value == null) {
                data.remove(parts[0]);
            } else {
                data.put(parts[0], value);
            }
            return;
        }

        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object child = current.get(part);
            if (!(child instanceof Map)) {
                Map<String, Object> next = new ConcurrentHashMap<>();
                current.put(part, next);
                current = next;
            } else {
                current = (Map<String, Object>) child;
            }
        }

        String last = parts[parts.length - 1];
        if (value == null) {
            current.remove(last);
        } else {
            current.put(last, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Object get(String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private void collectKeys(String prefix, Map<String, Object> map, Set<String> out, boolean deep) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            out.add(fullKey);
            if (deep && entry.getValue() instanceof Map<?, ?> childMap) {
                collectKeys(fullKey, (Map<String, Object>) childMap, out, true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopy(Map<String, Object> original) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) nested));
            } else if (entry.getValue() instanceof List<?> list) {
                copy.put(entry.getKey(), new ArrayList<>(list));
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    @Override
    public synchronized void reload() {
        if (filePath == null) return;
        if (!Files.exists(filePath)) {
            data.clear();
            return;
        }

        try (InputStream in = Files.newInputStream(filePath)) {
            Object loaded = yaml.load(in);
            data.clear();
            if (loaded instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        data.put(entry.getKey().toString(), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reload configuration from " + filePath + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void save() {
        if (filePath == null) return;
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                yaml.dump(data, writer);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save configuration to " + filePath + ": " + e.getMessage(), e);
        }
    }
}
