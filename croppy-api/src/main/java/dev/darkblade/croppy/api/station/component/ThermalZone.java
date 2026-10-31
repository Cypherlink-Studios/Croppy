package dev.darkblade.croppy.api.station.component;

/**
 * Operational thermal zones for culinary cookware and temperature-sensitive stations.
 */
public enum ThermalZone {
    COLD,
    WARM,
    SIMMER,
    BOIL,
    SEAR,
    SCORCH;

    /**
     * Resolves the operational thermal zone from a temperature in Celsius degrees.
     *
     * @param temperature temperature in Celsius
     * @return matching thermal zone
     */
    public static ThermalZone fromTemperature(double temperature) {
        if (temperature < 40.0) {
            return COLD;
        } else if (temperature < 75.0) {
            return WARM;
        } else if (temperature < 100.0) {
            return SIMMER;
        } else if (temperature < 140.0) {
            return BOIL;
        } else if (temperature < 220.0) {
            return SEAR;
        } else {
            return SCORCH;
        }
    }
}
