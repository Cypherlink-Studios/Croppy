package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.logging.CroppyLogger;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Loads and validates addon metadata from an {@code addon.yml} manifest.
 */
public class YamlAddonDescriptionLoader {

    private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_.-]+$");

    private final Yaml yaml;
    private final CroppyLogger logger;

    public YamlAddonDescriptionLoader() {
        this(NoOpCroppyLogger.INSTANCE);
    }

    public YamlAddonDescriptionLoader(CroppyLogger logger) {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    /**
     * Loads an addon description from a path pointing to an addon.yml file.
     *
     * @param manifestPath path to addon.yml
     * @return the parsed AddonDescription
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public AddonDescription load(Path manifestPath) throws IOException {
        Objects.requireNonNull(manifestPath, "manifestPath cannot be null");
        try (InputStream in = Files.newInputStream(manifestPath)) {
            return load(in, manifestPath.toString());
        }
    }

    /**
     * Parses an addon description from an input stream.
     *
     * @param in the input stream
     * @param sourceName descriptive source name for error context
     * @return the parsed AddonDescription
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    @SuppressWarnings("unchecked")
    public AddonDescription load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. Mandatory id
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing mandatory 'id' in addon manifest: " + sourceName);
        }
        String id = rawId.toString().trim();
        if (!VALID_IDENTIFIER_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid addon 'id' '" + id + "' in " + sourceName
                    + ". Must match regex: ^[a-z0-9_.-]+$");
        }

        // 2. Mandatory name
        Object rawName = root.get("name");
        if (rawName == null || rawName.toString().isBlank()) {
            throw new IllegalArgumentException("Missing mandatory 'name' in addon manifest: " + sourceName);
        }
        String name = rawName.toString().trim();

        // 3. Mandatory version
        Object rawVersion = root.get("version");
        if (rawVersion == null || rawVersion.toString().isBlank()) {
            throw new IllegalArgumentException("Missing mandatory 'version' in addon manifest: " + sourceName);
        }
        String version = rawVersion.toString().trim();

        // 4. Mandatory namespace
        Object rawNamespace = root.get("namespace");
        if (rawNamespace == null || rawNamespace.toString().isBlank()) {
            throw new IllegalArgumentException("Missing mandatory 'namespace' in addon manifest: " + sourceName);
        }
        String namespace = rawNamespace.toString().trim();
        if (!VALID_IDENTIFIER_PATTERN.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid addon 'namespace' '" + namespace + "' in " + sourceName
                    + ". Must match regex: ^[a-z0-9_.-]+$");
        }

        // 5. Optional main class
        Optional<String> main = Optional.empty();
        Object rawMain = root.get("main");
        if (rawMain != null && !rawMain.toString().isBlank()) {
            main = Optional.of(rawMain.toString().trim());
        }

        // 6. Optional authors
        List<String> authors = new ArrayList<>();
        Object rawAuthors = root.get("authors");
        if (rawAuthors instanceof List<?> list) {
            for (Object author : list) {
                if (author != null && !author.toString().isBlank()) {
                    authors.add(author.toString().trim());
                }
            }
        } else if (rawAuthors instanceof String str && !str.isBlank()) {
            authors.add(str.trim());
        } else if (root.get("author") != null && !root.get("author").toString().isBlank()) {
            authors.add(root.get("author").toString().trim());
        }

        // 7. Optional description
        String description = "";
        Object rawDesc = root.get("description");
        if (rawDesc != null) {
            description = rawDesc.toString().trim();
        }

        // 8. Optional dependencies
        List<String> dependencies = parseStringList(root.get("dependencies"));

        // 9. Optional softDependencies
        List<String> softDependencies = parseStringList(root.get("softDependencies"));
        if (softDependencies.isEmpty()) {
            softDependencies = parseStringList(root.get("soft_dependencies"));
        }

        logger.debug(LogChannel.LOADER, () -> "Parsed addon manifest '" + id + "' v" + version + " from " + sourceName);
        return new AddonDescription(
                id,
                name,
                version,
                namespace,
                main,
                authors,
                description,
                dependencies,
                softDependencies
        );
    }

    private List<String> parseStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    result.add(item.toString().trim());
                }
            }
        } else if (obj instanceof String str && !str.isBlank()) {
            result.add(str.trim());
        }
        return result;
    }
}
