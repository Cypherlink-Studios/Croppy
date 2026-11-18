package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Central registry managing {@link ItemMetadataContributor} instances.
 */
public interface ItemMetadataRegistry extends Registry<ItemMetadataContributor>, Service {
}
