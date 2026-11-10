package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultItemCategoryRegistryTest {

    private DefaultItemCategoryRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultItemCategoryRegistry();
    }

    @Test
    @DisplayName("Default categories are pre-registered on instantiation")
    void testDefaultCategoriesRegistered() {
        assertEquals(8, registry.size());
        assertTrue(registry.contains(ItemCategory.ALL.getKey()));
        assertTrue(registry.contains(ItemCategory.SEEDS.getKey()));
        assertTrue(registry.contains(ItemCategory.FERTILIZERS.getKey()));
        assertTrue(registry.contains(ItemCategory.WATERING_CANS.getKey()));
        assertTrue(registry.contains(ItemCategory.TOOLS.getKey()));
        assertTrue(registry.contains(ItemCategory.INGREDIENTS.getKey()));
        assertTrue(registry.contains(ItemCategory.STATIONS.getKey()));
        assertTrue(registry.contains(ItemCategory.MISC.getKey()));
    }

    @Test
    @DisplayName("Can register and retrieve custom addon category")
    void testRegisterCustomCategory() {
        Key coffeeCatKey = Key.of("croppy_coffee", "coffee_beans");
        ItemCategory coffeeCategory = new ItemCategory(
                coffeeCatKey,
                "minecraft:cocoa_beans",
                Key.of("croppy_coffee", "coffee_cherry"),
                "<gradient:#e6b89c:#6f4e37>Coffee Beans</gradient>",
                80
        );

        registry.register(coffeeCategory);

        assertEquals(9, registry.size());
        assertTrue(registry.contains(coffeeCatKey));
        assertEquals(coffeeCategory, registry.get(coffeeCatKey).orElse(null));
    }

    @Test
    @DisplayName("Can unregister category cleanly")
    void testUnregisterCategory() {
        Key customKey = Key.of("custom", "herbs");
        ItemCategory customCategory = new ItemCategory(
                customKey,
                "minecraft:fern",
                "Herbs",
                90
        );

        registry.register(customCategory);
        assertTrue(registry.contains(customKey));

        registry.unregister(customKey);
        assertFalse(registry.contains(customKey));
        assertEquals(8, registry.size());
    }
}
