package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperMenuTickerTest {

    @Test
    @DisplayName("enrichContext populates station, process, and progress variables")
    void testEnrichContext() {
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        ItemFactory itemFactory = new ItemFactory(null, itemRegistry);
        PaperMenuRenderer renderer = new PaperMenuRenderer(itemFactory);

        DefaultProcessRegistry processRegistry = new DefaultProcessRegistry();
        Key recipeKey = Key.of("croppy", "test_soup");
        ProcessDefinition recipe = new ProcessDefinition(
                recipeKey,
                "<green>Test Soup</green>",
                Key.of("croppy", "cooking_pot"),
                List.of(),
                null,
                List.of(PhaseDefinition.timed("cooking", 200, null)), // 200 ticks = 10s
                null,
                Key.of("croppy", "soup_item"),
                1,
                Map.of()
        );
        processRegistry.register(recipe);

        PaperMenuTicker ticker = new PaperMenuTicker(renderer, processRegistry);

        StationDefinition stationDef = new StationDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                "minecraft:cauldron",
                StationInteractionType.GUI,
                6,
                2,
                false,
                List.of(),
                Map.of()
        );
        DefaultStationInstance station = new DefaultStationInstance(stationDef.getKey(), new BlockPosition("world", 0, 64, 0), stationDef);

        // Idle state without process
        EvaluationContext idleCtx = ticker.enrichContext(EvaluationContext.empty(), station, null);
        assertEquals("croppy:cooking_pot", idleCtx.getVariable("station.id").orElse(null));
        assertEquals("Cooking Pot", idleCtx.getVariable("station.name").orElse(null));
        assertEquals("IDLE", idleCtx.getVariable("process.state").orElse(null));

        // Active running process at 100 ticks (50% progress)
        DefaultProcessComponent processComp = new DefaultProcessComponent();
        station.setComponent(ProcessComponent.class, processComp);

        ProcessInstance process = new DefaultProcessInstance(recipeKey);
        process.setState(ProcessState.RUNNING);
        process.addPhaseElapsedTicks(100);
        processComp.setActiveProcess(process);

        EvaluationContext runningCtx = ticker.enrichContext(EvaluationContext.empty(), station, null);
        assertEquals("RUNNING", runningCtx.getVariable("process.state").orElse(null));
        assertEquals("<green>Test Soup</green>", runningCtx.getVariable("recipe.name").orElse(null));
        assertEquals(50, runningCtx.getVariable("process.progress_percent").orElse(null));
        assertEquals(5L, runningCtx.getVariable("process.remaining_seconds").orElse(null)); // (200 - 100) / 20 = 5s
    }
}
