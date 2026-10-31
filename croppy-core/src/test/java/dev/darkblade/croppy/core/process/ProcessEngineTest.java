package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.FailurePolicy;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseType;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEngineTest {

    private DefaultProcessRegistry registry;
    private ProcessEngine engine;
    private StationDefinition stationDef;
    private DefaultStationInstance station;
    private DefaultStorageComponent storage;

    @BeforeEach
    void setUp() {
        registry = new DefaultProcessRegistry();
        engine = new ProcessEngine(registry);

        stationDef = new StationDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                "minecraft:cauldron",
                StationInteractionType.HYBRID,
                2,
                1,
                true,
                List.of(),
                Map.of()
        );

        station = new DefaultStationInstance(stationDef.key(), new BlockPosition("world", 0, 64, 0), stationDef);
        storage = new DefaultStorageComponent(2, 1);
        station.setComponent(StorageComponent.class, storage);
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent());
        station.setComponent(DisplayComponent.class, new DefaultDisplayComponent());
    }

    @Test
    void testStartProcessAndTimedAdvancement() {
        Key recipeKey = Key.of("croppy", "tomato_soup");
        Key tomatoKey = Key.of("croppy", "tomato");
        Key soupKey = Key.of("croppy", "tomato_soup");

        ProcessDefinition recipe = new ProcessDefinition(
                recipeKey,
                "Tomato Soup",
                stationDef.key(),
                List.of(StoredItem.of(tomatoKey, 2, 2)), // 2 Gold Tomatoes
                Expression.ALWAYS_TRUE,
                List.of(
                        PhaseDefinition.timed("boil", 50, Expression.ALWAYS_TRUE),
                        PhaseDefinition.timed("simmer", 50, Expression.ALWAYS_TRUE)
                ),
                new QualityOutputResolver(soupKey, 1),
                soupKey,
                1,
                Map.of()
        );
        registry.register(recipe);

        // Put ingredients into storage
        storage.addInput(StoredItem.of(tomatoKey, 2, 2));

        // Start process
        assertTrue(engine.tryStartProcess(station, EvaluationContext.empty()));
        assertTrue(storage.getInputs().isEmpty(), "Inputs should be consumed");

        ProcessComponent pc = station.getComponent(ProcessComponent.class).orElseThrow();
        ProcessInstance pi = pc.getActiveProcess().orElseThrow();
        assertEquals(ProcessState.RUNNING, pi.getState());
        assertEquals(0, pi.getCurrentPhaseIndex());

        // Tick 30 ticks (not finished yet)
        engine.tick(station, 30, EvaluationContext.empty());
        assertEquals(0, pi.getCurrentPhaseIndex());
        assertEquals(30, pi.getPhaseElapsedTicks());

        // Tick 25 ticks (total 55 >= 50, phase 1 advances to phase 2)
        engine.tick(station, 25, EvaluationContext.empty());
        assertEquals(1, pi.getCurrentPhaseIndex());
        assertEquals(ProcessState.RUNNING, pi.getState());

        // Tick 50 ticks (phase 2 finishes, process completes)
        engine.tick(station, 50, EvaluationContext.empty());
        assertEquals(ProcessState.COMPLETED, pi.getState());

        // Verify output produced with quality
        List<StoredItem> outputs = storage.getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(soupKey, outputs.get(0).itemKey());
        assertEquals(2, outputs.get(0).quality(), "Gold tomatoes should yield Gold soup");
    }

    @Test
    void testManualInteractionPhase() {
        Key recipeKey = Key.of("croppy", "stew");
        Key waterKey = Key.of("croppy", "water_cup");
        Key carrotKey = Key.of("croppy", "carrot");
        Key stewKey = Key.of("croppy", "stew");

        ProcessDefinition recipe = new ProcessDefinition(
                recipeKey,
                "Stew",
                stationDef.key(),
                List.of(StoredItem.of(waterKey, 1)),
                Expression.ALWAYS_TRUE,
                List.of(
                        PhaseDefinition.timed("heat", 20, Expression.ALWAYS_TRUE),
                        PhaseDefinition.interactive("add_carrot", 100, carrotKey, 1, "Add carrot!"),
                        PhaseDefinition.timed("cook", 20, Expression.ALWAYS_TRUE)
                ),
                new QualityOutputResolver(stewKey, 1),
                stewKey,
                1,
                Map.of()
        );
        registry.register(recipe);

        storage.addInput(StoredItem.of(waterKey, 1));
        assertTrue(engine.tryStartProcess(station, EvaluationContext.empty()));

        // Advance through phase 0
        engine.tick(station, 20, EvaluationContext.empty());

        ProcessInstance pi = station.getComponent(ProcessComponent.class).get().getActiveProcess().get();
        assertEquals(1, pi.getCurrentPhaseIndex());
        assertEquals(ProcessState.AWAITING_INPUT, pi.getState());

        // Attempt wrong item interaction -> fails
        assertFalse(engine.handleManualInteraction(station, waterKey, 1));
        assertEquals(ProcessState.AWAITING_INPUT, pi.getState());

        // Attempt correct item interaction -> advances
        assertTrue(engine.handleManualInteraction(station, carrotKey, 1));
        assertEquals(2, pi.getCurrentPhaseIndex());
        assertEquals(ProcessState.RUNNING, pi.getState());
    }

    @Test
    void testTimeoutSpoilFailurePolicy() {
        Key recipeKey = Key.of("croppy", "bread");
        Key doughKey = Key.of("croppy", "dough");
        Key spoiledKey = Key.of("croppy", "burnt_bread");

        PhaseDefinition interactivePhase = new PhaseDefinition(
                "flip",
                PhaseType.MANUAL_INTERACTION,
                0,
                50, // 50 ticks timeout
                Expression.ALWAYS_TRUE,
                null,
                0,
                FailurePolicy.SPOIL,
                spoiledKey,
                "Flip bread!",
                Map.of()
        );

        ProcessDefinition recipe = new ProcessDefinition(
                recipeKey,
                "Bread",
                stationDef.key(),
                List.of(StoredItem.of(doughKey, 1)),
                Expression.ALWAYS_TRUE,
                List.of(interactivePhase),
                null,
                Key.of("croppy", "bread"),
                1,
                Map.of()
        );
        registry.register(recipe);

        storage.addInput(StoredItem.of(doughKey, 1));
        engine.tryStartProcess(station, EvaluationContext.empty());

        // Tick beyond timeout (60 > 50)
        engine.tick(station, 60, EvaluationContext.empty());

        ProcessInstance pi = station.getComponent(ProcessComponent.class).get().getActiveProcess().get();
        assertEquals(ProcessState.FAILED, pi.getState());
        assertEquals(1, storage.getOutputs().size());
        assertEquals(spoiledKey, storage.getOutputs().get(0).itemKey());
    }
}
