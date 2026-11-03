package dev.darkblade.croppy.api.species;

/**
 * Specifies how a placed 3D block or planted crop orients itself in the world.
 */
public enum RotationMode {
    /**
     * Snaps to the 4 cardinal directions (North, South, East, West).
     */
    CARDINAL_4,

    /**
     * Snaps to the 8 compass directions (Cardinal + Ordinal).
     */
    CARDINAL_8,

    /**
     * Preserves the continuous yaw angle facing of the player.
     */
    FREE_YAW,

    /**
     * Applies a subtle randomized angle offset to the player's yaw for natural variation.
     */
    RANDOM_JITTER
}
