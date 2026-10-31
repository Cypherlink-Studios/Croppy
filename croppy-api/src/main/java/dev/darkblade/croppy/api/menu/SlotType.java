package dev.darkblade.croppy.api.menu;

/**
 * Functional roles assigned to inventory menu slots.
 */
public enum SlotType {

    /**
     * Accepts ingredient deposits and withdrawals, binding to station input storage.
     */
    INPUT,

    /**
     * Holds recipe outputs, permitting pickup/extraction only.
     */
    OUTPUT,

    /**
     * Dynamic visual indicator representing station, process, or system status.
     */
    STATUS,

    /**
     * Interactive clickable button executing configured actions.
     */
    ACTION,

    /**
     * Passive decorative element whose clicks and interactions are strictly cancelled.
     */
    DECORATION
}
