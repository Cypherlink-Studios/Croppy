package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.station.component.ThermalZone;

/**
 * Default in-memory implementation of {@link ThermalComponent}.
 */
public final class DefaultThermalComponent implements ThermalComponent {

    public static final double DEFAULT_AMBIENT_TEMPERATURE = 20.0;

    private volatile double temperature;
    private volatile double targetTemperature;
    private volatile boolean heated;

    public DefaultThermalComponent() {
        this(DEFAULT_AMBIENT_TEMPERATURE, DEFAULT_AMBIENT_TEMPERATURE, false);
    }

    public DefaultThermalComponent(double initialTemperature, double targetTemperature, boolean heated) {
        this.temperature = initialTemperature;
        this.targetTemperature = targetTemperature;
        this.heated = heated;
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public double getTargetTemperature() {
        return targetTemperature;
    }

    @Override
    public void setTargetTemperature(double targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    @Override
    public boolean isHeated() {
        return heated;
    }

    @Override
    public void setHeated(boolean heated) {
        this.heated = heated;
    }

    @Override
    public ThermalZone getThermalZone() {
        return ThermalZone.fromTemperature(temperature);
    }

    @Override
    public synchronized void converge(double step) {
        double delta = targetTemperature - temperature;
        if (Math.abs(delta) <= step) {
            temperature = targetTemperature;
        } else if (delta > 0) {
            temperature += Math.abs(step);
        } else {
            temperature -= Math.abs(step);
        }
    }
}
