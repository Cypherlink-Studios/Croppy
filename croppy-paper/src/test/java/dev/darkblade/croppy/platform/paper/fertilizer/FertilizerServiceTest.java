package dev.darkblade.croppy.platform.paper.fertilizer;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FertilizerServiceTest {

    private DefaultFertilizerRegistry registry;
    private FertilizerService service;

    @BeforeEach
    void setUp() {
        registry = new DefaultFertilizerRegistry();
        service = new FertilizerService(null, registry);
    }

    @Test
    @DisplayName("Verify transient soil buffer storage and consumption")
    void testSoilBuffer() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        Key speedGro = Key.of("croppy", "growth_fertilizer");

        service.bufferSoilFertilizer(pos, speedGro);

        Optional<Key> consumed = service.consumeBufferedFertilizer(pos);
        assertTrue(consumed.isPresent());
        assertEquals(speedGro, consumed.get());

        // Second consumption is empty
        assertTrue(service.consumeBufferedFertilizer(pos).isEmpty());
    }

    @Test
    @DisplayName("Verify fertilizer registry integration")
    void testRegistryIntegration() {
        Key key = Key.of("croppy", "growth_fertilizer");
        Fertilizer fert = new Fertilizer(
                key,
                "Growth Fertilizer",
                List.of("Boosts speed"),
                FertilizerType.SPEED,
                1.25,
                0.0,
                0.0,
                "BONE_MEAL",
                1001
        );
        registry.register(fert);

        assertTrue(registry.get(key).isPresent());
        assertEquals(FertilizerType.SPEED, registry.get(key).get().type());
    }
}
