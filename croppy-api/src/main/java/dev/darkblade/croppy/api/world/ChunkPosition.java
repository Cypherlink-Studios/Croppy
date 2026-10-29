package dev.darkblade.croppy.api.world;

import java.util.Objects;

/**
 * Immutable spatial coordinate representation for world chunks.
 *
 * @param worldName the name of the world
 * @param x chunk x-coordinate
 * @param z chunk z-coordinate
 */
public record ChunkPosition(String worldName, int x, int z) {
    public ChunkPosition {
        Objects.requireNonNull(worldName, "World name cannot be null");
        if (worldName.isBlank()) {
            throw new IllegalArgumentException("World name cannot be blank");
        }
    }

    public static ChunkPosition of(String worldName, int x, int z) {
        return new ChunkPosition(worldName, x, z);
    }

    @Override
    public String toString() {
        return worldName + ":chunk[" + x + "," + z + "]";
    }
}
