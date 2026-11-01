package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses hierarchical YAML localization files using SnakeYAML and flattens
 * nested keys into dot-delimited mappings.
 */
public class YamlLocaleLoader {

    private final Yaml yaml;

    public YamlLocaleLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Loads a single YAML stream and flattens its contents into dot-delimited key-value entries.
     *
     * @param in         input stream
     * @param sourceName name of the source (for diagnostics)
     * @return unmodifiable map of dot-delimited keys to template strings
     * @throws IllegalArgumentException if the YAML root is not a mapping
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "Input stream cannot be null");
        Object loaded = yaml.load(in);
        if (loaded == null) {
            return Collections.emptyMap();
        }
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, String> flattened = new HashMap<>();
        flattenMap("", (Map<String, Object>) rawMap, flattened);
        return Collections.unmodifiableMap(flattened);
    }

    /**
     * Loads a single YAML file from disk.
     *
     * @param file path to YAML file
     * @return unmodifiable flattened map
     * @throws IOException if reading the file fails
     */
    public Map<String, String> loadFile(Path file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.getFileName().toString());
        }
    }

    /**
     * Loads all YAML files from the given directory into a map of LocaleRef to key-value mappings.
     *
     * @param directory directory containing locale YAML files
     * @param logger    logger for diagnostics
     * @return map of loaded locales
     */
    public Map<LocaleRef, Map<String, String>> loadDirectory(Path directory, Logger logger) {
        Objects.requireNonNull(directory, "directory cannot be null");
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Collections.emptyMap();
        }

        Map<LocaleRef, Map<String, String>> result = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.{yml,yaml}")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                try {
                    LocaleRef localeRef = LocaleRef.parse(baseName);
                    Map<String, String> messages = loadFile(path);
                    result.put(localeRef, messages);
                    if (logger != null) {
                        logger.info("Loaded " + messages.size() + " messages for locale [" + localeRef + "] from " + fileName);
                    }
                } catch (Exception e) {
                    if (logger != null) {
                        logger.log(Level.WARNING, "Failed to load locale file: " + fileName, e);
                    }
                }
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed to read locale directory: " + directory, e);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private void flattenMap(String prefix, Map<String, Object> map, Map<String, String> output) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String fullKey = prefix.isEmpty() ? key : (prefix + "." + key);
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> subMap) {
                flattenMap(fullKey, (Map<String, Object>) subMap, output);
            } else if (value instanceof List<?> list) {
                // Multi-line values (such as lore lines) joined with newline
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append("\n");
                    }
                    sb.append(list.get(i) != null ? list.get(i).toString() : "");
                }
                output.put(fullKey, sb.toString());
            } else if (value != null) {
                output.put(fullKey, value.toString());
            }
        }
    }
}
