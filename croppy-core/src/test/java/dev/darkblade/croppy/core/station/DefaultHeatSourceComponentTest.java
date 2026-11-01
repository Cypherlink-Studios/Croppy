package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultHeatSourceComponentTest {

    @Test
    @DisplayName("Default initialization sets safe baseline values")
    void defaultInitialization() {
        DefaultHeatSourceComponent comp = new DefaultHeatSourceComponent();

        assertFalse(comp.isActive());
        assertFalse(comp.isBurning());
        assertEquals(20.0, comp.getOutputTemperature());
        assertEquals(20.0, comp.getTargetTemperature());
        assertEquals(0L, comp.getBurnTicksRemaining());
        assertEquals(20.0, comp.getMinTemperature());
        assertEquals(350.0, comp.getMaxTemperature());
    }

    @Test
    @DisplayName("Target temperature is clamped within min and max boundaries")
    void targetTemperatureClamping() {
        DefaultHeatSourceComponent comp = new DefaultHeatSourceComponent(
                false, 100.0, 20.0, 0L, 0L, 20.0, 300.0, 5.0, 2.0, 1.0);

        comp.setTargetTemperature(500.0);
        assertEquals(300.0, comp.getTargetTemperature(), "Should clamp to max temp");

        comp.setTargetTemperature(5.0);
        assertEquals(20.0, comp.getTargetTemperature(), "Should clamp to min temp");

        comp.setTargetTemperature(180.0);
        assertEquals(180.0, comp.getTargetTemperature());
    }

    @Test
    @DisplayName("Active combustion converges output towards target temperature")
    void heatingConvergence() {
        DefaultHeatSourceComponent comp = new DefaultHeatSourceComponent(
                true, 200.0, 20.0, 1000L, 1000L, 20.0, 350.0, 10.0, 2.0, 1.0);

        assertTrue(comp.isBurning());

        comp.tick(5); // 5 ticks * 10°C/tick = 50°C
        assertEquals(70.0, comp.getOutputTemperature(), 0.01);
        assertTrue(comp.getBurnTicksRemaining() < 1000L);

        comp.tick(50); // overshoot
        assertEquals(200.0, comp.getOutputTemperature(), "Should snap to target when within step");
    }

    @Test
    @DisplayName("High output temperature burns fuel faster than low temperature")
    void scaledFuelConsumption() {
        // Low temp (20°C): minimal scale ~1.0
        DefaultHeatSourceComponent lowTempComp = new DefaultHeatSourceComponent(
                true, 20.0, 20.0, 1000L, 1000L, 20.0, 350.0, 5.0, 2.0, 1.0);
        lowTempComp.tick(100);
        long lowTempBurned = 1000L - lowTempComp.getBurnTicksRemaining();

        // High temp (350°C): maximum scale ~2.5
        DefaultHeatSourceComponent highTempComp = new DefaultHeatSourceComponent(
                true, 350.0, 350.0, 1000L, 1000L, 20.0, 350.0, 5.0, 2.0, 1.0);
        highTempComp.tick(100);
        long highTempBurned = 1000L - highTempComp.getBurnTicksRemaining();

        assertTrue(highTempBurned > lowTempBurned,
                "High temperature should burn fuel faster (high: " + highTempBurned + ", low: " + lowTempBurned + ")");
    }

    @Test
    @DisplayName("Inactive station or fuel depletion cools down towards ambient temperature")
    void passiveCooling() {
        DefaultHeatSourceComponent comp = new DefaultHeatSourceComponent(
                false, 200.0, 200.0, 0L, 0L, 20.0, 350.0, 5.0, 10.0, 1.0);

        comp.tick(5); // 5 ticks * 10°C = 50°C drop
        assertEquals(150.0, comp.getOutputTemperature(), 0.01);

        comp.tick(50); // should snap to 20°C ambient
        assertEquals(20.0, comp.getOutputTemperature(), 0.01);
    }
}
