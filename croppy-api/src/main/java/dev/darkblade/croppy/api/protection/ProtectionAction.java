package dev.darkblade.croppy.api.protection;

/**
 * Enumerates interaction and gameplay actions subject to spatial permission and claim protection validation.
 */
public enum ProtectionAction {
    /**
     * Planting seeds or placing crop structures into the soil.
     */
    PLANT,

    /**
     * Harvesting mature crops (including right-click harvesting and virtual display interactions).
     */
    HARVEST,

    /**
     * Breaking or dismantling crops and station blocks.
     */
    BREAK,

    /**
     * Pouring water onto farmland or hydration grids with watering cans.
     */
    WATER,

    /**
     * Applying fertilizer nutrients (N-P-K) to farmland.
     */
    FERTILIZE,

    /**
     * Interacting with cooking stations (inserting ingredients, taking dishes, opening menus).
     */
    STATION_USE,

    /**
     * Adding fuel or igniting heating sources and stoves.
     */
    STATION_FUEL
}
