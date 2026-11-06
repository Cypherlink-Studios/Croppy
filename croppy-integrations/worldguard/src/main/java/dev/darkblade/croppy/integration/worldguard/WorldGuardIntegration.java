package dev.darkblade.croppy.integration.worldguard;

import dev.darkblade.croppy.api.protection.ProtectionPort;
import dev.darkblade.croppy.integration.common.CroppyIntegration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Lifecycle coordinator for WorldGuard 7.x integration.
 */
public class WorldGuardIntegration implements CroppyIntegration {

    private final Logger logger;
    private final boolean enabledInConfig;
    private final boolean useCustomFlags;
    private WorldGuardProtectionAdapter adapter;

    public WorldGuardIntegration(Logger logger, boolean enabledInConfig, boolean useCustomFlags) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.enabledInConfig = enabledInConfig;
        this.useCustomFlags = useCustomFlags;
    }

    @Override
    public String id() {
        return "worldguard";
    }

    @Override
    public boolean isAvailable() {
        if (!enabledInConfig) {
            return false;
        }
        if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
            return false;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        return plugin != null && plugin.isEnabled() && isWorldGuardPresent();
    }

    @Override
    public void onLoad() {
        if (!enabledInConfig) {
            return;
        }
        if (useCustomFlags && isWorldGuardPresent()) {
            WorldGuardFlags.register(logger);
        }
    }

    @Override
    public void onEnable() {
        if (isAvailable()) {
            this.adapter = new WorldGuardProtectionAdapter(useCustomFlags);
            logger.info("WorldGuard integration activated with custom flags: " + useCustomFlags);
        }
    }

    /**
     * Returns the active protection port adapter, or null if not enabled.
     *
     * @return WorldGuardProtectionAdapter instance or null
     */
    public ProtectionPort getProtectionAdapter() {
        return adapter;
    }

    private boolean isWorldGuardPresent() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
