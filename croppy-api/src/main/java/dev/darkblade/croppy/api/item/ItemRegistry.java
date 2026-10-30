package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

import java.util.Collection;
import java.util.Set;

/**
 * Registry managing all loaded custom {@link ItemDefinition}s grouped by category.
 */
public interface ItemRegistry extends Registry<ItemDefinition>, Service {

    /**
     * Retrieves all items registered under the specified category.
     *
     * @param category category identifier (e.g. "seeds", "fertilizers")
     * @return unmodifiable collection of items in this category
     */
    Collection<ItemDefinition> getByCategory(String category);

    /**
     * Returns all registered categories.
     *
     * @return unmodifiable set of category names
     */
    Set<String> getCategories();

    /**
     * Clears all registered item definitions.
     */
    void clear();
}
