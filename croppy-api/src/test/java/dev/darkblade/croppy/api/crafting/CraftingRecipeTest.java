package dev.darkblade.croppy.api.crafting;

import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeTest {

    @Test
    @DisplayName("Valid shaped recipe constructs and validates correctly")
    void validShapedRecipe() {
        Key key = Key.of("croppy", "cooking_pot");
        Key resultKey = Key.of("croppy", "cooking_pot");
        List<String> shape = List.of(
                "I I",
                "ICI",
                " I "
        );
        Map<Character, IngredientChoice> ingredients = Map.of(
                'I', new IngredientChoice("minecraft:iron_ingot"),
                'C', new IngredientChoice("minecraft:cauldron")
        );

        CraftingRecipe recipe = CraftingRecipe.shaped(
                key,
                "croppy_stations",
                "misc",
                resultKey,
                1,
                shape,
                ingredients
        );

        assertEquals(key, recipe.getKey());
        assertEquals(CraftingType.SHAPED, recipe.type());
        assertEquals("croppy_stations", recipe.group());
        assertEquals("misc", recipe.category());
        assertEquals(resultKey, recipe.resultItemKey());
        assertEquals(1, recipe.resultAmount());
        assertEquals(3, recipe.shape().size());
        assertEquals(2, recipe.shapedIngredients().size());
        assertTrue(recipe.shapelessIngredients().isEmpty());
    }

    @Test
    @DisplayName("Valid shapeless recipe constructs correctly")
    void validShapelessRecipe() {
        Key key = Key.of("croppy", "growth_fertilizer");
        Key resultKey = Key.of("croppy", "growth_fertilizer");
        List<IngredientChoice> ingredients = List.of(
                new IngredientChoice("minecraft:bone_meal", 1),
                new IngredientChoice("minecraft:wheat", 2)
        );

        CraftingRecipe recipe = CraftingRecipe.shapeless(
                key,
                "croppy_fertilizers",
                "misc",
                resultKey,
                2,
                ingredients
        );

        assertEquals(key, recipe.getKey());
        assertEquals(CraftingType.SHAPELESS, recipe.type());
        assertEquals(2, recipe.resultAmount());
        assertEquals(2, recipe.shapelessIngredients().size());
        assertTrue(recipe.shape().isEmpty());
    }

    @Test
    @DisplayName("Shaped recipe rejects unmapped character in shape")
    void shapedRejectsUnmappedChar() {
        Key key = Key.of("croppy", "test");
        List<String> shape = List.of(" A ");
        Map<Character, IngredientChoice> ingredients = Map.of(); // 'A' missing

        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shaped(key, null, null, key, 1, shape, ingredients)
        );
    }

    @Test
    @DisplayName("Shaped recipe rejects irregular row widths")
    void shapedRejectsIrregularRows() {
        Key key = Key.of("croppy", "test");
        List<String> shape = List.of("AA", "A");
        Map<Character, IngredientChoice> ingredients = Map.of('A', new IngredientChoice("minecraft:stone"));

        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shaped(key, null, null, key, 1, shape, ingredients)
        );
    }

    @Test
    @DisplayName("Shaped recipe rejects dimensions exceeding 3x3")
    void shapedRejectsExceedingDimensions() {
        Key key = Key.of("croppy", "test");
        List<String> shapeTooWide = List.of("AAAA");
        Map<Character, IngredientChoice> ingredients = Map.of('A', new IngredientChoice("minecraft:stone"));

        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shaped(key, null, null, key, 1, shapeTooWide, ingredients)
        );

        List<String> shapeTooTall = List.of("A", "A", "A", "A");
        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shaped(key, null, null, key, 1, shapeTooTall, ingredients)
        );
    }

    @Test
    @DisplayName("Shapeless recipe rejects empty ingredient list or more than 9 items")
    void shapelessValidatesCapacity() {
        Key key = Key.of("croppy", "test");

        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shapeless(key, null, null, key, 1, List.of())
        );

        List<IngredientChoice> tooMany = List.of(
                new IngredientChoice("minecraft:dirt", 10)
        );
        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shapeless(key, null, null, key, 1, tooMany)
        );
    }

    @Test
    @DisplayName("Non-positive resultAmount throws exception")
    void nonPositiveResultAmountThrows() {
        Key key = Key.of("croppy", "test");
        assertThrows(IllegalArgumentException.class, () ->
                CraftingRecipe.shapeless(key, null, null, key, 0, List.of(new IngredientChoice("minecraft:stone")))
        );
    }
}
