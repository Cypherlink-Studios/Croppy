package dev.darkblade.croppy.core.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.crafting.CraftingType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Default in-memory implementation of {@link CraftingRecipeRegistry}.
 */
public final class DefaultCraftingRecipeRegistry extends SimpleRegistry<CraftingRecipe> implements CraftingRecipeRegistry {

    @Override
    public List<CraftingRecipe> findByResult(Key resultItemKey) {
        Objects.requireNonNull(resultItemKey, "resultItemKey cannot be null");
        return values().stream()
                .filter(r -> r.resultItemKey().equals(resultItemKey))
                .toList();
    }

    @Override
    public List<CraftingRecipe> findByType(CraftingType type) {
        Objects.requireNonNull(type, "type cannot be null");
        return values().stream()
                .filter(r -> r.type() == type)
                .toList();
    }
}
