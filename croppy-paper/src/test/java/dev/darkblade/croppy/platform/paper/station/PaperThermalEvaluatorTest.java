package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.platform.paper.station.PaperThermalEvaluator.HeatSourceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @DisplayName("Custom heat source station is prioritized over vanilla evaluation")
    void testCustomHeatSourcePriority() {
        BlockPosition stovePos = new BlockPosition("world", 10, 64, 10);
        StationDefinition stoveDef = new StationDefinition(
                Key.of("croppy", "stove"),
                "Stove",
                "minecraft:smoker",
                StationInteractionType.HYBRID,
                1, 0, true, List.of(), Map.of("heat_source", "true")
        );
        DefaultStationInstance stove = new DefaultStationInstance(stoveDef.key(), stovePos, stoveDef);
        DefaultHeatSourceComponent heatComp = new DefaultHeatSourceComponent(
                true, 185.0, 185.0, 1000L, 1000L, 20.0, 350.0, 5.0, 2.0, 1.0
        );
        stove.setComponent(HeatSourceComponent.class, heatComp);

        PaperThermalEvaluator evaluatorWithStations = new PaperThermalEvaluator(pos -> {
            if (pos.equals(stovePos)) {
                return Optional.of(stove);
            }
            return Optional.empty();
        });

        // 1. When stove is active at 185°C
        HeatSourceResult result = evaluatorWithStations.evaluateHeatSource(stovePos, null);
        assertTrue(result.isHeated());
        assertEquals(185.0, result.targetTemperature(), 0.001);

        // 2. When stove is turned off
        heatComp.setActive(false);
        HeatSourceResult offResult = evaluatorWithStations.evaluateHeatSource(stovePos, null);
        assertFalse(offResult.isHeated());
        assertEquals(20.0, offResult.targetTemperature(), 0.001);

        // 3. When position below has no custom station, fallback to vanilla block evaluation
        BlockPosition emptyPos = new BlockPosition("world", 20, 64, 20);
        HeatSourceResult fallbackResult = evaluatorWithStations.evaluateHeatSource(emptyPos, null);
        assertFalse(fallbackResult.isHeated());
        assertEquals(20.0, fallbackResult.targetTemperature(), 0.001);
    }

    @Test
    @DisplayName("updateThermalState marks DisplayComponent dirty when temperature changes")
    void testThermalConvergenceMarksDisplayDirty() {
        BlockPosition potPos = new BlockPosition("world", 10, 65, 10);
        StationDefinition potDef = new StationDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                "minecraft:player_head",
                StationInteractionType.HYBRID,
                6, 2, true, List.of(), Map.of("thermal", "true")
        );
        DefaultStationInstance pot = new DefaultStationInstance(potDef.key(), potPos, potDef);
        dev.darkblade.croppy.core.station.component.DefaultThermalComponent thermal =
                new dev.darkblade.croppy.core.station.component.DefaultThermalComponent(20.0, 20.0, false);
        dev.darkblade.croppy.core.station.component.DefaultDisplayComponent display =
                new dev.darkblade.croppy.core.station.component.DefaultDisplayComponent();
        display.setDirty(false);

        pot.setComponent(dev.darkblade.croppy.api.station.component.ThermalComponent.class, thermal);
        pot.setComponent(dev.darkblade.croppy.api.station.component.DisplayComponent.class, display);

        BlockPosition stovePos = new BlockPosition("world", 10, 64, 10);
        StationDefinition stoveDef = new StationDefinition(
                Key.of("croppy", "stove"),
                "Stove",
                "minecraft:smoker",
                StationInteractionType.HYBRID,
                1, 0, true, List.of(), Map.of("heat_source", "true")
        );
        DefaultStationInstance stove = new DefaultStationInstance(stoveDef.key(), stovePos, stoveDef);
        DefaultHeatSourceComponent heatComp = new DefaultHeatSourceComponent(
                true, 100.0, 100.0, 1000L, 1000L, 20.0, 350.0, 5.0, 2.0, 1.0
        );
        stove.setComponent(HeatSourceComponent.class, heatComp);

        PaperThermalEvaluator evaluatorWithStations = new PaperThermalEvaluator(pos -> {
            if (pos.equals(stovePos)) {
                return Optional.of(stove);
            }
            return Optional.empty();
        });

        assertFalse(display.isDirty());
        evaluatorWithStations.updateThermalState(pot, null, 5.0);

        assertTrue(thermal.getTemperature() > 20.0);
        assertTrue(display.isDirty(), "DisplayComponent must be marked dirty when thermal state converges");
    }
}
