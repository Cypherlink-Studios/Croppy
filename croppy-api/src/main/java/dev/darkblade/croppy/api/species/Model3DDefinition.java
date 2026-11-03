package dev.darkblade.croppy.api.species;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Objects;

/**
 * Immutable definition of a 3D visual model and its interaction hitbox.
 *
 * @param itemModel      the key identifying the item_model in the resource pack
 * @param scaleX         horizontal X scale factor
 * @param scaleY         vertical Y scale factor
 * @param scaleZ         horizontal Z scale factor
 * @param translationX   horizontal X translation offset
 * @param translationY   vertical Y translation offset
 * @param translationZ   horizontal Z translation offset
 * @param hitboxWidth    width of the virtual interaction hitbox
 * @param hitboxHeight   height of the virtual interaction hitbox
 */
public record Model3DDefinition(
        Key itemModel,
        float scaleX,
        float scaleY,
        float scaleZ,
        float translationX,
        float translationY,
        float translationZ,
        float hitboxWidth,
        float hitboxHeight
) {
    public Model3DDefinition {
        Objects.requireNonNull(itemModel, "itemModel must not be null");
        if (hitboxWidth <= 0.0f) {
            throw new IllegalArgumentException("hitboxWidth must be greater than 0");
        }
        if (hitboxHeight <= 0.0f) {
            throw new IllegalArgumentException("hitboxHeight must be greater than 0");
        }
    }

    public static Model3DDefinition of(Key itemModel, float hitboxWidth, float hitboxHeight) {
        return new Model3DDefinition(itemModel, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, hitboxWidth, hitboxHeight);
    }
}
