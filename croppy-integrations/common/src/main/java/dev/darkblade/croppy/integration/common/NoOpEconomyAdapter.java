package dev.darkblade.croppy.integration.common;

import dev.darkblade.croppy.api.economy.EconomyPort;

import java.util.Locale;
import java.util.UUID;

/**
 * Fallback no-op economy adapter that reports insufficient funds and rejects transactions gracefully.
 */
public final class NoOpEconomyAdapter implements EconomyPort {

    public static final NoOpEconomyAdapter INSTANCE = new NoOpEconomyAdapter();

    @Override
    public boolean has(UUID playerId, double amount) {
        return false;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        return false;
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        return false;
    }

    @Override
    public String format(double amount) {
        return String.format(Locale.ROOT, "$%.2f", amount);
    }
}
