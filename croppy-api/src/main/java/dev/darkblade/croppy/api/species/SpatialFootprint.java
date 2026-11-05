package dev.darkblade.croppy.api.species;

import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable spatial footprint describing the multi-block volume occupied by a crop.
 *
 * @param height  vertical height in blocks (must be >= 1)
 * @param offsetX relative horizontal X offset
 * @param offsetY relative vertical Y offset
 * @param offsetZ relative horizontal Z offset
 */
public record SpatialFootprint(int height, int offsetX, int offsetY, int offsetZ) {

    public static final SpatialFootprint SINGLE_BLOCK = new SpatialFootprint(1, 0, 0, 0);

    public SpatialFootprint {
        if (height < 1) {
            throw new IllegalArgumentException("SpatialFootprint height must be at least 1, got: " + height);
        }
    }

    public static SpatialFootprint ofHeight(int height) {
        return new SpatialFootprint(height, 0, 0, 0);
    }

    /**
     * Calculates all world block positions occupied by this footprint given a root position.
     *
     * @param origin the root planting position
     * @return unmodifiable list of all occupied positions
     */
    public List<BlockPosition> calculateOccupiedPositions(BlockPosition origin) {
        Objects.requireNonNull(origin, "origin cannot be null");
        if (height == 1 && offsetX == 0 && offsetY == 0 && offsetZ == 0) {
            return List.of(origin);
        }
        List<BlockPosition> positions = new ArrayList<>(height);
        for (int dy = 0; dy < height; dy++) {
            positions.add(new BlockPosition(origin.worldName(), origin.x() + offsetX, origin.y() + offsetY + dy, origin.z() + offsetZ));
        }
        return List.copyOf(positions);
    }
}
