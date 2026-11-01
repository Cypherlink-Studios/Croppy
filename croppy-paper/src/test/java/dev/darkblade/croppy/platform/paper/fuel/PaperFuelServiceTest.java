package dev.darkblade.croppy.platform.paper.fuel;

import dev.darkblade.croppy.api.fuel.FuelProperties;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.core.fuel.DefaultFuelRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PaperFuelServiceTest {

    @Test
    @DisplayName("Fuel service resolves registered fuels by key")
    void testResolveFuelByKey() {
        DefaultFuelRegistry registry = new DefaultFuelRegistry();
        PaperFuelService service = new PaperFuelService(registry);

        Optional<FuelProperties> coal = service.resolveFuel(Key.of("minecraft", "coal"));
        assertTrue(coal.isPresent());
        assertEquals(1600L, coal.get().burnTicks());

        Optional<FuelProperties> unknown = service.resolveFuel(Key.of("minecraft", "dirt"));
        assertTrue(unknown.isEmpty());
        assertFalse(service.isFuel((Key) null));
    }

    @Test
    @DisplayName("Fuel service exposes underlying core registry for extensions")
    void testCoreRegistryAccess() {
        DefaultFuelRegistry registry = new DefaultFuelRegistry();
        PaperFuelService service = new PaperFuelService(registry);

        assertSame(registry, service.getCoreRegistry());

        Key customKey = Key.of("croppy", "super_fuel");
        registry.registerFuel(customKey, new FuelProperties(5000L, 800.0, 1.5));

        assertTrue(service.isFuel(customKey));
        assertEquals(5000L, service.resolveFuel(customKey).orElseThrow().burnTicks());
    }
}
