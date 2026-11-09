package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRecipeCategoryRegistryTest {

    private DefaultRecipeCategoryRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultRecipeCategoryRegistry();
    }

    @Test
    @DisplayName("Default categories are pre-registered on instantiation")
    void testDefaultCategoriesRegistered() {
        assertEquals(6, registry.size());
        assertTrue(registry.contains(RecipeCategory.ALL.getKey()));
        assertTrue(registry.contains(RecipeCategory.BAKERY.getKey()));
        assertTrue(registry.contains(RecipeCategory.SOUPS.getKey()));
        assertTrue(registry.contains(RecipeCategory.COOKING.getKey()));
        assertTrue(registry.contains(RecipeCategory.BEVERAGES.getKey()));
        assertTrue(registry.contains(RecipeCategory.PRESERVES.getKey()));
    }

    @Test
    @DisplayName("Can register and retrieve custom addon category")
    void testRegisterCustomCategory() {
        Key coffeeCatKey = Key.of("croppy_coffee", "brewing");
        RecipeCategory coffeeCategory = new RecipeCategory(
                coffeeCatKey,
                "<gradient:#e6b89c:#6f4e37>Coffee Brewing</gradient>",
                Key.of("croppy_coffee", "espresso")
        );

        registry.register(coffeeCategory);

        assertEquals(7, registry.size());
        assertTrue(registry.contains(coffeeCatKey));
        assertEquals(coffeeCategory, registry.get(coffeeCatKey).orElse(null));
    }

    @Test
    @DisplayName("Can unregister category cleanly")
    void testUnregisterCategory() {
        Key customKey = Key.of("custom", "fermenting");
        RecipeCategory customCategory = new RecipeCategory(
                customKey,
                "Fermenting",
                Key.of("minecraft", "barrel")
        );

        registry.register(customCategory);
        assertTrue(registry.contains(customKey));

        registry.unregister(customKey);
        assertFalse(registry.contains(customKey));
        assertEquals(6, registry.size());
    }
}
