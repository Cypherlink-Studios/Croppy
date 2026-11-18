package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.item.ItemMetadataContributor;
import dev.darkblade.croppy.api.item.ItemMetadataRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Default thread-safe registry for {@link ItemMetadataContributor} implementations.
 */
public class DefaultItemMetadataRegistry extends SimpleRegistry<ItemMetadataContributor> implements ItemMetadataRegistry {
}
