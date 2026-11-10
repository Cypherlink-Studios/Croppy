package dev.darkblade.croppy.api.station.visual;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Optional;

/**
 * Immutable specification for overriding a station's base 3D model, custom model data, and spatial transforms.
 *
 * @param itemModel       optional native item_model identifier (Minecraft 1.21.4+)
 * @param customModelData optional custom model data integer (0 if disabled)
 * @param transform       optional spatial transform offsets, scale, and base rotation (null to keep base transform)
 */
public record StationModelOverride(
        Key itemModel,
        int customModelData,
        DisplayTransform transform
) {

    public StationModelOverride {
        customModelData = Math.max(0, customModelData);
    }

    public StationModelOverride(Key itemModel) {
        this(itemModel, 0, null);
    }

    public StationModelOverride(Key itemModel, DisplayTransform transform) {
        this(itemModel, 0, transform);
    }

    public StationModelOverride(DisplayTransform transform) {
        this(null, 0, transform);
    }

    public Optional<Key> getItemModel() {
        return Optional.ofNullable(itemModel);
    }

    public Optional<DisplayTransform> getTransform() {
        return Optional.ofNullable(transform);
    }

    public boolean hasCustomModelData() {
        return customModelData > 0;
    }

    public boolean hasTransform() {
        return transform != null;
    }

    public DisplayTransform getEffectiveTransform(DisplayTransform baseTransform) {
        if (transform != null) {
            return transform;
        }
        return (baseTransform != null) ? baseTransform : DisplayTransform.DEFAULT;
    }
}
