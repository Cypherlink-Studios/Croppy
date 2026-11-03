package dev.darkblade.croppy.api.species;

import java.util.Objects;

/**
 * Visual backing configuration for a crop species.
 *
 * @param type display type (e.g. "vanilla" or "model_3d")
 * @param blockId identifier of the base/support block (e.g. "minecraft:wheat" or "minecraft:air")
 * @param seedItemId identifier of the triggering seed/planting item (e.g. "minecraft:wheat_seeds")
 * @param rotationMode orientation mode for 3D placement rotation
 */
public record CropDisplay(String type, String blockId, String seedItemId, RotationMode rotationMode) {
    public CropDisplay {
        Objects.requireNonNull(type, "Display type cannot be null");
        Objects.requireNonNull(blockId, "Display blockId cannot be null");
        Objects.requireNonNull(seedItemId, "Display seedItemId cannot be null");
        if (rotationMode == null) {
            rotationMode = RotationMode.CARDINAL_4;
        }
    }

    public CropDisplay(String type, String blockId, String seedItemId) {
        this(type, blockId, seedItemId, RotationMode.CARDINAL_4);
    }
}
