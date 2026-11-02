package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemDefinitionTest {

    @Test
    @DisplayName("Canonical constructor retains native itemModel and accessors work")
    void testCanonicalConstructorWithItemModel() {
        Key itemKey = Key.of("croppy", "copper_can");
        Key modelKey = Key.of("croppy", "copper_can");

        ItemDefinition def = new ItemDefinition(
                itemKey,
                "watering_cans",
                "minecraft:copper_ingot",
                2002,
                modelKey,
                "<red>Copper Can</red>",
                List.of("<gray>Description</gray>"),
                Map.of("capacity", "40")
        );

        assertEquals(itemKey, def.key());
        assertEquals(itemKey, def.getKey());
        assertEquals("watering_cans", def.category());
        assertEquals("minecraft:copper_ingot", def.materialKey());
        assertEquals(2002, def.customModelData());
        assertEquals(modelKey, def.itemModel());
        assertTrue(def.getItemModel().isPresent());
        assertEquals(modelKey, def.getItemModel().get());
        assertEquals("<red>Copper Can</red>", def.displayName());
        assertEquals(1, def.lore().size());
        assertEquals("40", def.pdcProperties().get("capacity"));
    }

    @Test
    @DisplayName("Backward-compatible 7-arg constructor defaults itemModel to null")
    void testBackwardCompatibleConstructor() {
        Key itemKey = Key.of("croppy", "speed_gro");

        ItemDefinition def = new ItemDefinition(
                itemKey,
                "fertilizers",
                "minecraft:bone_meal",
                101,
                "<green>Speed-Gro</green>",
                List.of("<gray>Booster</gray>"),
                Map.of("speed", "1.2")
        );

        assertEquals(itemKey, def.key());
        assertNull(def.itemModel());
        assertTrue(def.getItemModel().isEmpty());
        assertEquals(101, def.customModelData());
        assertEquals("<green>Speed-Gro</green>", def.displayName());
    }

    @Test
    @DisplayName("Null checks and collection defaults")
    void testNullChecksAndDefaults() {
        Key itemKey = Key.of("croppy", "test_item");

        assertThrows(NullPointerException.class, () -> new ItemDefinition(
                null, "category", "material", 0, null, null, null, null
        ));

        assertThrows(NullPointerException.class, () -> new ItemDefinition(
                itemKey, null, "material", 0, null, null, null, null
        ));

        assertThrows(NullPointerException.class, () -> new ItemDefinition(
                itemKey, "category", null, 0, null, null, null, null
        ));

        ItemDefinition def = new ItemDefinition(
                itemKey, "category", "material", 0, null, "Name", null, null
        );

        assertNotNull(def.lore());
        assertTrue(def.lore().isEmpty());
        assertNotNull(def.pdcProperties());
        assertTrue(def.pdcProperties().isEmpty());
        assertNull(def.itemModel());
        assertTrue(def.getItemModel().isEmpty());
    }
}
