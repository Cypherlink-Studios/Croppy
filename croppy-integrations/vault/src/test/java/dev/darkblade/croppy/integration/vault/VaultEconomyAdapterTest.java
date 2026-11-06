package dev.darkblade.croppy.integration.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VaultEconomyAdapter Unit Tests")
class VaultEconomyAdapterTest {

    @Test
    @DisplayName("VaultEconomyAdapter delegates operations and formatting properly")
    void testDelegation() {
        Economy dummyEconomy = (Economy) Proxy.newProxyInstance(
                Economy.class.getClassLoader(),
                new Class<?>[]{Economy.class},
                (proxy, method, args) -> {
                    if ("format".equals(method.getName())) {
                        return String.format(Locale.ROOT, "$%.2f", (Double) args[0]);
                    }
                    if ("has".equals(method.getName())) {
                        return (Double) args[1] <= 100.0;
                    }
                    if ("withdrawPlayer".equals(method.getName())) {
                        double amt = (Double) args[1];
                        return new EconomyResponse(amt, 100.0 - amt, EconomyResponse.ResponseType.SUCCESS, null);
                    }
                    if ("depositPlayer".equals(method.getName())) {
                        double amt = (Double) args[1];
                        return new EconomyResponse(amt, 100.0 + amt, EconomyResponse.ResponseType.SUCCESS, null);
                    }
                    return null;
                }
        );

        VaultEconomyAdapter adapter = new VaultEconomyAdapter(dummyEconomy);
        assertEquals("$50.00", adapter.format(50.0));
        assertThrows(NullPointerException.class, () -> adapter.has(null, 10.0));
        assertThrows(NullPointerException.class, () -> adapter.withdraw(null, 10.0));
        assertThrows(NullPointerException.class, () -> adapter.deposit(null, 10.0));
    }
}
