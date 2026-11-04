package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.station.component.ThermalZone;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import org.junit.jupiter.api.DisplayName;
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
    void testThermalSerializationRoundtrip() {
        Key potKey = Key.of("croppy", "cooking_pot");
        StationDefinition def = new StationDefinition(
                potKey,
                "Cooking Pot",
                "minecraft:player_head",
                StationInteractionType.HYBRID,
                6,
                2,
                true,
                List.of(),
                Map.of()
        );

        BlockPosition pos = new BlockPosition("world", 10, 64, -20);
        DefaultStationInstance station = new DefaultStationInstance(potKey, pos, def);

        DefaultThermalComponent thermal = new DefaultThermalComponent(95.5, 120.0, true);
        station.setComponent(ThermalComponent.class, thermal);

        byte[] bytes = StationBinaryCodec.serialize(List.of(station));
        assertNotNull(bytes);

        List<StationInstance> deserialized = StationBinaryCodec.deserialize(bytes, k -> Optional.of(def));
        assertEquals(1, deserialized.size());

        ThermalComponent restored = deserialized.get(0).getComponent(ThermalComponent.class).orElseThrow();
        assertEquals(95.5, restored.getTemperature(), 0.001);
        assertEquals(120.0, restored.getTargetTemperature(), 0.001);
        assertTrue(restored.isHeated());
        assertEquals(ThermalZone.SIMMER, restored.getThermalZone());
    }

    @Test
    void testHeatSourceSerializationRoundtrip() {
        Key stoveKey = Key.of("croppy", "stove");
        StationDefinition def = new StationDefinition(
                stoveKey,
                "Stove",
                "minecraft:smoker",
                StationInteractionType.HYBRID,
                1,
                0,
                true,
                List.of(),
                Map.of("heat_source", "true")
        );

        BlockPosition pos = new BlockPosition("world", 15, 64, 30);
        DefaultStationInstance station = new DefaultStationInstance(stoveKey, pos, def);

        DefaultHeatSourceComponent heatSource = new DefaultHeatSourceComponent(
                true, 185.0, 160.0, 1200L, 1600L, 20.0, 350.0, 5.0, 2.0, 1.2
        );
        station.setComponent(HeatSourceComponent.class, heatSource);

        byte[] bytes = StationBinaryCodec.serialize(List.of(station));
        assertNotNull(bytes);

        List<StationInstance> deserialized = StationBinaryCodec.deserialize(bytes, k -> Optional.of(def));
        assertEquals(1, deserialized.size());

        HeatSourceComponent restored = deserialized.get(0).getComponent(HeatSourceComponent.class).orElseThrow();
        assertTrue(restored.isActive());
        assertEquals(185.0, restored.getTargetTemperature(), 0.001);
        assertEquals(160.0, restored.getOutputTemperature(), 0.001);
        assertEquals(1200L, restored.getBurnTicksRemaining());
        assertEquals(1600L, restored.getMaxBurnTicks());
        assertEquals(20.0, restored.getMinTemperature(), 0.001);
        assertEquals(350.0, restored.getMaxTemperature(), 0.001);
        assertEquals(5.0, restored.getHeatingRate(), 0.001);
        assertEquals(2.0, restored.getCoolingRate(), 0.001);
        assertEquals(1.2, restored.getFuelBurnMultiplier(), 0.001);
    }

    @Test
    void testSerializationRoundtripWithYaw() {
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
        DefaultStationInstance station = new DefaultStationInstance(stationKey, pos, def, 270.0f);

        byte[] bytes = StationBinaryCodec.serialize(List.of(station));
        assertNotNull(bytes);

        List<StationInstance> deserialized = StationBinaryCodec.deserialize(bytes, k -> Optional.of(def));
        assertEquals(1, deserialized.size());

        StationInstance restored = deserialized.get(0);
        assertEquals(270.0f, restored.getYaw(), 0.001f);
    }

    @Test
    void testLegacyV3BackwardCompatibility() throws Exception {
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

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(baos)) {
            out.writeInt(0x53544154); // MAGIC
            out.writeInt(3); // V3
            out.writeInt(1); // count

            out.writeUTF("croppy:keg");
            out.writeUTF("world");
            out.writeInt(10);
            out.writeInt(64);
            out.writeInt(20);
            // Notice: No yaw written in V3

            out.writeBoolean(false); // Storage
            out.writeBoolean(false); // Process
            out.writeBoolean(false); // Display
            out.writeBoolean(false); // Thermal (V2+)
            out.writeBoolean(false); // HeatSource (V3+)
        }

        byte[] v3Bytes = baos.toByteArray();
        List<StationInstance> deserialized = StationBinaryCodec.deserialize(v3Bytes, k -> Optional.of(def));
        assertEquals(1, deserialized.size());
        assertEquals(0.0f, deserialized.get(0).getYaw(), 0.001f);
    }

    @Test
    @DisplayName("Idle station without active process restores ProcessComponent on deserialization")
    void testIdleStationRestoresProcessComponent() {
        Key stationKey = Key.of("croppy", "skillet");
        StationDefinition def = new StationDefinition(stationKey, "Skillet", "minecraft:iron_block", StationInteractionType.PHYSICAL, 2, 1, false, List.of(), Map.of());
        BlockPosition pos = new BlockPosition("world", 10, 64, 20);
        DefaultStationInstance station = new DefaultStationInstance(stationKey, pos, def);
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent()); // idle, no active process

        byte[] bytes = StationBinaryCodec.serialize(List.of(station));
        List<StationInstance> deserialized = StationBinaryCodec.deserialize(bytes, k -> Optional.of(def));

        assertEquals(1, deserialized.size());
        StationInstance restored = deserialized.get(0);
        Optional<ProcessComponent> processOpt = restored.getComponent(ProcessComponent.class);
        assertTrue(processOpt.isPresent(), "ProcessComponent must always be restored even when idle");
        assertFalse(processOpt.get().hasActiveProcess(), "Idle station should not have active process");
    }

    @Test
    void testEmptyCollectionHandling() {
        byte[] empty = StationBinaryCodec.serialize(List.of());
        assertEquals(0, empty.length);

        List<StationInstance> list = StationBinaryCodec.deserialize(empty, k -> Optional.empty());
        assertTrue(list.isEmpty());
    }
}
