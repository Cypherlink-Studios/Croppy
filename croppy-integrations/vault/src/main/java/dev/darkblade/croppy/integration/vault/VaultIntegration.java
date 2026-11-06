package dev.darkblade.croppy.integration.vault;

import dev.darkblade.croppy.api.economy.EconomyPort;
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

    private final Logger logger;
    private final boolean enabledInConfig;
    private VaultEconomyAdapter adapter;

    public VaultIntegration(Logger logger, boolean enabledInConfig) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.enabledInConfig = enabledInConfig;
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
                logger.info("Hooked into Vault Economy provider: " + rsp.getProvider().getName());
            } else {
                logger.warning("Vault is installed, but no Economy provider has registered with ServicesManager.");
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
