package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.item.ItemCategory;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Default in-memory implementation of {@link ItemCategoryRegistry}.
 * Pre-populates standard item categories.
 */
public final class DefaultItemCategoryRegistry extends SimpleRegistry<ItemCategory> implements ItemCategoryRegistry {

    public DefaultItemCategoryRegistry() {
        registerDefaults();
    }

    /**
     * Registers default built-in item categories.
     */
    public void registerDefaults() {
        register(ItemCategory.ALL);
        register(ItemCategory.SEEDS);
        register(ItemCategory.FERTILIZERS);
        register(ItemCategory.WATERING_CANS);
        register(ItemCategory.TOOLS);
        register(ItemCategory.INGREDIENTS);
        register(ItemCategory.STATIONS);
        register(ItemCategory.MISC);
    }
}
