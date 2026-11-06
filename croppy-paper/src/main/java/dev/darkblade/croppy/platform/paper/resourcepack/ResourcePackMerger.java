package dev.darkblade.croppy.platform.paper.resourcepack;

import dev.darkblade.croppy.api.addon.AddonDescription;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers and merges assets from enabled addons into the server companion resource pack.
 */
public class ResourcePackMerger {

    private static final Pattern PATH_SEGMENT_PATTERN = Pattern.compile("^[a-z0-9_.-]+$");
    public static final String CORE_NAMESPACE = "croppy";

    private final Path basePackDirectory;
    private final Logger logger;

    public ResourcePackMerger(Path basePackDirectory, Logger logger) {
        this.basePackDirectory = Objects.requireNonNull(basePackDirectory, "basePackDirectory cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger(ResourcePackMerger.class.getName());
    }

    /**
     * Merges assets from the specified addon source into the companion resource pack.
     *
     * @param addon source addon description
     * @param addonPath directory or JAR file path of the addon
     * @return the number of asset files merged
     * @throws IOException if an I/O error occurs
     */
    public int mergeAddonAssets(AddonDescription addon, Path addonPath) throws IOException {
        Objects.requireNonNull(addon, "addon cannot be null");
        Objects.requireNonNull(addonPath, "addonPath cannot be null");

        if (!Files.exists(addonPath)) {
            return 0;
        }

        Path targetAssetsDir = basePackDirectory.resolve("assets");
        if (!Files.exists(targetAssetsDir)) {
            Files.createDirectories(targetAssetsDir);
        }

        if (Files.isDirectory(addonPath)) {
            return mergeFromDirectory(addon, addonPath.resolve("assets"), targetAssetsDir);
        } else if (addonPath.getFileName().toString().endsWith(".jar") || addonPath.getFileName().toString().endsWith(".zip")) {
            return mergeFromJar(addon, addonPath, targetAssetsDir);
        }
        return 0;
    }

    private int mergeFromDirectory(AddonDescription addon, Path sourceAssetsDir, Path targetAssetsDir) throws IOException {
        if (!Files.exists(sourceAssetsDir) || !Files.isDirectory(sourceAssetsDir)) {
            return 0;
        }

        int mergedCount = 0;
        try (Stream<Path> stream = Files.walk(sourceAssetsDir)) {
            for (Path sourceFile : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                Path relative = sourceAssetsDir.relativize(sourceFile);
                if (validateAndCopy(addon, relative, Files.newInputStream(sourceFile), targetAssetsDir)) {
                    mergedCount++;
                }
            }
        }
        return mergedCount;
    }

    private int mergeFromJar(AddonDescription addon, Path jarPath, Path targetAssetsDir) throws IOException {
        int mergedCount = 0;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith("assets/")) {
                    String relativePath = name.substring("assets/".length());
                    Path rel = Path.of(relativePath);
                    try (InputStream in = jar.getInputStream(entry)) {
                        if (validateAndCopy(addon, rel, in, targetAssetsDir)) {
                            mergedCount++;
                        }
                    }
                }
            }
        }
        return mergedCount;
    }

    private boolean validateAndCopy(AddonDescription addon, Path relativePath, InputStream in, Path targetAssetsDir) {
        try (in) {
            String[] segments = relativePath.toString().replace('\\', '/').split("/");
            if (segments.length == 0) return false;

            String namespace = segments[0];

            // Protect core namespace from unauthorized overrides unless the addon specifically claims it
            if (CORE_NAMESPACE.equalsIgnoreCase(namespace) && !CORE_NAMESPACE.equalsIgnoreCase(addon.namespace())) {
                logger.warning("Addon '" + addon.id() + "' attempted to overwrite core assets in 'assets/croppy/'. Rejected.");
                return false;
            }

            for (String segment : segments) {
                if (!PATH_SEGMENT_PATTERN.matcher(segment).matches()) {
                    logger.warning("Invalid resource pack asset path segment '" + segment + "' in addon '" + addon.id() + "'. Skipping.");
                    return false;
                }
            }

            Path destination = targetAssetsDir.resolve(relativePath);
            if (destination.getParent() != null && !Files.exists(destination.getParent())) {
                Files.createDirectories(destination.getParent());
            }

            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to copy asset file " + relativePath + " from addon " + addon.id(), e);
            return false;
        }
    }
}
