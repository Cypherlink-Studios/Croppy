package dev.darkblade.croppy.core.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.CraftingType;
import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCraftingRecipeRegistryTest {

    private DefaultCraftingRecipeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultCraftingRecipeRegistry();
    }

    @Test
    @DisplayName("Register and retrieve shaped and shapeless recipes")
    void registerAndRetrieve() {
        CraftingRecipe shaped = CraftingRecipe.shaped(
                Key.of("croppy", "cooking_pot"),
                "stations",
                "misc",
                Key.of("croppy", "cooking_pot"),
                1,
                List.of(" I ", "ICI", " I "),
                Map.of(
                        'I', new IngredientChoice("minecraft:iron_ingot"),
                        'C', new IngredientChoice("minecraft:cauldron")
                )
        );

        CraftingRecipe shapeless = CraftingRecipe.shapeless(
                Key.of("croppy", "growth_fertilizer"),
                "fertilizers",
                "misc",
                Key.of("croppy", "growth_fertilizer"),
                2,
                List.of(new IngredientChoice("minecraft:bone_meal", 1))
        );

        registry.register(shaped);
        registry.register(shapeless);

        assertEquals(2, registry.size());
        assertTrue(registry.contains(shaped.getKey()));
        assertTrue(registry.contains(shapeless.getKey()));
        assertEquals(shaped, registry.get(shaped.getKey()).orElseThrow());
        assertEquals(shapeless, registry.get(shapeless.getKey()).orElseThrow());

        assertEquals(1, registry.findByResult(Key.of("croppy", "cooking_pot")).size());
        assertEquals(1, registry.findByType(CraftingType.SHAPED).size());
        assertEquals(1, registry.findByType(CraftingType.SHAPELESS).size());
    }

    @Test
    @DisplayName("Duplicate registration throws IllegalStateException")
    void duplicateRegistrationThrows() {
        CraftingRecipe recipe = CraftingRecipe.shapeless(
                Key.of("croppy", "test"),
                null,
                null,
                Key.of("croppy", "test"),
                1,
                List.of(new IngredientChoice("minecraft:stone"))
        );

        registry.register(recipe);
        assertThrows(IllegalStateException.class, () -> registry.register(recipe));
    }

    @Test
    @DisplayName("Unregister removes recipe cleanly")
    void unregister() {
        CraftingRecipe recipe = CraftingRecipe.shapeless(
                Key.of("croppy", "test"),
                null,
                null,
                Key.of("croppy", "test"),
                1,
                List.of(new IngredientChoice("minecraft:stone"))
        );

        registry.register(recipe);
        assertTrue(registry.contains(recipe.getKey()));

        boolean removed = registry.unregister(recipe.getKey());
        assertTrue(removed);
        assertFalse(registry.contains(recipe.getKey()));
        assertEquals(0, registry.size());
    }
}
