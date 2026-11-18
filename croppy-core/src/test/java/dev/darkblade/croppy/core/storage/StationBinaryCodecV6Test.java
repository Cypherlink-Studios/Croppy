package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StationBinaryCodecV6Test {

    @Test
    void roundTripsStoredItemPropertiesAndProcessContextVariables() {
        Key stationKey = Key.of("brewery", "barrel");
        StationDefinition def = new StationDefinition(stationKey, "Barrel", "minecraft:barrel",
                StationInteractionType.GUI, 4, 2, null, false, List.of(), Map.of());
        DefaultStationInstance station = new DefaultStationInstance(stationKey, new BlockPosition("world", 10, 64, -5), def);

        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addInput(new StoredItem(Key.of("brewery", "apple_must"), 1, 2,
                Map.of("batch_id", "batch-123", "sweetness", 0.75, "sealed", false)));
        storage.addOutput(new StoredItem(Key.of("brewery", "apple_cider"), 2, 3,
                Map.of("alcohol", 5.5, "volume_ml", 250)));
        station.setComponent(StorageComponent.class, storage);

        DefaultProcessInstance proc = new DefaultProcessInstance(Key.of("brewery", "ferment"),
                ProcessState.RUNNING, 1, 40L, 140L);
        proc.setContextVariable("batch_id", "batch-123");
        proc.setContextVariable("last_eval_ms", 1700000000000L);
        proc.setContextVariable("temp_avg", 21.5);
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent(proc));

        byte[] bytes = StationBinaryCodec.serialize(List.of(station));
        List<StationInstance> restored = StationBinaryCodec.deserialize(bytes, k -> k.equals(stationKey) ? Optional.of(def) : Optional.empty());

        assertEquals(1, restored.size());
        StationInstance r = restored.get(0);
        StorageComponent rStorage = r.getComponent(StorageComponent.class).orElseThrow();
        assertEquals(1, rStorage.getInputs().size());
        StoredItem rIn = rStorage.getInputs().get(0);
        assertEquals(2, rIn.quality());
        assertEquals("batch-123", rIn.properties().get("batch_id"));
        assertEquals(0.75, rIn.properties().get("sweetness"));
        assertEquals(false, rIn.properties().get("sealed"));

        StoredItem rOut = rStorage.getOutputs().get(0);
        assertEquals(3, rOut.quality());
        assertEquals(5.5, rOut.properties().get("alcohol"));
        assertEquals(250, rOut.properties().get("volume_ml"));

        ProcessInstance rProc = r.getComponent(ProcessComponent.class).orElseThrow().getActiveProcess().orElseThrow();
        assertEquals("batch-123", rProc.getContextVariables().get("batch_id"));
        assertEquals(1700000000000L, rProc.getContextVariables().get("last_eval_ms"));
        assertEquals(21.5, rProc.getContextVariables().get("temp_avg"));
    }
}
