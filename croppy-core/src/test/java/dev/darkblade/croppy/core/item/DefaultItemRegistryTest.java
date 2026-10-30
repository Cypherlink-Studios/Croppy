package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultItemRegistryTest {

    private DefaultItemRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultItemRegistry();
    }

    @Test
    @DisplayName("Register and retrieve item by key")
    void testRegisterAndGet() {
        Key key = Key.of("croppy", "speed_gro");
        ItemDefinition item = new ItemDefinition(
                key,
                "fertilizers",
                "minecraft:bone_meal",
                101,
                "<green>Speed-Gro</green>",
                List.of("<gray>Growth speed bonus</gray>"),
                Map.of("speed_multiplier", "1.5")
        );

        registry.register(item);

        Optional<ItemDefinition> result = registry.get(key);
        assertTrue(result.isPresent());
        assertEquals("fertilizers", result.get().category());
        assertEquals("minecraft:bone_meal", result.get().materialKey());
        assertEquals(101, result.get().customModelData());
        assertEquals(1, result.get().lore().size());
        assertEquals("1.5", result.get().pdcProperties().get("speed_multiplier"));
    }

    @Test
    @DisplayName("Query items by category")
    void testGetByCategory() {
        ItemDefinition seed1 = new ItemDefinition(
                Key.of("croppy", "wheat_seeds"),
                "seeds",
                "minecraft:wheat_seeds",
                0,
                "Wheat Seeds",
                List.of(),
                Map.of()
        );
        ItemDefinition seed2 = new ItemDefinition(
                Key.of("croppy", "tomato_seeds"),
                "seeds",
                "minecraft:pumpkin_seeds",
                0,
                "Tomato Seeds",
                List.of(),
                Map.of()
        );
        ItemDefinition fert = new ItemDefinition(
                Key.of("croppy", "speed_gro"),
                "fertilizers",
                "minecraft:bone_meal",
                0,
                "Speed-Gro",
                List.of(),
                Map.of()
        );

        registry.register(seed1);
        registry.register(seed2);
        registry.register(fert);

        Collection<ItemDefinition> seeds = registry.getByCategory("seeds");
        assertEquals(2, seeds.size());

        Collection<ItemDefinition> fertilizers = registry.getByCategory("fertilizers");
        assertEquals(1, fertilizers.size());

        Collection<ItemDefinition> unknown = registry.getByCategory("unknown");
        assertTrue(unknown.isEmpty());

        assertEquals(2, registry.getCategories().size());
        assertTrue(registry.getCategories().contains("seeds"));
        assertTrue(registry.getCategories().contains("fertilizers"));
    }

    @Test
    @DisplayName("Reject duplicate keys on registration")
    void testDuplicateRegistration() {
        Key key = Key.of("croppy", "item_a");
        ItemDefinition item1 = new ItemDefinition(key, "tools", "minecraft:stick", 0, "Tool", List.of(), Map.of());
        ItemDefinition item2 = new ItemDefinition(key, "tools", "minecraft:blaze_rod", 0, "Tool 2", List.of(), Map.of());

        registry.register(item1);
        assertThrows(IllegalStateException.class, () -> registry.register(item2));
    }

    @Test
    @DisplayName("Clear removes all entries and categories")
    void testClear() {
        registry.register(new ItemDefinition(Key.of("croppy", "can"), "watering_cans", "minecraft:bucket", 0, "Can", List.of(), Map.of()));
        assertEquals(1, registry.size());
        assertEquals(1, registry.getCategories().size());

        registry.clear();
        assertEquals(0, registry.size());
        assertTrue(registry.getCategories().isEmpty());
        assertTrue(registry.getByCategory("watering_cans").isEmpty());
    }
}
