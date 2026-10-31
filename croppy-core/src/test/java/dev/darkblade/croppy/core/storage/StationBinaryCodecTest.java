package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StationBinaryCodecTest {

    @Test
    void testSerializationRoundtrip() {
        Key stationKey = Key.of("croppy", "keg");
        StationDefinition def = new StationDefinition(
                stationKey,
                "Keg",
                "minecraft:barrel",
                StationInteractionType.HYBRID,
                2,
                1,
                true,
                List.of(),
                Map.of()
        );

        BlockPosition pos = new BlockPosition("world", 100, 65, 200);
        DefaultStationInstance station = new DefaultStationInstance(stationKey, pos, def);

        // Add storage
        DefaultStorageComponent storage = new DefaultStorageComponent(2, 1);
        storage.addInput(StoredItem.of(Key.of("croppy", "grape"), 5, 2));
        storage.addOutput(StoredItem.of(Key.of("croppy", "wine"), 1, 3));
        station.setComponent(StorageComponent.class, storage);

        // Add process
        DefaultProcessInstance process = new DefaultProcessInstance(Key.of("croppy", "grape_to_wine"), ProcessState.RUNNING, 1, 50, 150);
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent(process));

        // Add display
        DefaultDisplayComponent display = new DefaultDisplayComponent();
        display.setDisplayedItem(Key.of("croppy", "grape"));
        display.setStatusText("<gray>Fermenting...</gray>");
        station.setComponent(DisplayComponent.class, display);

        // Serialize
        byte[] bytes = StationBinaryCodec.serialize(List.of(station));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Deserialize
        List<StationInstance> deserialized = StationBinaryCodec.deserialize(bytes, k -> Optional.of(def));
        assertEquals(1, deserialized.size());

        StationInstance restored = deserialized.get(0);
        assertEquals(stationKey, restored.getStationKey());
        assertEquals(pos, restored.getPosition());

        // Verify storage
        StorageComponent restoredStorage = restored.getComponent(StorageComponent.class).orElseThrow();
        assertEquals(1, restoredStorage.getInputs().size());
        assertEquals(Key.of("croppy", "grape"), restoredStorage.getInputs().get(0).itemKey());
        assertEquals(5, restoredStorage.getInputs().get(0).amount());
        assertEquals(2, restoredStorage.getInputs().get(0).quality());
        assertEquals(1, restoredStorage.getOutputs().size());
        assertEquals(Key.of("croppy", "wine"), restoredStorage.getOutputs().get(0).itemKey());

        // Verify process
        ProcessComponent restoredProcess = restored.getComponent(ProcessComponent.class).orElseThrow();
        assertTrue(restoredProcess.hasActiveProcess());
        assertEquals(Key.of("croppy", "grape_to_wine"), restoredProcess.getActiveProcess().get().getProcessKey());
        assertEquals(ProcessState.RUNNING, restoredProcess.getActiveProcess().get().getState());
        assertEquals(1, restoredProcess.getActiveProcess().get().getCurrentPhaseIndex());
        assertEquals(50, restoredProcess.getActiveProcess().get().getPhaseElapsedTicks());

        // Verify display
        DisplayComponent restoredDisplay = restored.getComponent(DisplayComponent.class).orElseThrow();
        assertEquals(Optional.of(Key.of("croppy", "grape")), restoredDisplay.getDisplayedItem());
        assertEquals("<gray>Fermenting...</gray>", restoredDisplay.getStatusText());
    }

    @Test
    void testEmptyCollectionHandling() {
        byte[] empty = StationBinaryCodec.serialize(List.of());
        assertEquals(0, empty.length);

        List<StationInstance> list = StationBinaryCodec.deserialize(empty, k -> Optional.empty());
        assertTrue(list.isEmpty());
    }
}
