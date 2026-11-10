package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StationStateDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.station.visual.StationModelOverride;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationStateEvaluatorTest {

    private StationDefinition definition;
    private DefaultStationInstance station;

    @BeforeEach
    void setUp() {
        StationStateDefinition boilingState = new StationStateDefinition(
                "boiling",
                30,
                "temperature >= 100.0 && process_state == 'RUNNING'",
                new StationModelOverride(Key.of("croppy", "station/pot/boiling"))
        );

        StationStateDefinition litState = new StationStateDefinition(
                "lit",
                20,
                "fuel_ticks > 0",
                new StationModelOverride(Key.of("croppy", "station/pot/lit"))
        );

        StationStateDefinition hasItemsState = new StationStateDefinition(
                "has_items",
                10,
                "has_inputs == true",
                new StationModelOverride(Key.of("croppy", "station/pot/water"))
        );

        StationStateDefinition idleState = new StationStateDefinition(
                "idle",
                0,
                null,
                new StationModelOverride(Key.of("croppy", "station/pot/empty"))
        );

        definition = new StationDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                "minecraft:barrier",
                StationInteractionType.HYBRID,
                6,
                2,
                StationDefinition.DEFAULT_MENU_KEY,
                true,
                List.of(),
                java.util.Map.of(),
                null,
                dev.darkblade.croppy.api.species.RotationMode.CARDINAL_4,
                List.of(idleState, hasItemsState, litState, boilingState)
        );

        station = new DefaultStationInstance(definition.key(), new BlockPosition("world", 0, 64, 0), definition);
    }

    @Test
    void testFallbackToIdleWhenNoConditionsMet() {
        Optional<StationStateDefinition> result = StationStateEvaluator.evaluate(station);
        assertTrue(result.isPresent());
        assertEquals("idle", result.get().id());
    }

    @Test
    void testLitStateWhenFuelPresent() {
        DefaultHeatSourceComponent heatSource = new DefaultHeatSourceComponent();
        heatSource.startBurn(100);
        station.setComponent(HeatSourceComponent.class, heatSource);

        Optional<StationStateDefinition> result = StationStateEvaluator.evaluate(station);
        assertTrue(result.isPresent());
        assertEquals("lit", result.get().id());
    }

    @Test
    void testBoilingStateTakesPriorityOverLit() {
        DefaultHeatSourceComponent heatSource = new DefaultHeatSourceComponent();
        heatSource.startBurn(100);
        station.setComponent(HeatSourceComponent.class, heatSource);

        DefaultThermalComponent thermal = new DefaultThermalComponent();
        thermal.setTemperature(120.0);
        station.setComponent(ThermalComponent.class, thermal);

        DefaultProcessInstance process = new DefaultProcessInstance(
                Key.of("croppy", "soup"),
                ProcessState.RUNNING,
                0,
                20L,
                20L
        );

        Optional<StationStateDefinition> result = StationStateEvaluator.evaluate(station, process);
        assertTrue(result.isPresent());
        assertEquals("boiling", result.get().id());
    }

    @Test
    void testStorageReactivity() {
        DefaultStorageComponent storage = new DefaultStorageComponent(6, 2);
        storage.addInput(StoredItem.of(Key.of("croppy", "carrot"), 2));
        station.setComponent(StorageComponent.class, storage);

        Optional<StationStateDefinition> result = StationStateEvaluator.evaluate(station);
        assertTrue(result.isPresent());
        assertEquals("has_items", result.get().id());
    }
}
