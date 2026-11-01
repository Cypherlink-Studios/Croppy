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

import static org.junit.jupiter.api.Assertions.*;

class DefaultRecipeRegistryTest {

    private DefaultRecipeRegistry registry;
    private RecipeDefinition soupRecipe;
    private RecipeDefinition breadRecipe;

    @BeforeEach
    void setUp() {
        registry = new DefaultRecipeRegistry();

        soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "<yellow>Tomato Soup</yellow>",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "tomato_soup_process"),
                List.of(StoredItem.of(Key.of("croppy", "tomato"), 2)),
                Key.of("croppy", "tomato_soup"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(Key.of("croppy", "experimental")),
                Map.of()
        );

        breadRecipe = new RecipeDefinition(
                Key.of("croppy", "artisan_bread"),
                "<yellow>Artisan Bread</yellow>",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "oven"),
                Key.of("croppy", "artisan_bread_process"),
                List.of(StoredItem.of(Key.of("croppy", "flour"), 3)),
                Key.of("croppy", "artisan_bread"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(Key.of("croppy", "automatic")),
                Map.of()
        );
    }

    @Test
    @DisplayName("Registers and retrieves recipes correctly")
    void testRegisterAndGet() {
        registry.register(soupRecipe);
        registry.register(breadRecipe);

        assertEquals(2, registry.size());
        assertTrue(registry.contains(soupRecipe.getKey()));
        assertEquals(soupRecipe, registry.get(soupRecipe.getKey()).orElseThrow());
    }

    @Test
    @DisplayName("Throws exception on duplicate registration")
    void testDuplicateRegistration() {
        registry.register(soupRecipe);
        assertThrows(IllegalStateException.class, () -> registry.register(soupRecipe));
    }

    @Test
    @DisplayName("Filters recipes by station")
    void testFindByStation() {
        registry.register(soupRecipe);
        registry.register(breadRecipe);

        List<RecipeDefinition> potRecipes = registry.findByStation(Key.of("croppy", "cooking_pot"));
        assertEquals(1, potRecipes.size());
        assertEquals(soupRecipe.getKey(), potRecipes.get(0).getKey());

        List<RecipeDefinition> skilletRecipes = registry.findByStation(Key.of("croppy", "skillet"));
        assertTrue(skilletRecipes.isEmpty());
    }

    @Test
    @DisplayName("Filters recipes by category")
    void testFindByCategory() {
        registry.register(soupRecipe);
        registry.register(breadRecipe);

        List<RecipeDefinition> soupCategory = registry.findByCategory(RecipeCategory.SOUPS.getKey());
        assertEquals(1, soupCategory.size());
        assertEquals(soupRecipe.getKey(), soupCategory.get(0).getKey());

        List<RecipeDefinition> allCategory = registry.findByCategory(RecipeCategory.ALL.getKey());
        assertEquals(2, allCategory.size());
    }
}
