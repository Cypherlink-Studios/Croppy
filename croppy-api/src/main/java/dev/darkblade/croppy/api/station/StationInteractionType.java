package dev.darkblade.croppy.api.station;

/**
 * Defines the supported interaction modes for crafting stations.
 */
public enum StationInteractionType {
    /**
     * Physical in-world right-click interaction using items or tools in hand.
     */
    PHYSICAL,

    /**
     * Virtual container inventory GUI opened upon interaction.
     */
    GUI,

    /**
     * Hybrid mode: standard right-click opens GUI, sneak-right-click interacts physically in-world.
     */
    HYBRID
}
