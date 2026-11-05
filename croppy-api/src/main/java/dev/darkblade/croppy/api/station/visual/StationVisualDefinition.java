package dev.darkblade.croppy.api.station.visual;

import java.util.Optional;

/**
 * Immutable definition configuring the visual presentation layers for a crafting station.
 *
 * @param baseModel      optional base 3D model display layer
 * @param processDisplay optional in-process item display layer
 * @param textDisplay    optional status text hologram display layer
 * @param hideBaseBlock  whether to conceal the physical base block from client view via packet masking
 * @param interactionBox optional bounding box for virtual interaction entity
 */
public record StationVisualDefinition(
        DisplayLayerConfig baseModel,
        DisplayLayerConfig processDisplay,
        TextDisplayTemplate textDisplay,
        boolean hideBaseBlock,
        StationInteractionBox interactionBox
) {

    /**
     * Backward-compatible constructor defaulting {@code hideBaseBlock} to {@code false} and {@code interactionBox} to {@code null}.
     */
    public StationVisualDefinition(
            DisplayLayerConfig baseModel,
            DisplayLayerConfig processDisplay,
            TextDisplayTemplate textDisplay
    ) {
        this(baseModel, processDisplay, textDisplay, false, null);
    }

    public Optional<DisplayLayerConfig> getBaseModel() {
        return Optional.ofNullable(baseModel);
    }

    public Optional<DisplayLayerConfig> getProcessDisplay() {
        return Optional.ofNullable(processDisplay);
    }

    public Optional<TextDisplayTemplate> getTextDisplay() {
        return Optional.ofNullable(textDisplay);
    }

    public Optional<StationInteractionBox> getInteractionBox() {
        return Optional.ofNullable(interactionBox);
    }
}
