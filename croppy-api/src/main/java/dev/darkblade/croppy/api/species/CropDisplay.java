package dev.darkblade.croppy.api.species;

import java.util.Objects;

/**
 * Visual backing configuration for a crop species.
 *
 * @param type display type (e.g. "vanilla" or "custom")
 * @param blockId identifier of the block (e.g. "minecraft:wheat")
 * @param seedItemId identifier of the triggering seed/planting item (e.g. "minecraft:wheat_seeds")
 */
public record CropDisplay(String type, String blockId, String seedItemId) {
    public CropDisplay {
        Objects.requireNonNull(type, "Display type cannot be null");
        Objects.requireNonNull(blockId, "Display blockId cannot be null");
        Objects.requireNonNull(seedItemId, "Display seedItemId cannot be null");
    }
}
