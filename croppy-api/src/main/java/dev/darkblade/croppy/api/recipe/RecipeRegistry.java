package dev.darkblade.croppy.api.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

import java.util.List;

/**
 * Registry containing all registered {@link RecipeDefinition} instances.
 */
public interface RecipeRegistry extends Registry<RecipeDefinition>, Service {

    /**
     * Finds all recipes associated with a specific station definition key.
     *
     * @param stationKey station definition key
     * @return list of recipes usable at that station
     */
    List<RecipeDefinition> findByStation(Key stationKey);

    /**
     * Finds all recipes assigned to a specific recipe category.
     *
     * @param categoryKey category key
     * @return list of recipes in that category
     */
    List<RecipeDefinition> findByCategory(Key categoryKey);
}
