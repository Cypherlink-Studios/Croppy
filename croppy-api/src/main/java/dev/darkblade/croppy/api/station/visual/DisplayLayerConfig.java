package dev.darkblade.croppy.api.station.visual;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for a discrete visual layer (base model or in-process item display).
 *
 * @param enabled               whether this visual layer is active
 * @param type                  renderer type ("ITEM" or "BLOCK")
 * @param itemKey               optional custom item identifier (for Item Display)
 * @param blockState            optional block state string (for Block Display)
 * @param transform             spatial offsets, scale, and base rotation
 * @param billboard             camera tracking orientation
 * @param viewDistance          maximum viewing radius before culling
 * @param interpolationDuration client-side interpolation duration in ticks
 */
public record DisplayLayerConfig(
        boolean enabled,
        String type,
        Key itemKey,
        String blockState,
        DisplayTransform transform,
        BillboardMode billboard,
        int viewDistance,
        int interpolationDuration
) {

    public DisplayLayerConfig {
        type = (type == null || type.isBlank()) ? "ITEM" : type.toUpperCase(java.util.Locale.ROOT);
        transform = (transform == null) ? DisplayTransform.DEFAULT : transform;
        billboard = (billboard == null) ? BillboardMode.FIXED : billboard;
        viewDistance = (viewDistance <= 0) ? 24 : viewDistance;
        interpolationDuration = Math.max(0, interpolationDuration);
    }

    public Optional<Key> getItemKey() {
        return Optional.ofNullable(itemKey);
    }

    public Optional<String> getBlockState() {
        return Optional.ofNullable(blockState);
    }
}
