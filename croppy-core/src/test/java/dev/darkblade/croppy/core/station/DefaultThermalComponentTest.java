package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.station.component.ThermalZone;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultThermalComponentTest {

    @Test
    @DisplayName("Default initialization sets 20°C and unheated")
    void testDefaultInit() {
        DefaultThermalComponent comp = new DefaultThermalComponent();
        assertEquals(20.0, comp.getTemperature(), 0.001);
        assertEquals(20.0, comp.getTargetTemperature(), 0.001);
        assertFalse(comp.isHeated());
        assertEquals(ThermalZone.COLD, comp.getThermalZone());
    }

    @Test
    @DisplayName("Thermal zone resolves correctly across temperature boundaries")
    void testThermalZones() {
        DefaultThermalComponent comp = new DefaultThermalComponent();

        comp.setTemperature(20.0);
        assertEquals(ThermalZone.COLD, comp.getThermalZone());

        comp.setTemperature(60.0);
        assertEquals(ThermalZone.WARM, comp.getThermalZone());

        comp.setTemperature(85.0);
        assertEquals(ThermalZone.SIMMER, comp.getThermalZone());

        comp.setTemperature(110.0);
        assertEquals(ThermalZone.BOIL, comp.getThermalZone());

        comp.setTemperature(160.0);
        assertEquals(ThermalZone.SEAR, comp.getThermalZone());

        comp.setTemperature(250.0);
        assertEquals(ThermalZone.SCORCH, comp.getThermalZone());
    }

    @Test
    @DisplayName("Temperature converges towards target temperature correctly")
    void testConvergence() {
        DefaultThermalComponent comp = new DefaultThermalComponent(20.0, 100.0, true);
        assertTrue(comp.isHeated());

        // Converge up by step 25
        comp.converge(25.0);
        assertEquals(45.0, comp.getTemperature(), 0.001);

        // Converge up by step 50
        comp.converge(50.0);
        assertEquals(95.0, comp.getTemperature(), 0.001);

        // Final step should snap to 100.0 without overshooting
        comp.converge(20.0);
        assertEquals(100.0, comp.getTemperature(), 0.001);

        // Change target to cooling 20.0
        comp.setTargetTemperature(20.0);
        comp.setHeated(false);
        comp.converge(30.0);
        assertEquals(70.0, comp.getTemperature(), 0.001);
    }
}
