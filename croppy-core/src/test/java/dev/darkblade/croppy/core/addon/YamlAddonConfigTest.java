package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class YamlAddonConfigTest {

    @TempDir
    Path tempDir;

    private static final String SAMPLE_YAML = """
            name: "Artisan Coffee"
            enabled: true
            brewing:
              temperature: 92.5
              default-duration: 45
              tags:
                - "specialty"
                - "dark-roast"
            advanced:
              nested:
                deep-flag: true
                counter: 10000000000
            """;

    @Test
    @DisplayName("Load from InputStream and read typed values")
    void testLoadFromInputStream() {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_YAML.getBytes(StandardCharsets.UTF_8));
        YamlAddonConfig config = YamlAddonConfig.load(in);

        assertEquals("Artisan Coffee", config.getString("name"));
        assertTrue(config.getBoolean("enabled"));
        assertEquals(92.5, config.getDouble("brewing.temperature"), 0.001);
        assertEquals(45, config.getInt("brewing.default-duration"));
        assertEquals(List.of("specialty", "dark-roast"), config.getStringList("brewing.tags"));
        assertTrue(config.getBoolean("advanced.nested.deep-flag"));
        assertEquals(10000000000L, config.getLong("advanced.nested.counter"));
    }

    @Test
    @DisplayName("Fallback to default values on missing keys")
    void testFallbackDefaults() {
        YamlAddonConfig config = YamlAddonConfig.empty();

        assertEquals("fallback", config.getString("missing.key", "fallback"));
        assertEquals(42, config.getInt("missing.key", 42));
        assertEquals(99.9, config.getDouble("missing.key", 99.9), 0.001);
        assertTrue(config.getBoolean("missing.key", true));
        assertEquals(12345L, config.getLong("missing.key", 12345L));
        assertTrue(config.getStringList("missing.key").isEmpty());
    }

    @Test
    @DisplayName("Nested section navigation")
    void testSections() {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_YAML.getBytes(StandardCharsets.UTF_8));
        YamlAddonConfig config = YamlAddonConfig.load(in);

        Optional<AddonConfig> brewingSection = config.getSection("brewing");
        assertTrue(brewingSection.isPresent());
        assertEquals(92.5, brewingSection.get().getDouble("temperature"), 0.001);
        assertEquals(45, brewingSection.get().getInt("default-duration"));

        Optional<AddonConfig> nonExistent = config.getSection("invalid.path");
        assertTrue(nonExistent.isEmpty());
    }

    @Test
    @DisplayName("Set values, save to disk, and reload")
    void testSetSaveAndReload() throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        YamlAddonConfig config = new YamlAddonConfig(configFile);

        config.set("app.title", "My Croppy Addon");
        config.set("app.version", 1);
        config.set("app.features.auto-water", true);
        config.save();

        assertTrue(Files.exists(configFile));

        // Load into a new config instance
        YamlAddonConfig loaded = YamlAddonConfig.load(configFile);
        assertEquals("My Croppy Addon", loaded.getString("app.title"));
        assertEquals(1, loaded.getInt("app.version"));
        assertTrue(loaded.getBoolean("app.features.auto-water"));

        // Modify file externally and reload
        Files.writeString(configFile, "app:\n  title: \"Updated Addon\"\n");
        loaded.reload();
        assertEquals("Updated Addon", loaded.getString("app.title"));
    }

    @Test
    @DisplayName("Collect keys shallow and deep")
    void testGetKeys() {
        ByteArrayInputStream in = new ByteArrayInputStream(SAMPLE_YAML.getBytes(StandardCharsets.UTF_8));
        YamlAddonConfig config = YamlAddonConfig.load(in);

        Set<String> shallow = config.getKeys(false);
        assertTrue(shallow.contains("name"));
        assertTrue(shallow.contains("enabled"));
        assertTrue(shallow.contains("brewing"));
        assertFalse(shallow.contains("brewing.temperature"));

        Set<String> deep = config.getKeys(true);
        assertTrue(deep.contains("name"));
        assertTrue(deep.contains("brewing.temperature"));
        assertTrue(deep.contains("advanced.nested.deep-flag"));
    }
}
