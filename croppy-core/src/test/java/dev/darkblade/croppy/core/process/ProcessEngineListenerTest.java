package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.FailurePolicy;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseType;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessListener;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEngineListenerTest {

    @Test
    void dispatchesStartTickPhaseAdvanceAndCompleteSafely() {
        DefaultProcessRegistry registry = new DefaultProcessRegistry();
        Key stationKey = Key.of("test", "kettle");
        Key procKey = Key.of("test", "brew");
        Key inputKey = Key.of("minecraft", "apple");
        Key outputKey = Key.of("test", "cider");

        PhaseDefinition p1 = new PhaseDefinition("p1", PhaseType.TIMED, 10, 0, Expression.ALWAYS_TRUE,
                null, 0, FailurePolicy.CANCEL, null, null, Map.of());
        PhaseDefinition p2 = new PhaseDefinition("p2", PhaseType.TIMED, 10, 0, Expression.ALWAYS_TRUE,
                null, 0, FailurePolicy.CANCEL, null, null, Map.of());
        ProcessDefinition def = new ProcessDefinition(procKey, "Brew", stationKey,
                List.of(StoredItem.of(inputKey, 1)), Expression.ALWAYS_TRUE,
                List.of(p1, p2), null, outputKey, 1, Map.of());
        registry.register(def);

        ProcessEngine engine = new ProcessEngine(registry);
        List<String> events = new ArrayList<>();

        // Faulty listener must not break execution or other listeners
        engine.addListener(new ProcessListener() {
            @Override
            public void onProcessStart(dev.darkblade.croppy.api.station.StationInstance s, ProcessDefinition d, ProcessInstance i) {
                throw new RuntimeException("boom");
            }
        });
        engine.addListener(new ProcessListener() {
            @Override
            public void onProcessStart(dev.darkblade.croppy.api.station.StationInstance s, ProcessDefinition d, ProcessInstance i) {
                events.add("start:" + d.key());
            }

            @Override
            public void onProcessTick(dev.darkblade.croppy.api.station.StationInstance s, ProcessDefinition d, ProcessInstance i, long deltaTicks) {
                events.add("tick:" + deltaTicks);
            }

            @Override
            public void onPhaseAdvance(dev.darkblade.croppy.api.station.StationInstance s, ProcessDefinition d, ProcessInstance i, int prev, int next) {
                events.add("advance:" + prev + "->" + next);
            }

            @Override
            public void onProcessComplete(dev.darkblade.croppy.api.station.StationInstance s, ProcessDefinition d, ProcessInstance i, List<StoredItem> outputs) {
                events.add("complete:" + outputs.get(0).itemKey());
            }
        });

        StationDefinition stationDef = new StationDefinition(stationKey, "Kettle", "minecraft:cauldron",
                StationInteractionType.GUI, 2, 2, null, false, List.of(), Map.of());
        DefaultStationInstance station = new DefaultStationInstance(stationKey, new BlockPosition("world", 0, 64, 0), stationDef);
        DefaultStorageComponent storage = new DefaultStorageComponent(2, 2);
        storage.addInput(StoredItem.of(inputKey, 1));
        station.setComponent(StorageComponent.class, storage);

        assertTrue(engine.tryStartProcess(station, EvaluationContext.empty()));
        engine.tick(station, 10, EvaluationContext.empty());
        engine.tick(station, 10, EvaluationContext.empty());

        assertEquals(List.of(
                "start:test:brew",
                "tick:10",
                "advance:0->1",
                "tick:10",
                "complete:test:cider"
        ), events);
    }
}
