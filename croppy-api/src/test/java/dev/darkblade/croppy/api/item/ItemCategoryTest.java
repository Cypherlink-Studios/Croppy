package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemCategoryTest {

    @Test
    @DisplayName("Canonical constructor retains all fields and accessors work")
    void testCanonicalConstructor() {
        Key key = Key.of("croppy", "watering_cans");
        Key model = Key.of("croppy", "copper_can");

        ItemCategory category = new ItemCategory(
                key,
                "minecraft:copper_ingot",
                model,
                "<blue>Watering Cans</blue>",
                30
        );

        assertEquals(key, category.key());
        assertEquals(key, category.getKey());
        assertEquals("minecraft:copper_ingot", category.iconMaterial());
        assertEquals(model, category.iconItemModel());
        assertTrue(category.getIconItemModel().isPresent());
        assertEquals(model, category.getIconItemModel().get());
        assertEquals("<blue>Watering Cans</blue>", category.displayName());
        assertEquals(30, category.priority());
    }

    @Test
    @DisplayName("Overloaded 4-arg constructor sets iconItemModel to null")
    void testOverloadedConstructor() {
        Key key = Key.of("croppy", "seeds");
        ItemCategory category = new ItemCategory(
                key,
                "minecraft:wheat_seeds",
                "<green>Seeds</green>",
                10
        );

        assertEquals(key, category.key());
        assertEquals("minecraft:wheat_seeds", category.iconMaterial());
        assertNull(category.iconItemModel());
        assertTrue(category.getIconItemModel().isEmpty());
        assertEquals(10, category.priority());
    }

    @Test
    @DisplayName("Priority and Key sorting in compareTo")
    void testCompareTo() {
        ItemCategory c1 = new ItemCategory(Key.of("croppy", "b"), "mat", "name", 10);
        ItemCategory c2 = new ItemCategory(Key.of("croppy", "a"), "mat", "name", 20);
        ItemCategory c3 = new ItemCategory(Key.of("croppy", "a"), "mat", "name", 10);

        assertTrue(c1.compareTo(c2) < 0, "Lower priority should come first");
        assertTrue(c2.compareTo(c1) > 0, "Higher priority should come after");
        assertTrue(c3.compareTo(c1) < 0, "Same priority should break tie by Key alphabetically");
    }

    @Test
    @DisplayName("Null validation throws NullPointerException")
    void testNullValidation() {
        assertThrows(NullPointerException.class, () -> new ItemCategory(null, "mat", "name", 0));
        assertThrows(NullPointerException.class, () -> new ItemCategory(Key.of("c", "k"), null, "name", 0));
        assertThrows(NullPointerException.class, () -> new ItemCategory(Key.of("c", "k"), "mat", null, 0));
    }

    @Test
    @DisplayName("Pre-registered default constants exist and have correct keys")
    void testDefaultConstants() {
        assertEquals("all", ItemCategory.ALL.key().value());
        assertEquals("seeds", ItemCategory.SEEDS.key().value());
        assertEquals("fertilizers", ItemCategory.FERTILIZERS.key().value());
        assertEquals("watering_cans", ItemCategory.WATERING_CANS.key().value());
        assertEquals("tools", ItemCategory.TOOLS.key().value());
        assertEquals("ingredients", ItemCategory.INGREDIENTS.key().value());
        assertEquals("stations", ItemCategory.STATIONS.key().value());
        assertEquals("misc", ItemCategory.MISC.key().value());
    }
}
