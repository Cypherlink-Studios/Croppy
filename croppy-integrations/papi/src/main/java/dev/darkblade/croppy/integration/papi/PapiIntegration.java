package dev.darkblade.croppy.integration.papi;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.integration.common.CroppyIntegration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Lifecycle coordinator for PlaceholderAPI integration.
 */
public class PapiIntegration implements CroppyIntegration {

    private final CroppyLogger logger;
    private final CroppyCore core;
    private final String version;
    private final boolean enabledInConfig;
    private final int raycastDistance;
    private CroppyPlaceholderExpansion expansion;

    public PapiIntegration(CroppyLogger logger, CroppyCore core, String version, boolean enabledInConfig, int raycastDistance) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.core = Objects.requireNonNull(core, "CroppyCore cannot be null");
        this.version = Objects.requireNonNull(version, "version cannot be null");
        this.enabledInConfig = enabledInConfig;
        this.raycastDistance = raycastDistance;
    }

    public PapiIntegration(Logger logger, CroppyCore core, String version, boolean enabledInConfig, int raycastDistance) {
        this(new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null")), core, version, enabledInConfig, raycastDistance);
    }

    @Override
    public String id() {
        return "papi";
    }

    @Override
    public boolean isAvailable() {
        if (!enabledInConfig) {
            return false;
        }
        if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
            return false;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        if (isAvailable()) {
            this.expansion = new CroppyPlaceholderExpansion(core, version, raycastDistance);
            if (this.expansion.register()) {
                logger.debug(LogChannel.INTEGRATION, () -> "Registered Croppy PlaceholderAPI expansion (raycast distance: " + raycastDistance + ").");
            } else {
                logger.warn(LogChannel.INTEGRATION, "Failed to register Croppy PlaceholderAPI expansion.");
            }
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Throwable ignored) {
            }
            expansion = null;
        }
    }

    public CroppyPlaceholderExpansion getExpansion() {
        return expansion;
    }
}
