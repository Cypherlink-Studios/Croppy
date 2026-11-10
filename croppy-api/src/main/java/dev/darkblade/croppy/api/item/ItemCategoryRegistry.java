package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Registry containing all registered {@link ItemCategory} instances.
 */
public interface ItemCategoryRegistry extends Registry<ItemCategory>, Service {

    /**
     * Registers default built-in item categories.
     */
    void registerDefaults();
}
