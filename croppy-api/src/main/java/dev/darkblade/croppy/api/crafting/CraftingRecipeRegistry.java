package dev.darkblade.croppy.api.crafting;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

import java.util.List;

/**
 * Registry containing all registered {@link CraftingRecipe} definitions for crafting tables.
 */
public interface CraftingRecipeRegistry extends Registry<CraftingRecipe>, Service {

    /**
     * Finds all crafting recipes that produce a specific item key.
     *
     * @param resultItemKey identifier of the resulting item
     * @return list of recipes producing that item
     */
    List<CraftingRecipe> findByResult(Key resultItemKey);

    /**
     * Finds all crafting recipes of a given crafting type.
     *
     * @param type crafting type (shaped or shapeless)
     * @return list of matching recipes
     */
    List<CraftingRecipe> findByType(CraftingType type);
}
