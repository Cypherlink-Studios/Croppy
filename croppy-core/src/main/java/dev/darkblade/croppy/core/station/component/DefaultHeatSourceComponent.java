package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.station.component.HeatSourceComponent;

/**
 * In-memory implementation of {@link HeatSourceComponent} with scaled fuel consumption
 * and operational thermal convergence.
 */
public final class DefaultHeatSourceComponent implements HeatSourceComponent {

    public static final double DEFAULT_AMBIENT_TEMPERATURE = 20.0;
    public static final double DEFAULT_MIN_TEMP = 20.0;
    public static final double DEFAULT_MAX_TEMP = 350.0;
    public static final double DEFAULT_HEATING_RATE = 5.0;
    public static final double DEFAULT_COOLING_RATE = 2.0;
    public static final double DEFAULT_FUEL_MULTIPLIER = 1.0;

    private volatile boolean active;
    private volatile double targetTemperature;
    private volatile double outputTemperature;
    private volatile long burnTicksRemaining;
    private volatile long maxBurnTicks;

    private final double minTemperature;
    private final double maxTemperature;
    private final double heatingRate;
    private final double coolingRate;
    private final double fuelBurnMultiplier;

    public DefaultHeatSourceComponent() {
        this(false, DEFAULT_MIN_TEMP, DEFAULT_AMBIENT_TEMPERATURE, 0L, 0L,
                DEFAULT_MIN_TEMP, DEFAULT_MAX_TEMP, DEFAULT_HEATING_RATE, DEFAULT_COOLING_RATE, DEFAULT_FUEL_MULTIPLIER);
    }

    public DefaultHeatSourceComponent(
            boolean active,
            double targetTemperature,
            double outputTemperature,
            long burnTicksRemaining,
            long maxBurnTicks,
            double minTemperature,
            double maxTemperature,
            double heatingRate,
            double coolingRate,
            double fuelBurnMultiplier
    ) {
        if (minTemperature < 0) {
            throw new IllegalArgumentException("minTemperature cannot be negative: " + minTemperature);
        }
        if (maxTemperature < minTemperature) {
            throw new IllegalArgumentException("maxTemperature (" + maxTemperature + ") cannot be less than minTemperature (" + minTemperature + ")");
        }
        if (heatingRate <= 0) {
            throw new IllegalArgumentException("heatingRate must be positive: " + heatingRate);
        }
        if (coolingRate <= 0) {
            throw new IllegalArgumentException("coolingRate must be positive: " + coolingRate);
        }
        if (fuelBurnMultiplier <= 0) {
            throw new IllegalArgumentException("fuelBurnMultiplier must be positive: " + fuelBurnMultiplier);
        }

        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.heatingRate = heatingRate;
        this.coolingRate = coolingRate;
        this.fuelBurnMultiplier = fuelBurnMultiplier;

        this.active = active;
        this.targetTemperature = clampTemperature(targetTemperature);
        this.outputTemperature = outputTemperature;
        this.burnTicksRemaining = Math.max(0L, burnTicksRemaining);
        this.maxBurnTicks = Math.max(0L, maxBurnTicks);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public double getTargetTemperature() {
        return targetTemperature;
    }

    @Override
    public void setTargetTemperature(double targetTemperature) {
        this.targetTemperature = clampTemperature(targetTemperature);
    }

    @Override
    public double getOutputTemperature() {
        return outputTemperature;
    }

    @Override
    public void setOutputTemperature(double outputTemperature) {
        this.outputTemperature = outputTemperature;
    }

    @Override
    public long getBurnTicksRemaining() {
        return burnTicksRemaining;
    }

    @Override
    public void setBurnTicksRemaining(long burnTicks) {
        this.burnTicksRemaining = Math.max(0L, burnTicks);
    }

    @Override
    public long getMaxBurnTicks() {
        return maxBurnTicks;
    }

    @Override
    public void setMaxBurnTicks(long maxBurnTicks) {
        this.maxBurnTicks = Math.max(0L, maxBurnTicks);
    }

    @Override
    public double getMinTemperature() {
        return minTemperature;
    }

    @Override
    public double getMaxTemperature() {
        return maxTemperature;
    }

    @Override
    public double getHeatingRate() {
        return heatingRate;
    }

    @Override
    public double getCoolingRate() {
        return coolingRate;
    }

    @Override
    public double getFuelBurnMultiplier() {
        return fuelBurnMultiplier;
    }

    /**
     * Starts a new fuel burn cycle with the specified duration.
     *
     * @param durationTicks fuel burn duration in ticks
     */
    public synchronized void startBurn(long durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        this.burnTicksRemaining = durationTicks;
        this.maxBurnTicks = durationTicks;
    }

    /**
     * Advances fuel combustion and converges emitted temperature towards target.
     *
     * @param deltaTicks elapsed simulation ticks
     */
    public synchronized void tick(long deltaTicks) {
        if (deltaTicks <= 0) {
            return;
        }

        if (active && burnTicksRemaining > 0) {
            // Scaled fuel combustion: higher temperature burns fuel faster
            double tempRange = Math.max(1.0, maxTemperature - minTemperature);
            double tempProgress = Math.clamp((outputTemperature - minTemperature) / tempRange, 0.0, 1.0);
            double scale = 1.0 + (tempProgress * 1.5);
            long burnCost = Math.max(1L, (long) Math.ceil(deltaTicks * scale * fuelBurnMultiplier));

            burnTicksRemaining = Math.max(0L, burnTicksRemaining - burnCost);

            // Thermal convergence towards target temperature
            double diff = targetTemperature - outputTemperature;
            double step = heatingRate * deltaTicks;
            if (Math.abs(diff) <= step) {
                outputTemperature = targetTemperature;
            } else if (diff > 0) {
                outputTemperature += step;
            } else {
                outputTemperature -= step;
            }
        } else {
            // Passive cooling towards ambient temperature
            double diff = DEFAULT_AMBIENT_TEMPERATURE - outputTemperature;
            double step = coolingRate * deltaTicks;
            if (Math.abs(diff) <= step) {
                outputTemperature = DEFAULT_AMBIENT_TEMPERATURE;
            } else if (diff > 0) {
                outputTemperature += step;
            } else {
                outputTemperature -= step;
            }
        }
    }

    private double clampTemperature(double temp) {
        return Math.clamp(temp, minTemperature, maxTemperature);
    }
}
