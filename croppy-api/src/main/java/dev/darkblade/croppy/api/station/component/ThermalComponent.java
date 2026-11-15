package dev.darkblade.croppy.api.station.component;

import dev.darkblade.croppy.api.expression.VariableProvider;

import java.util.Locale;
import java.util.Map;

/**
 * Component managing temperature, heat source presence, and thermal operational zones for crafting stations.
 */
public interface ThermalComponent extends StationComponent, VariableProvider {

    @Override
    default String getVariableNamespace() {
        return "thermal";
    }

    @Override
    default void populateVariables(Map<String, Object> variables) {
        double temp = getTemperature();
        ThermalZone zone = getThermalZone();
        variables.put("temp", temp);
        variables.put("temperature", temp);
        variables.put("temp_c", String.format(Locale.ROOT, "%.1f°C", temp));
        variables.put("target_temp", getTargetTemperature());
        variables.put("zone", zone != null ? zone.name() : "COLD");
        variables.put("is_heated", isHeated());
    }

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
