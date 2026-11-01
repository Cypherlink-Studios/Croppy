package dev.darkblade.croppy.api.station.visual;

import java.util.Optional;

/**
 * Immutable definition configuring the visual presentation layers for a crafting station.
 *
 * @param baseModel      optional base 3D model display layer
 * @param processDisplay optional in-process item display layer
 * @param textDisplay    optional status text hologram display layer
 */
public record StationVisualDefinition(
        DisplayLayerConfig baseModel,
        DisplayLayerConfig processDisplay,
        TextDisplayTemplate textDisplay
) {

    public Optional<DisplayLayerConfig> getBaseModel() {
        return Optional.ofNullable(baseModel);
    }

    public Optional<DisplayLayerConfig> getProcessDisplay() {
        return Optional.ofNullable(processDisplay);
    }

    public Optional<TextDisplayTemplate> getTextDisplay() {
        return Optional.ofNullable(textDisplay);
    }
}
