package dev.darkblade.croppy.api.world;

import java.util.Objects;

/**
 * Immutable decoupled spatial coordinate representation for block locations.
 *
 * @param worldName the name of the world
 * @param x x-coordinate
 * @param y y-coordinate
 * @param z z-coordinate
 */
public record BlockPosition(String worldName, int x, int y, int z) {
    public BlockPosition {
        Objects.requireNonNull(worldName, "World name cannot be null");
        if (worldName.isBlank()) {
            throw new IllegalArgumentException("World name cannot be blank");
        }
    }

    public static BlockPosition of(String worldName, int x, int y, int z) {
        return new BlockPosition(worldName, x, y, z);
    }

    public BlockPosition add(int dx, int dy, int dz) {
        return new BlockPosition(worldName, x + dx, y + dy, z + dz);
    }

    public BlockPosition down() {
        return add(0, -1, 0);
    }

    public BlockPosition up() {
        return add(0, 1, 0);
    }

    @Override
    public String toString() {
        return worldName + "[" + x + "," + y + "," + z + "]";
    }
}
