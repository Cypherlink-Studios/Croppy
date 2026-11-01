package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RecipeMatcherTest {

    private RecipeMatcher matcher;
    private RecipeDefinition pastaRecipe;
    private RecipeDefinition cakeRecipe;
    private RecipeDefinition excessAllowedRecipe;

    @BeforeEach
    void setUp() {
        matcher = new RecipeMatcher();

        // 2 ingredients: 1 flour + 1 egg (strict mode)
        pastaRecipe = new RecipeDefinition(
                Key.of("croppy", "pasta"),
                "<yellow>Fresh Pasta</yellow>",
                RecipeCategory.COOKING.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "pasta_process"),
                List.of(
                        StoredItem.of(Key.of("croppy", "flour"), 1),
                        StoredItem.of(Key.of("croppy", "egg"), 1)
                ),
                Key.of("croppy", "pasta"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        // 3 ingredients: 1 flour + 1 egg + 1 sugar (strict mode)
        cakeRecipe = new RecipeDefinition(
                Key.of("croppy", "cake_batter"),
                "<yellow>Cake Batter</yellow>",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "cake_process"),
                List.of(
                        StoredItem.of(Key.of("croppy", "flour"), 1),
                        StoredItem.of(Key.of("croppy", "egg"), 1),
                        StoredItem.of(Key.of("croppy", "sugar"), 1)
                ),
                Key.of("croppy", "cake_batter"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        // 1 ingredient: 1 potato (allows excess items)
        excessAllowedRecipe = new RecipeDefinition(
                Key.of("croppy", "mashed_potatoes"),
                "<yellow>Mashed Potatoes</yellow>",
                RecipeCategory.COOKING.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "mash_process"),
                List.of(StoredItem.of(Key.of("croppy", "potato"), 1)),
                Key.of("croppy", "mashed_potatoes"),
                1,
                HintDefinition.DEFAULT,
                true,
                List.of(),
                Map.of()
        );
    }

    @Test
    @DisplayName("Matches exact ingredients in strict mode")
    void testExactMatch() {
        List<StoredItem> inputs = List.of(
                StoredItem.of(Key.of("croppy", "flour"), 1),
                StoredItem.of(Key.of("croppy", "egg"), 1)
        );

        Optional<RecipeDefinition> match = matcher.findMatch(List.of(pastaRecipe, cakeRecipe), inputs);
        assertTrue(match.isPresent());
        assertEquals(pastaRecipe.getKey(), match.get().getKey());
    }

    @Test
    @DisplayName("Prevents subset shadowing: Cake matches over Pasta when Sugar is present")
    void testAntiShadowingSubsetPrecedence() {
        // Player inserts Flour, Egg, and Sugar
        List<StoredItem> inputs = List.of(
                StoredItem.of(Key.of("croppy", "flour"), 1),
                StoredItem.of(Key.of("croppy", "egg"), 1),
                StoredItem.of(Key.of("croppy", "sugar"), 1)
        );

        // Both recipes passed as candidates. Pasta MUST NOT shadow Cake.
        Optional<RecipeDefinition> match = matcher.findMatch(List.of(pastaRecipe, cakeRecipe), inputs);
        assertTrue(match.isPresent());
        assertEquals(cakeRecipe.getKey(), match.get().getKey(), "Cake recipe must match, not shadowed by pasta");
    }

    @Test
    @DisplayName("Strict mode rejects extraneous unlisted items")
    void testStrictRejectsExtraneous() {
        // Flour + Egg + Dirt
        List<StoredItem> inputs = List.of(
                StoredItem.of(Key.of("croppy", "flour"), 1),
                StoredItem.of(Key.of("croppy", "egg"), 1),
                StoredItem.of(Key.of("minecraft", "dirt"), 1)
        );

        Optional<RecipeDefinition> match = matcher.findMatch(List.of(pastaRecipe), inputs);
        assertTrue(match.isEmpty(), "Strict mode must reject extraneous dirt item");
    }

    @Test
    @DisplayName("Allows excess items when recipe explicitly permits it")
    void testAllowExcessInputs() {
        // Potato + Dirt (excessAllowedRecipe allows excess)
        List<StoredItem> inputs = List.of(
                StoredItem.of(Key.of("croppy", "potato"), 1),
                StoredItem.of(Key.of("minecraft", "dirt"), 1)
        );

        Optional<RecipeDefinition> match = matcher.findMatch(List.of(excessAllowedRecipe), inputs);
        assertTrue(match.isPresent());
        assertEquals(excessAllowedRecipe.getKey(), match.get().getKey());
    }

    @Test
    @DisplayName("Aggregates quantities across split stacks")
    void testAggregatedQuantities() {
        // Recipe requiring 3 flour
        RecipeDefinition bread = new RecipeDefinition(
                Key.of("croppy", "bread"),
                "Bread",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "oven"),
                Key.of("croppy", "bread_p"),
                List.of(StoredItem.of(Key.of("croppy", "flour"), 3)),
                Key.of("croppy", "bread"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        // Player puts 1 flour in slot A and 2 flour in slot B
        List<StoredItem> inputs = List.of(
                StoredItem.of(Key.of("croppy", "flour"), 1),
                StoredItem.of(Key.of("croppy", "flour"), 2)
        );

        assertTrue(matcher.isMatch(bread, inputs));
    }
}
