package dev.darkblade.croppy.api.station.component;

/**
 * Component managing temperature, heat source presence, and thermal operational zones for crafting stations.
 */
public interface ThermalComponent extends StationComponent {

    /**
     * @return current temperature in Celsius degrees
     */
    double getTemperature();

    /**
     * Sets current temperature in Celsius degrees.
     *
     * @param temperature temperature
     */
    void setTemperature(double temperature);

    /**
     * @return target temperature provided by heat source or ambient world
     */
    double getTargetTemperature();

    /**
     * Sets target temperature.
     *
     * @param targetTemperature target temperature
     */
    void setTargetTemperature(double targetTemperature);

    /**
     * @return true if an active heat source is heating the station
     */
    boolean isHeated();

    /**
     * Sets whether an active heat source is heating the station.
     *
     * @param heated true if active heat source present
     */
    void setHeated(boolean heated);

    /**
     * @return operational thermal zone based on current temperature
     */
    default ThermalZone getThermalZone() {
        return ThermalZone.fromTemperature(getTemperature());
    }

    /**
     * Advances temperature towards target temperature by step delta.
     *
     * @param step convergence step
     */
    void converge(double step);
}
