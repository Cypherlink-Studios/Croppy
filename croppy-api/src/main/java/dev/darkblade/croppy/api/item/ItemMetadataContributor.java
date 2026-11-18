package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;
import java.util.Map;

/**
 * Platform-independent contributor bridging domain {@link StoredItem} metadata
 * (such as batch identifiers or custom properties) to item persistent data tags and MiniMessage lore.
 */
public interface ItemMetadataContributor extends Keyed {

    /**
     * Returns whether this contributor applies to the given {@link StoredItem}.
     *
     * @param item stored item
     * @return true if this contributor should serialize/format metadata for the item
     */
    boolean supports(StoredItem item);

    /**
     * Serializes domain properties from {@code item} into string key-value pairs
     * to be stored in platform persistent data. Keys should be namespaced (e.g. {@code "brewery:batch_id"}).
     *
     * @param item stored item
     * @return map of namespaced string keys to string values
     */
    default Map<String, String> serializeMetadata(StoredItem item) {
        return Map.of();
    }

    /**
     * Formats optional MiniMessage lore lines to append when constructing the item stack.
     *
     * @param item stored item
     * @return MiniMessage lore lines
     */
    default List<String> formatLore(StoredItem item) {
        return List.of();
    }

    /**
     * Extracts domain properties from platform persistent data tags when reconstructing a {@link StoredItem}.
     *
     * @param itemKey item key
     * @param pdcTags namespaced string tags present on the item
     * @return extracted property entries to merge into {@link StoredItem#properties()}
     */
    default Map<String, Object> deserializeMetadata(dev.darkblade.croppy.api.identifier.Key itemKey, Map<String, String> pdcTags) {
        return Map.of();
    }
}
