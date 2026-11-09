package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Default in-memory implementation of {@link RecipeCategoryRegistry}.
 * Pre-populates standard culinary recipe categories.
 */
public final class DefaultRecipeCategoryRegistry extends SimpleRegistry<RecipeCategory> implements RecipeCategoryRegistry {

    public DefaultRecipeCategoryRegistry() {
        registerDefaults();
    }

    /**
     * Registers default built-in categories.
     */
    public void registerDefaults() {
        register(RecipeCategory.ALL);
        register(RecipeCategory.BAKERY);
        register(RecipeCategory.SOUPS);
        register(RecipeCategory.COOKING);
        register(RecipeCategory.BEVERAGES);
        register(RecipeCategory.PRESERVES);
    }
}
