package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.AddonManager;
import dev.darkblade.croppy.api.addon.AddonState;
import dev.darkblade.croppy.api.registry.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core manager tracking addon discovery, state transitions, and file resolution.
 */
public class DefaultAddonManager implements AddonManager, Service {

    public static final String MANIFEST_FILE = "addon.yml";

    protected final Path addonsDirectory;
    protected final Logger logger;
    protected final YamlAddonDescriptionLoader descriptionLoader;

    protected final Map<String, AddonDescription> discoveredAddons = new ConcurrentHashMap<>();
    protected final Map<String, AddonState> addonStates = new ConcurrentHashMap<>();
    protected final Map<String, Path> addonPaths = new ConcurrentHashMap<>();
    protected final Map<String, Path> dataFolders = new ConcurrentHashMap<>();

    public DefaultAddonManager(Path addonsDirectory, Logger logger) {
        this.addonsDirectory = Objects.requireNonNull(addonsDirectory, "addonsDirectory cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger(DefaultAddonManager.class.getName());
        this.descriptionLoader = new YamlAddonDescriptionLoader();
    }

    public DefaultAddonManager(Path addonsDirectory, Logger logger, YamlAddonDescriptionLoader descriptionLoader) {
        this.addonsDirectory = Objects.requireNonNull(addonsDirectory, "addonsDirectory cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger(DefaultAddonManager.class.getName());
        this.descriptionLoader = Objects.requireNonNull(descriptionLoader, "descriptionLoader cannot be null");
    }

    @Override
    public synchronized void discoverAddons() {
        if (!Files.exists(addonsDirectory)) {
            try {
                Files.createDirectories(addonsDirectory);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create addons directory: " + addonsDirectory, e);
                return;
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(addonsDirectory)) {
            for (Path entry : stream) {
                try {
                    discoverEntry(entry);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to inspect potential addon entry: " + entry.getFileName(), e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to scan addons directory: " + addonsDirectory, e);
        }
    }

    private void discoverEntry(Path entry) throws IOException {
        String fileName = entry.getFileName().toString();
        if (Files.isDirectory(entry)) {
            Path manifest = entry.resolve(MANIFEST_FILE);
            if (Files.exists(manifest)) {
                AddonDescription desc = descriptionLoader.load(manifest);
                registerDiscovered(desc, entry);
            }
        } else if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
            try (JarFile jar = new JarFile(entry.toFile())) {
                JarEntry manifestEntry = jar.getJarEntry(MANIFEST_FILE);
                if (manifestEntry != null) {
                    try (InputStream in = jar.getInputStream(manifestEntry)) {
                        AddonDescription desc = descriptionLoader.load(in, entry.toString() + "!/" + MANIFEST_FILE);
                        registerDiscovered(desc, entry);
                    }
                }
            }
        }
    }

    public boolean registerDiscovered(AddonDescription description, Path path) {
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(path, "path cannot be null");
        String id = description.id();

        // 1. Duplicate ID validation
        if (discoveredAddons.containsKey(id)) {
            Path existing = addonPaths.get(id);
            logger.severe("CRITICAL: Duplicate addon id '" + id + "' detected! Conflict between ["
                    + existing + "] and [" + path + "]. Registration rejected.");
            return false;
        }

        discoveredAddons.put(id, description);
        addonPaths.put(id, path);
        addonStates.putIfAbsent(id, AddonState.ON_DISK);

        // 2. Data folder binding & naming convention validation
        if (Files.isDirectory(path)) {
            if (!path.getFileName().toString().equalsIgnoreCase(id)) {
                logger.warning("Directory addon '" + id + "' is located in folder '" + path.getFileName()
                        + "'. It is strongly recommended that directory names match the addon manifest id.");
            }
            dataFolders.put(id, path);
        } else {
            dataFolders.put(id, addonsDirectory.resolve(id));
        }
        return true;
    }

    @Override
    public Optional<AddonDescription> getAddon(String addonId) {
        if (addonId == null) return Optional.empty();
        return Optional.ofNullable(discoveredAddons.get(addonId));
    }

    public Optional<Path> getAddonPath(String addonId) {
        if (addonId == null) return Optional.empty();
        return Optional.ofNullable(addonPaths.get(addonId));
    }

    public Optional<Path> getDataFolder(String addonId) {
        if (addonId == null) return Optional.empty();
        return Optional.ofNullable(dataFolders.get(addonId));
    }

    public Map<String, Path> getAddonPaths() {
        return Collections.unmodifiableMap(addonPaths);
    }

    public Map<String, Path> getDataFolders() {
        return Collections.unmodifiableMap(dataFolders);
    }

    @Override
    public AddonState getState(String addonId) {
        if (addonId == null) return AddonState.ON_DISK;
        return addonStates.getOrDefault(addonId, AddonState.ON_DISK);
    }

    @Override
    public Collection<AddonDescription> getDiscoveredAddons() {
        return Collections.unmodifiableCollection(discoveredAddons.values());
    }

    @Override
    public synchronized boolean loadAddon(String addonId) {
        AddonDescription desc = discoveredAddons.get(addonId);
        if (desc == null) return false;
        AddonState current = getState(addonId);
        if (current != AddonState.ON_DISK) return false;

        // Path Ownership Gate: verify data folder does not collide with another addon's root directory
        Path dataFolder = dataFolders.get(addonId);
        Path addonPath = addonPaths.get(addonId);
        if (dataFolder != null && addonPath != null && !Files.isDirectory(addonPath)) {
            for (Map.Entry<String, Path> entry : addonPaths.entrySet()) {
                if (!entry.getKey().equals(addonId) && Files.isDirectory(entry.getValue())) {
                    if (entry.getValue().toAbsolutePath().normalize().equals(dataFolder.toAbsolutePath().normalize())) {
                        logger.severe("CRITICAL: Path collision detected! JAR addon '" + addonId + "' data folder ["
                                + dataFolder + "] is already occupied by directory addon '" + entry.getKey() + "'. Halting load.");
                        return false;
                    }
                }
            }
        }

        addonStates.put(addonId, AddonState.LOADED);
        return true;
    }

    @Override
    public synchronized boolean enableAddon(String addonId) {
        AddonDescription desc = discoveredAddons.get(addonId);
        if (desc == null) return false;
        AddonState current = getState(addonId);
        if (current == AddonState.ON_DISK) {
            if (!loadAddon(addonId)) return false;
        }

        addonStates.put(addonId, AddonState.ENABLED);
        return true;
    }

    @Override
    public synchronized boolean disableAddon(String addonId) {
        AddonDescription desc = discoveredAddons.get(addonId);
        if (desc == null) return false;
        AddonState current = getState(addonId);
        if (current != AddonState.ENABLED) return false;

        addonStates.put(addonId, AddonState.LOADED);
        return true;
    }

    @Override
    public synchronized boolean unloadAddon(String addonId) {
        AddonDescription desc = discoveredAddons.get(addonId);
        if (desc == null) return false;
        AddonState current = getState(addonId);
        if (current == AddonState.ENABLED) {
            disableAddon(addonId);
        }

        addonStates.put(addonId, AddonState.ON_DISK);
        return true;
    }
}
