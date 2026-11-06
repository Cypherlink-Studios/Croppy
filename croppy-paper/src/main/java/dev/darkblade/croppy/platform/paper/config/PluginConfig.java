package dev.darkblade.croppy.platform.paper.config;

import org.bukkit.configuration.file.FileConfiguration;

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

    public void load(FileConfiguration config) {
        if (config == null) {
            return;
        }

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
}
