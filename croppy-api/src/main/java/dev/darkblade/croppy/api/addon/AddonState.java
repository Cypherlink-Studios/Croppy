package dev.darkblade.croppy.api.addon;

/**
 * Represents the lifecycle state of a Croppy addon.
 */
public enum AddonState {
    /**
     * The addon is discovered on disk but its manifest or classes have not been loaded into memory.
     */
    ON_DISK,

    /**
     * The addon manifest has been parsed, its dependencies resolved, and classes loaded, but it is not active.
     */
    LOADED,

    /**
     * The addon is enabled and actively participating in simulation, registries, and events.
     */
    ENABLED
}
