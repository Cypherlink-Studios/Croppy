package dev.darkblade.croppy.api.station.component;

/**
 * Component managing active heat emission, fuel combustion, target temperature regulation,
 * and thermal operational limits for heat-generating stations.
 */
public interface HeatSourceComponent extends StationComponent {

    /**
     * @return true if the heat source is powered on / active
     */
    boolean isActive();

    /**
     * Sets whether the heat source is powered on / active.
     *
     * @param active true if powered on
     */
    void setActive(boolean active);

    /**
     * @return desired target output temperature in Celsius
     */
    double getTargetTemperature();

    /**
     * Sets desired target output temperature in Celsius.
     *
     * @param targetTemperature target temperature
     */
    void setTargetTemperature(double targetTemperature);

    /**
     * @return current actual emitted heat temperature in Celsius
     */
    double getOutputTemperature();

    /**
     * Sets current emitted heat temperature in Celsius.
     *
     * @param outputTemperature output temperature
     */
    void setOutputTemperature(double outputTemperature);

    /**
     * @return remaining fuel burn ticks for the active combustion cycle
     */
    long getBurnTicksRemaining();

    /**
     * Sets remaining fuel burn ticks.
     *
     * @param burnTicks remaining burn ticks
     */
    void setBurnTicksRemaining(long burnTicks);

    /**
     * @return total duration in ticks of the current fuel item combustion cycle
     */
    long getMaxBurnTicks();

    /**
     * Sets total duration in ticks of the current fuel item combustion cycle.
     *
     * @param maxBurnTicks max burn ticks
     */
    void setMaxBurnTicks(long maxBurnTicks);

    /**
     * @return minimum configurable temperature in Celsius for this station
     */
    double getMinTemperature();

    /**
     * @return maximum configurable temperature in Celsius for this station
     */
    double getMaxTemperature();

    /**
     * @return rate of temperature increase in Celsius per tick
     */
    double getHeatingRate();

    /**
     * @return rate of passive cooling in Celsius per tick
     */
    double getCoolingRate();

    /**
     * @return fuel consumption multiplier for this station
     */
    double getFuelBurnMultiplier();

    /**
     * @return true if actively burning fuel
     */
    default boolean isBurning() {
        return isActive() && getBurnTicksRemaining() > 0;
    }
}
