package dev.darkblade.croppy.core.fuel;

import dev.darkblade.croppy.api.fuel.FuelProperties;
import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFuelRegistryTest {

    @Test
    @DisplayName("Default registry contains baseline vanilla fuels")
    void baselineVanillaFuelsPresent() {
        DefaultFuelRegistry registry = new DefaultFuelRegistry();

        Optional<FuelProperties> coal = registry.resolveFuel(Key.of("minecraft", "coal"));
        assertTrue(coal.isPresent());
        assertEquals(1600L, coal.get().burnTicks());

        Optional<FuelProperties> blazeRod = registry.resolveFuel(Key.of("minecraft", "blaze_rod"));
        assertTrue(blazeRod.isPresent());
        assertEquals(2400L, blazeRod.get().burnTicks());

        Optional<FuelProperties> stick = registry.resolveFuel(Key.of("minecraft", "stick"));
        assertTrue(stick.isPresent());
        assertEquals(100L, stick.get().burnTicks());
    }

    @Test
    @DisplayName("Custom fuel registration, resolution, and unregistration")
    void customFuelManagement() {
        DefaultFuelRegistry registry = new DefaultFuelRegistry();
        Key customFuel = Key.of("croppy", "peat_briquette");

        assertFalse(registry.isFuel(customFuel));

        registry.registerFuel(customFuel, new FuelProperties(2200L, 500.0, 1.1));
        assertTrue(registry.isFuel(customFuel));
        assertEquals(2200L, registry.resolveFuel(customFuel).orElseThrow().burnTicks());

        assertTrue(registry.unregisterFuel(customFuel));
        assertFalse(registry.isFuel(customFuel));
    }

    @Test
    @DisplayName("Non-fuel items return empty optional")
    void nonFuelReturnsEmpty() {
        DefaultFuelRegistry registry = new DefaultFuelRegistry();
        Optional<FuelProperties> diamond = registry.resolveFuel(Key.of("minecraft", "diamond"));
        assertTrue(diamond.isEmpty());
        assertFalse(registry.isFuel(Key.of("minecraft", "diamond")));
    }
}
