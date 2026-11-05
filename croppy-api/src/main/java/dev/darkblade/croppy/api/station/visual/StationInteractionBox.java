package dev.darkblade.croppy.api.station.visual;

/**
 * Immutable bounding box configuring the dimensions for a station's virtual interaction entity.
 *
 * @param width  hitbox width in blocks (must be positive)
 * @param height hitbox height in blocks (must be positive)
 */
public record StationInteractionBox(float width, float height) {

    public StationInteractionBox {
        if (width <= 0.0f) {
            throw new IllegalArgumentException("width must be positive, got: " + width);
        }
        if (height <= 0.0f) {
            throw new IllegalArgumentException("height must be positive, got: " + height);
        }
    }
}
