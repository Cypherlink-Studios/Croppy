package dev.darkblade.croppy.api.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EconomyPort Unit Tests")
class EconomyPortTest {

    @Test
    @DisplayName("EconomyPort contract can be implemented and manages mock transactions")
    void testEconomyPortContract() {
        Map<UUID, Double> balances = new HashMap<>();

        EconomyPort mockEconomy = new EconomyPort() {
            @Override
            public boolean has(UUID playerId, double amount) {
                return balances.getOrDefault(playerId, 0.0) >= amount;
            }

            @Override
            public boolean withdraw(UUID playerId, double amount) {
                if (!has(playerId, amount)) {
                    return false;
                }
                balances.put(playerId, balances.get(playerId) - amount);
                return true;
            }

            @Override
            public boolean deposit(UUID playerId, double amount) {
                balances.put(playerId, balances.getOrDefault(playerId, 0.0) + amount);
                return true;
            }

            @Override
            public String format(double amount) {
                return String.format(java.util.Locale.ROOT, "$%.2f", amount);
            }
        };

        UUID player = UUID.randomUUID();
        assertEquals("$100.00", mockEconomy.format(100.0));
        assertFalse(mockEconomy.has(player, 50.0));
        assertFalse(mockEconomy.withdraw(player, 50.0));

        assertTrue(mockEconomy.deposit(player, 150.0));
        assertTrue(mockEconomy.has(player, 100.0));
        assertTrue(mockEconomy.withdraw(player, 50.0));
        assertEquals(100.0, balances.get(player));
    }
}
