package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper platform implementation of {@link AddonConfig} adapting Bukkit's {@link YamlConfiguration}.
 * Preserves comments, headers, and formatting on Paper 1.21.8+.
 */
public class PaperAddonConfig implements AddonConfig {

    private final File file;
    private final Logger logger;
    private ConfigurationSection section;

    public PaperAddonConfig(File file, Logger logger) {
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger(PaperAddonConfig.class.getName());
        if (file.exists()) {
            this.section = YamlConfiguration.loadConfiguration(file);
        } else {
            this.section = new YamlConfiguration();
        }
    }

    public PaperAddonConfig(Path filePath, Logger logger) {
        this(Objects.requireNonNull(filePath, "filePath cannot be null").toFile(), logger);
    }

    public PaperAddonConfig(File file, ConfigurationSection section, Logger logger) {
        this.file = file;
        this.section = Objects.requireNonNull(section, "section cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger(PaperAddonConfig.class.getName());
    }

    public ConfigurationSection getConfigurationSection() {
        return section;
    }

    @Override
    public String getString(String path) {
        return getString(path, null);
    }

    @Override
    public String getString(String path, String def) {
        return section.getString(path, def);
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int def) {
        return section.getInt(path, def);
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    @Override
    public long getLong(String path, long def) {
        return section.getLong(path, def);
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    @Override
    public double getDouble(String path, double def) {
        return section.getDouble(path, def);
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return section.getBoolean(path, def);
    }

    @Override
    public List<String> getStringList(String path) {
        List<String> list = section.getStringList(path);
        return Collections.unmodifiableList(list);
    }

    @Override
    public boolean contains(String path) {
        return section.contains(path);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return Collections.unmodifiableSet(section.getKeys(deep));
    }

    @Override
    public Optional<AddonConfig> getSection(String path) {
        if (section.isConfigurationSection(path)) {
            ConfigurationSection sub = section.getConfigurationSection(path);
            if (sub != null) {
                return Optional.of(new PaperAddonConfig(file, sub, logger));
            }
        }
        return Optional.empty();
    }

    @Override
    public void set(String path, Object value) {
        section.set(path, value);
    }

    @Override
    public synchronized void reload() {
        if (file == null) return;
        if (file.exists()) {
            this.section = YamlConfiguration.loadConfiguration(file);
        } else {
            this.section = new YamlConfiguration();
        }
    }

    @Override
    public synchronized void save() {
        if (file == null) return;
        if (!(section instanceof YamlConfiguration yamlConfig)) {
            logger.warning("Cannot save sub-section independently; save must be performed from root configuration.");
            return;
        }

        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            yamlConfig.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration to " + file.getAbsolutePath(), e);
        }
    }
}
