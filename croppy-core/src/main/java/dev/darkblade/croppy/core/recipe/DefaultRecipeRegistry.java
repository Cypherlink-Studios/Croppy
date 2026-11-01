package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Default in-memory implementation of {@link RecipeRegistry}.
 */
public final class DefaultRecipeRegistry extends SimpleRegistry<RecipeDefinition> implements RecipeRegistry {

    @Override
    public List<RecipeDefinition> findByStation(Key stationKey) {
        Objects.requireNonNull(stationKey, "stationKey cannot be null");
        return values().stream()
                .filter(r -> r.stationKey().equals(stationKey))
                .toList();
    }

    @Override
    public List<RecipeDefinition> findByCategory(Key categoryKey) {
        Objects.requireNonNull(categoryKey, "categoryKey cannot be null");
        if (categoryKey.equals(RecipeCategory.ALL.getKey())) {
            return List.copyOf(values());
        }
        return values().stream()
                .filter(r -> r.categoryKey().equals(categoryKey))
                .toList();
    }
}
