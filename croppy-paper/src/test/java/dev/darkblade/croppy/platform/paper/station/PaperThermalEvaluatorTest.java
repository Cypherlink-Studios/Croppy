package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.platform.paper.station.PaperThermalEvaluator.HeatSourceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaperThermalEvaluatorTest {

    private final PaperThermalEvaluator evaluator = new PaperThermalEvaluator();

    @Test
    @DisplayName("Campfire heat evaluation honors lit state")
    void testCampfireLitState() {
        HeatSourceResult lit = evaluator.evaluateMaterial("CAMPFIRE", true);
        assertTrue(lit.isHeated());
        assertEquals(300.0, lit.targetTemperature(), 0.001);

        HeatSourceResult unlit = evaluator.evaluateMaterial("CAMPFIRE", false);
        assertFalse(unlit.isHeated());
        assertEquals(20.0, unlit.targetTemperature(), 0.001);
    }

    @Test
    @DisplayName("Soul campfire and furnace temperatures")
    void testSoulCampfireAndFurnace() {
        HeatSourceResult soulCampfire = evaluator.evaluateMaterial("SOUL_CAMPFIRE", true);
        assertTrue(soulCampfire.isHeated());
        assertEquals(450.0, soulCampfire.targetTemperature(), 0.001);

        HeatSourceResult smoker = evaluator.evaluateMaterial("SMOKER", true);
        assertTrue(smoker.isHeated());
        assertEquals(250.0, smoker.targetTemperature(), 0.001);

        HeatSourceResult furnace = evaluator.evaluateMaterial("FURNACE", true);
        assertTrue(furnace.isHeated());
        assertEquals(350.0, furnace.targetTemperature(), 0.001);
    }

    @Test
    @DisplayName("Fire and lava are always heated")
    void testFireAndLava() {
        HeatSourceResult fire = evaluator.evaluateMaterial("FIRE", false);
        assertTrue(fire.isHeated());
        assertEquals(500.0, fire.targetTemperature(), 0.001);

        HeatSourceResult lava = evaluator.evaluateMaterial("LAVA", false);
        assertTrue(lava.isHeated());
        assertEquals(1000.0, lava.targetTemperature(), 0.001);

        HeatSourceResult magma = evaluator.evaluateMaterial("MAGMA_BLOCK", false);
        assertTrue(magma.isHeated());
        assertEquals(600.0, magma.targetTemperature(), 0.001);
    }

    @Test
    @DisplayName("Non-heat blocks evaluate to ambient 20°C")
    void testNonHeatBlocks() {
        HeatSourceResult dirt = evaluator.evaluateMaterial("DIRT", false);
        assertFalse(dirt.isHeated());
        assertEquals(20.0, dirt.targetTemperature(), 0.001);

        HeatSourceResult stone = evaluator.evaluateMaterial("STONE", true);
        assertFalse(stone.isHeated());
        assertEquals(20.0, stone.targetTemperature(), 0.001);
    }
}
