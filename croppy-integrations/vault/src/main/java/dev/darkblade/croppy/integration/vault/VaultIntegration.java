package dev.darkblade.croppy.integration.vault;

import dev.darkblade.croppy.api.economy.EconomyPort;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.integration.common.CroppyIntegration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Lifecycle coordinator for Vault economy integration.
 */
public class VaultIntegration implements CroppyIntegration {

    private final CroppyLogger logger;
    private final boolean enabledInConfig;
    private VaultEconomyAdapter adapter;

    public VaultIntegration(CroppyLogger logger, boolean enabledInConfig) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.enabledInConfig = enabledInConfig;
    }

    public VaultIntegration(Logger logger, boolean enabledInConfig) {
        this(new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null")), enabledInConfig);
    }

    @Override
    public String id() {
        return "vault";
    }

    @Override
    public boolean isAvailable() {
        if (!enabledInConfig) {
            return false;
        }
        if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
            return false;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        if (isAvailable()) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null && rsp.getProvider() != null) {
                this.adapter = new VaultEconomyAdapter(rsp.getProvider());
                logger.debug(LogChannel.INTEGRATION, () -> "Hooked into Vault Economy provider: " + rsp.getProvider().getName());
            } else {
                logger.warn(LogChannel.INTEGRATION, "Vault is installed, but no Economy provider has registered with ServicesManager.");
            }
        }
    }

    @Override
    public void onDisable() {
        this.adapter = null;
    }

    public EconomyPort getEconomyAdapter() {
        return adapter;
    }
}
