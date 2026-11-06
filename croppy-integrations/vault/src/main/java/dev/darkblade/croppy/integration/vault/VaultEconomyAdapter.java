package dev.darkblade.croppy.integration.vault;

import dev.darkblade.croppy.api.economy.EconomyPort;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Economy port adapter delegating currency checks and operations to Vault's registered Economy provider.
 */
public class VaultEconomyAdapter implements EconomyPort {

    private final Economy economy;

    public VaultEconomyAdapter(Economy economy) {
        this.economy = Objects.requireNonNull(economy, "Economy service provider cannot be null");
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        if (amount <= 0) {
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        if (amount <= 0) {
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        if (!economy.has(player, amount)) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        if (amount <= 0) {
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
