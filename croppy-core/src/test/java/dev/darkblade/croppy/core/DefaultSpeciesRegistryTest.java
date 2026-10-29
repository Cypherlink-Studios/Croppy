package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.core.species.SpeciesDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSpeciesRegistryTest {

    private DefaultSpeciesRegistry registry;
    private Species wheat;

    @BeforeEach
    void setUp() {
        registry = new DefaultSpeciesRegistry();
        wheat = new SpeciesDefinition(
                Key.croppy("wheat"),
                "Trigo",
                new CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds"),
                List.of(new StageDefinition("mature", 0, 7)),
                new CropRequirements(9, 1),
                HarvestDefinition.of()
        );
    }

    @Test
    @DisplayName("Lookup species by key, seed item, and block")
    void testRegistryLookups() {
        registry.register(wheat);

        Optional<Species> byKey = registry.get(Key.croppy("wheat"));
        assertTrue(byKey.isPresent());
        assertEquals("Trigo", byKey.get().getName());

        Optional<Species> bySeed = registry.findBySeedItem("minecraft:wheat_seeds");
        assertTrue(bySeed.isPresent());
        assertEquals(wheat, bySeed.get());

        // Case-insensitivity check
        Optional<Species> bySeedUpper = registry.findBySeedItem("MINECRAFT:WHEAT_SEEDS");
        assertTrue(bySeedUpper.isPresent());

        Optional<Species> byBlock = registry.findByBlock("minecraft:wheat");
        assertTrue(byBlock.isPresent());
        assertEquals(wheat, byBlock.get());

        // Non-existent lookups
        assertTrue(registry.findBySeedItem("minecraft:carrot").isEmpty());
        assertTrue(registry.findByBlock("minecraft:carrots").isEmpty());
    }
}
