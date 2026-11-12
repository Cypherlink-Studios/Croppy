package dev.darkblade.croppy.platform.paper.config;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.platform.paper.logging.LogStyle;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import java.util.Objects;

/**
 * Accessor for core configurable plugin settings (growth parameters, storage backend, default locale).
 */
public class PluginConfig {

    private long growthTickInterval = 20L;
    private double growthBaseProgress = 1.0;
    private int growthWaterSearchRadius = 4;
    private String storageType = "pdc";
    private String defaultLocale = "en-US";

    // Integrations
    private boolean worldGuardEnabled = true;
    private boolean worldGuardUseCustomFlags = true;
    private boolean papiEnabled = true;
    private int papiRaycastDistance = 5;
    private boolean vaultEnabled = true;

    // Resource Pack Pipeline
    private boolean resourcePackServerEnabled = true;
    private String resourcePackServerHost = "0.0.0.0";
    private int resourcePackServerPort = 8085;
    private String resourcePackPublicUrl = "";
    private boolean resourcePackAutoSendOnJoin = true;
    private boolean resourcePackRequired = false;
    private String resourcePackPrompt = "<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>Please download the 3D companion pack to see custom crops and stations.</gray>";

    // Update Checker & Stager
    private boolean updatesEnabled = true;
    private boolean updatesCheckOnStartup = true;
    private dev.darkblade.croppy.api.update.UpdateChannel updatesChannel = dev.darkblade.croppy.api.update.UpdateChannel.STABLE;

    // Culinary Compendium Settings
    private boolean compendiumShowUnknownRecipes = true;
    private boolean compendiumEnableDetailView = true;
    private int compendiumStationProximityDistance = 5;

    // Crafting Settings
    private boolean craftingAutoDiscoverRecipes = true;

    // Logging Settings
    private LogLevel loggingLevel = LogLevel.INFO;
    private LogStyle loggingStyle = LogStyle.RICH;
    private Set<LogChannel> loggingDebugChannels = Set.of();

    public void load(FileConfiguration config) {
        load(config, NoOpCroppyLogger.INSTANCE);
    }

    public void load(FileConfiguration config, CroppyLogger logger) {
        if (config == null) {
            return;
        }
        CroppyLogger log = logger != null ? logger : NoOpCroppyLogger.INSTANCE;

        this.growthTickInterval = Math.max(1L, config.getLong("growth.tick-interval", 20L));
        this.growthBaseProgress = Math.max(0.01, config.getDouble("growth.base-progress", 1.0));
        this.growthWaterSearchRadius = Math.max(0, config.getInt("growth.water-search-radius", 4));
        this.storageType = config.getString("storage.type", "pdc").trim().toLowerCase();

        // Dual fallback: locale.default -> messages.default-locale -> en-US
        String rawLocale = config.getString("locale.default");
        if (rawLocale == null || rawLocale.isBlank()) {
            rawLocale = config.getString("messages.default-locale", "en-US");
        }
        this.defaultLocale = (rawLocale != null && !rawLocale.isBlank()) ? rawLocale.trim() : "en-US";

        // Integrations configuration
        this.worldGuardEnabled = config.getBoolean("integrations.worldguard.enabled", true);
        this.worldGuardUseCustomFlags = config.getBoolean("integrations.worldguard.use-custom-flags", true);
        this.papiEnabled = config.getBoolean("integrations.papi.enabled", true);
        this.papiRaycastDistance = Math.max(1, Math.min(config.getInt("integrations.papi.raycast-distance", 5), 32));
        this.vaultEnabled = config.getBoolean("integrations.vault.enabled", true);

        // Resource pack configuration
        this.resourcePackServerEnabled = config.getBoolean("resource-pack.embedded-server.enabled", true);
        this.resourcePackServerHost = config.getString("resource-pack.embedded-server.host", "0.0.0.0");
        this.resourcePackServerPort = config.getInt("resource-pack.embedded-server.port", 8085);
        this.resourcePackPublicUrl = config.getString("resource-pack.embedded-server.public-url", "");
        this.resourcePackAutoSendOnJoin = config.getBoolean("resource-pack.delivery.auto-send-on-join", true);
        this.resourcePackRequired = config.getBoolean("resource-pack.delivery.required", false);
        this.resourcePackPrompt = config.getString("resource-pack.delivery.prompt", "<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>Please download the 3D companion pack to see custom crops and stations.</gray>");

        // Updates configuration
        this.updatesEnabled = config.getBoolean("updates.enabled", true);
        this.updatesCheckOnStartup = config.getBoolean("updates.check-on-startup", true);
        this.updatesChannel = dev.darkblade.croppy.api.update.UpdateChannel.fromString(config.getString("updates.channel", "STABLE"));

        // Compendium configuration
        this.compendiumShowUnknownRecipes = config.getBoolean("compendium.show-unknown-recipes", true);
        this.compendiumEnableDetailView = config.getBoolean("compendium.enable-detail-view", true);
        this.compendiumStationProximityDistance = Math.max(1, config.getInt("compendium.station-proximity-distance", 5));

        // Crafting configuration
        this.craftingAutoDiscoverRecipes = config.getBoolean("crafting.auto-discover-recipes", true);

        // Logging configuration
        String rawLevel = config.getString("logging.level", "INFO");
        this.loggingLevel = LogLevel.fromString(rawLevel).orElseGet(() -> {
            log.warn(LogChannel.CORE, "Invalid logging.level '" + rawLevel + "' in config.yml; falling back to INFO.");
            return LogLevel.INFO;
        });

        String rawStyle = config.getString("logging.style", "RICH");
        this.loggingStyle = LogStyle.fromString(rawStyle).orElseGet(() -> {
            log.warn(LogChannel.CORE, "Invalid logging.style '" + rawStyle + "' in config.yml; falling back to RICH.");
            return LogStyle.RICH;
        });

        List<String> rawChannels = config.getStringList("logging.debug-channels");
        EnumSet<LogChannel> parsedChannels = EnumSet.noneOf(LogChannel.class);
        if (rawChannels != null) {
            for (String entry : rawChannels) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                LogChannel.fromString(entry).ifPresentOrElse(
                        parsedChannels::add,
                        () -> log.warn(LogChannel.CORE, "Unknown logging.debug-channels entry '" + entry + "' ignored.")
                );
            }
        }
        this.loggingDebugChannels = parsedChannels.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(parsedChannels);
    }

    public boolean isCraftingAutoDiscoverRecipes() {
        return craftingAutoDiscoverRecipes;
    }

    public boolean isUpdatesEnabled() {
        return updatesEnabled;
    }

    public boolean isUpdatesCheckOnStartup() {
        return updatesCheckOnStartup;
    }

    public dev.darkblade.croppy.api.update.UpdateChannel getUpdatesChannel() {
        return updatesChannel;
    }

    public boolean isCompendiumShowUnknownRecipes() {
        return compendiumShowUnknownRecipes;
    }

    public boolean isCompendiumEnableDetailView() {
        return compendiumEnableDetailView;
    }

    public int getCompendiumStationProximityDistance() {
        return compendiumStationProximityDistance;
    }

    public long getGrowthTickInterval() {
        return growthTickInterval;
    }

    public double getGrowthBaseProgress() {
        return growthBaseProgress;
    }

    public int getGrowthWaterSearchRadius() {
        return growthWaterSearchRadius;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public boolean isWorldGuardUseCustomFlags() {
        return worldGuardUseCustomFlags;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }

    public int getPapiRaycastDistance() {
        return papiRaycastDistance;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public boolean isResourcePackServerEnabled() {
        return resourcePackServerEnabled;
    }

    public String getResourcePackServerHost() {
        return resourcePackServerHost;
    }

    public int getResourcePackServerPort() {
        return resourcePackServerPort;
    }

    public String getResourcePackPublicUrl() {
        return resourcePackPublicUrl;
    }

    public boolean isResourcePackAutoSendOnJoin() {
        return resourcePackAutoSendOnJoin;
    }

    public boolean isResourcePackRequired() {
        return resourcePackRequired;
    }

    public String getResourcePackPrompt() {
        return resourcePackPrompt;
    }

    public LogLevel getLoggingLevel() {
        return loggingLevel;
    }

    public LogStyle getLoggingStyle() {
        return loggingStyle;
    }

    public Set<LogChannel> getLoggingDebugChannels() {
        return loggingDebugChannels;
    }
}
