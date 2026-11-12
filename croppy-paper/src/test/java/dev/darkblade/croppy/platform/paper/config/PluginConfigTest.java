package dev.darkblade.croppy.platform.paper.config;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import dev.darkblade.croppy.platform.paper.logging.LogStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginConfigTest {

    @Test
    void testLoggingDefaultsWhenSectionMissing() {
        PluginConfig config = new PluginConfig();
        config.load(new YamlConfiguration());

        assertEquals(LogLevel.INFO, config.getLoggingLevel());
        assertEquals(LogStyle.RICH, config.getLoggingStyle());
        assertTrue(config.getLoggingDebugChannels().isEmpty());
    }

    @Test
    void testLoggingValidConfigurationValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("logging.level", "debug");
        yaml.set("logging.style", "compact");
        yaml.set("logging.debug-channels", List.of("LOADER", "station", " I18N "));

        PluginConfig config = new PluginConfig();
        config.load(yaml);

        assertEquals(LogLevel.DEBUG, config.getLoggingLevel());
        assertEquals(LogStyle.COMPACT, config.getLoggingStyle());
        assertEquals(Set.of(LogChannel.LOADER, LogChannel.STATION, LogChannel.I18N), config.getLoggingDebugChannels());
    }

    @Test
    void testLoggingInvalidValuesFallbackSafelyAndWarn() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("logging.level", "VERBOSE");
        yaml.set("logging.style", "FANCY");
        yaml.set("logging.debug-channels", List.of("growth", "nonexistent_channel"));

        List<String> warnings = new ArrayList<>();
        CroppyLogger spyLogger = new CroppyLogger() {
            @Override
            public boolean isEnabled(LogLevel level, LogChannel channel) {
                return true;
            }

            @Override
            public void log(LogLevel level, LogChannel channel, String message) {
                if (level == LogLevel.WARN) {
                    warnings.add(message);
                }
            }

            @Override
            public void log(LogLevel level, LogChannel channel, String message, Throwable throwable) {
                if (level == LogLevel.WARN) {
                    warnings.add(message);
                }
            }
        };

        PluginConfig config = new PluginConfig();
        config.load(yaml, spyLogger);

        assertEquals(LogLevel.INFO, config.getLoggingLevel());
        assertEquals(LogStyle.RICH, config.getLoggingStyle());
        assertEquals(Set.of(LogChannel.GROWTH), config.getLoggingDebugChannels());
        assertEquals(3, warnings.size());
    }

    @Test
    @DisplayName("Should load default values when configuration is empty or null")
    void shouldLoadDefaultsOnEmptyConfig() {
        PluginConfig config = new PluginConfig();
        config.load(null);

        assertEquals(20L, config.getGrowthTickInterval());
        assertEquals(1.0, config.getGrowthBaseProgress());
        assertEquals(4, config.getGrowthWaterSearchRadius());
        assertEquals("pdc", config.getStorageType());
        assertEquals("en-US", config.getDefaultLocale());
        assertEquals(true, config.isWorldGuardEnabled());
        assertEquals(true, config.isWorldGuardUseCustomFlags());
        assertEquals(true, config.isPapiEnabled());
        assertEquals(5, config.getPapiRaycastDistance());
        assertEquals(true, config.isVaultEnabled());

        config.load(new YamlConfiguration());
        assertEquals(20L, config.getGrowthTickInterval());
        assertEquals(1.0, config.getGrowthBaseProgress());
        assertEquals(4, config.getGrowthWaterSearchRadius());
        assertEquals("pdc", config.getStorageType());
        assertEquals("en-US", config.getDefaultLocale());
        assertEquals(true, config.isWorldGuardEnabled());
        assertEquals(true, config.isWorldGuardUseCustomFlags());
        assertEquals(true, config.isPapiEnabled());
        assertEquals(5, config.getPapiRaycastDistance());
        assertEquals(true, config.isVaultEnabled());
    }

    @Test
    @DisplayName("Should correctly load configured growth and storage settings")
    void shouldLoadConfiguredGrowthAndStorage() {
        String yaml = """
                growth:
                  tick-interval: 40
                  base-progress: 2.5
                  water-search-radius: 6
                storage:
                  type: "sqlite"
                locale:
                  default: "es-ES"
                """;

        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        PluginConfig config = new PluginConfig();
        config.load(fileConfig);

        assertEquals(40L, config.getGrowthTickInterval());
        assertEquals(2.5, config.getGrowthBaseProgress());
        assertEquals(6, config.getGrowthWaterSearchRadius());
        assertEquals("sqlite", config.getStorageType());
        assertEquals("es-ES", config.getDefaultLocale());
    }

    @Test
    @DisplayName("Should resolve default locale via dual-fallback mechanism")
    void shouldResolveLocaleWithFallback() {
        // 1. Modern locale.default takes precedence
        String modernYaml = """
                locale:
                  default: "fr-FR"
                messages:
                  default-locale: "es-ES"
                """;
        YamlConfiguration modernConfig = YamlConfiguration.loadConfiguration(new StringReader(modernYaml));
        PluginConfig config = new PluginConfig();
        config.load(modernConfig);
        assertEquals("fr-FR", config.getDefaultLocale());

        // 2. Legacy messages.default-locale is used when locale.default is absent
        String legacyYaml = """
                messages:
                  default-locale: "de-DE"
                """;
        YamlConfiguration legacyConfig = YamlConfiguration.loadConfiguration(new StringReader(legacyYaml));
        config.load(legacyConfig);
        assertEquals("de-DE", config.getDefaultLocale());

        // 3. Fallback to en-US when neither is specified
        String emptyYaml = """
                growth:
                  tick-interval: 20
                """;
        YamlConfiguration emptyConfig = YamlConfiguration.loadConfiguration(new StringReader(emptyYaml));
        config.load(emptyConfig);
        assertEquals("en-US", config.getDefaultLocale());
    }

    @Test
    @DisplayName("Should enforce minimum bounds for numeric configuration properties")
    void shouldEnforceMinimumBounds() {
        String yaml = """
                growth:
                  tick-interval: -10
                  base-progress: -5.0
                  water-search-radius: -2
                """;

        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        PluginConfig config = new PluginConfig();
        config.load(fileConfig);

        assertEquals(1L, config.getGrowthTickInterval());
        assertEquals(0.01, config.getGrowthBaseProgress());
        assertEquals(0, config.getGrowthWaterSearchRadius());
    }

    @Test
    @DisplayName("Should correctly load update checker and stager settings")
    void shouldLoadUpdateSettings() {
        PluginConfig defaultConfig = new PluginConfig();
        defaultConfig.load(new YamlConfiguration());
        assertTrue(defaultConfig.isUpdatesEnabled());
        assertTrue(defaultConfig.isUpdatesCheckOnStartup());
        assertEquals(dev.darkblade.croppy.api.update.UpdateChannel.STABLE, defaultConfig.getUpdatesChannel());

        String yaml = """
                updates:
                  enabled: false
                  check-on-startup: false
                  channel: "BETA"
                """;

        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        PluginConfig config = new PluginConfig();
        config.load(fileConfig);

        assertFalse(config.isUpdatesEnabled());
        assertFalse(config.isUpdatesCheckOnStartup());
        assertEquals(dev.darkblade.croppy.api.update.UpdateChannel.BETA, config.getUpdatesChannel());
    }
}
