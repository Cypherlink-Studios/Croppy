package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Compact binary serializer/deserializer for spatial station instances.
 */
public final class StationBinaryCodec {

    private static final int MAGIC = 0x53544154; // 'STAT'
    public static final int CURRENT_VERSION = 3;

    private StationBinaryCodec() {}

    /**
     * Serializes a collection of station instances into a binary byte array.
     *
     * @param stations stations to serialize
     * @return byte array
     */
    public static byte[] serialize(Collection<StationInstance> stations) {
        if (stations == null || stations.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(MAGIC);
            out.writeInt(CURRENT_VERSION);
            out.writeInt(stations.size());

            for (StationInstance station : stations) {
                // Core identifier & position
                out.writeUTF(station.getStationKey().asString());
                BlockPosition pos = station.getPosition();
                out.writeUTF(pos.worldName());
                out.writeInt(pos.x());
                out.writeInt(pos.y());
                out.writeInt(pos.z());

                // 1. Storage Component
                Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
                out.writeBoolean(storageOpt.isPresent());
                if (storageOpt.isPresent()) {
                    StorageComponent storage = storageOpt.get();
                    List<StoredItem> inputs = storage.getInputs();
                    out.writeInt(inputs.size());
                    for (StoredItem item : inputs) {
                        out.writeUTF(item.itemKey().asString());
                        out.writeInt(item.amount());
                        out.writeInt(item.quality());
                    }

                    List<StoredItem> outputs = storage.getOutputs();
                    out.writeInt(outputs.size());
                    for (StoredItem item : outputs) {
                        out.writeUTF(item.itemKey().asString());
                        out.writeInt(item.amount());
                        out.writeInt(item.quality());
                    }
                }

                // 2. Process Component
                Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
                boolean hasActiveProcess = processOpt.isPresent() && processOpt.get().hasActiveProcess();
                out.writeBoolean(hasActiveProcess);
                if (hasActiveProcess) {
                    ProcessInstance process = processOpt.get().getActiveProcess().orElseThrow();
                    out.writeUTF(process.getProcessKey().asString());
                    out.writeUTF(process.getState().name());
                    out.writeInt(process.getCurrentPhaseIndex());
                    out.writeLong(process.getPhaseElapsedTicks());
                    out.writeLong(process.getTotalElapsedTicks());
                }

                // 3. Display Component
                Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
                out.writeBoolean(displayOpt.isPresent());
                if (displayOpt.isPresent()) {
                    DisplayComponent display = displayOpt.get();
                    Optional<Key> itemKey = display.getDisplayedItem();
                    out.writeBoolean(itemKey.isPresent());
                    if (itemKey.isPresent()) {
                        out.writeUTF(itemKey.get().asString());
                    }
                    out.writeUTF(display.getStatusText());
                }

                // 4. Thermal Component
                Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
                out.writeBoolean(thermalOpt.isPresent());
                if (thermalOpt.isPresent()) {
                    ThermalComponent thermal = thermalOpt.get();
                    out.writeDouble(thermal.getTemperature());
                    out.writeDouble(thermal.getTargetTemperature());
                    out.writeBoolean(thermal.isHeated());
                }

                // 5. Heat Source Component (version 3+)
                Optional<HeatSourceComponent> heatSourceOpt = station.getComponent(HeatSourceComponent.class);
                out.writeBoolean(heatSourceOpt.isPresent());
                if (heatSourceOpt.isPresent()) {
                    HeatSourceComponent heatSource = heatSourceOpt.get();
                    out.writeBoolean(heatSource.isActive());
                    out.writeDouble(heatSource.getTargetTemperature());
                    out.writeDouble(heatSource.getOutputTemperature());
                    out.writeLong(heatSource.getBurnTicksRemaining());
                    out.writeLong(heatSource.getMaxBurnTicks());
                    out.writeDouble(heatSource.getMinTemperature());
                    out.writeDouble(heatSource.getMaxTemperature());
                    out.writeDouble(heatSource.getHeatingRate());
                    out.writeDouble(heatSource.getCoolingRate());
                    out.writeDouble(heatSource.getFuelBurnMultiplier());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize station collection", e);
        }

        return baos.toByteArray();
    }

    /**
     * Deserializes binary data into a list of station instances.
     *
     * @param data             binary data bytes
     * @param definitionLookup function resolving definitions for keys
     * @return list of restored station instances
     */
    public static List<StationInstance> deserialize(byte[] data, Function<Key, Optional<StationDefinition>> definitionLookup) {
        if (data == null || data.length == 0) {
            return Collections.emptyList();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IllegalArgumentException("Invalid binary station header: " + Integer.toHexString(magic));
            }
            int version = in.readInt();
            if (version < 1 || version > CURRENT_VERSION) {
                throw new IllegalArgumentException("Unsupported binary station data version: " + version);
            }

            int count = in.readInt();
            List<StationInstance> list = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                Key stationKey = Key.parse(in.readUTF());
                String world = in.readUTF();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                BlockPosition pos = new BlockPosition(world, x, y, z);

                Optional<StationDefinition> defOpt = definitionLookup.apply(stationKey);
                if (defOpt.isEmpty()) {
                    // Definition no longer exists, skip restoring invalid station
                    skipPayload(in, version);
                    continue;
                }
                StationDefinition def = defOpt.get();
                DefaultStationInstance station = new DefaultStationInstance(stationKey, pos, def);

                // 1. Storage
                boolean hasStorage = in.readBoolean();
                if (hasStorage) {
                    DefaultStorageComponent storage = new DefaultStorageComponent(def.inputCapacity(), def.outputCapacity());
                    int inputCount = in.readInt();
                    for (int j = 0; j < inputCount; j++) {
                        Key itemKey = Key.parse(in.readUTF());
                        int amount = in.readInt();
                        int quality = in.readInt();
                        storage.addInput(StoredItem.of(itemKey, amount, quality));
                    }
                    int outputCount = in.readInt();
                    for (int j = 0; j < outputCount; j++) {
                        Key itemKey = Key.parse(in.readUTF());
                        int amount = in.readInt();
                        int quality = in.readInt();
                        storage.addOutput(StoredItem.of(itemKey, amount, quality));
                    }
                    station.setComponent(StorageComponent.class, storage);
                }

                // 2. Process
                boolean hasProcess = in.readBoolean();
                if (hasProcess) {
                    Key processKey = Key.parse(in.readUTF());
                    ProcessState state = ProcessState.valueOf(in.readUTF());
                    int phaseIndex = in.readInt();
                    long phaseTicks = in.readLong();
                    long totalTicks = in.readLong();
                    DefaultProcessInstance process = new DefaultProcessInstance(processKey, state, phaseIndex, phaseTicks, totalTicks);
                    station.setComponent(ProcessComponent.class, new DefaultProcessComponent(process));
                }

                // 3. Display
                boolean hasDisplay = in.readBoolean();
                if (hasDisplay) {
                    DefaultDisplayComponent display = new DefaultDisplayComponent();
                    boolean hasItem = in.readBoolean();
                    if (hasItem) {
                        display.setDisplayedItem(Key.parse(in.readUTF()));
                    }
                    display.setStatusText(in.readUTF());
                    display.setDirty(false);
                    station.setComponent(DisplayComponent.class, display);
                }

                // 4. Thermal Component (version 2+)
                if (version >= 2) {
                    boolean hasThermal = in.readBoolean();
                    if (hasThermal) {
                        double temp = in.readDouble();
                        double targetTemp = in.readDouble();
                        boolean heated = in.readBoolean();
                        station.setComponent(ThermalComponent.class, new DefaultThermalComponent(temp, targetTemp, heated));
                    }
                }

                // 5. Heat Source Component (version 3+)
                if (version >= 3) {
                    boolean hasHeatSource = in.readBoolean();
                    if (hasHeatSource) {
                        boolean active = in.readBoolean();
                        double targetTemp = in.readDouble();
                        double outputTemp = in.readDouble();
                        long burnTicks = in.readLong();
                        long maxBurnTicks = in.readLong();
                        double minTemp = in.readDouble();
                        double maxTemp = in.readDouble();
                        double heatingRate = in.readDouble();
                        double coolingRate = in.readDouble();
                        double fuelMult = in.readDouble();
                        station.setComponent(HeatSourceComponent.class, new DefaultHeatSourceComponent(
                                active, targetTemp, outputTemp, burnTicks, maxBurnTicks, minTemp, maxTemp, heatingRate, coolingRate, fuelMult
                        ));
                    }
                }

                list.add(station);
            }

            return list;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize station data", e);
        }
    }

    private static void skipPayload(DataInputStream in, int version) throws IOException {
        boolean hasStorage = in.readBoolean();
        if (hasStorage) {
            int inCount = in.readInt();
            for (int i = 0; i < inCount; i++) {
                in.readUTF(); in.readInt(); in.readInt();
            }
            int outCount = in.readInt();
            for (int i = 0; i < outCount; i++) {
                in.readUTF(); in.readInt(); in.readInt();
            }
        }
        boolean hasProcess = in.readBoolean();
        if (hasProcess) {
            in.readUTF(); in.readUTF(); in.readInt(); in.readLong(); in.readLong();
        }
        boolean hasDisplay = in.readBoolean();
        if (hasDisplay) {
            boolean hasItem = in.readBoolean();
            if (hasItem) in.readUTF();
            in.readUTF();
        }
        if (version >= 2) {
            boolean hasThermal = in.readBoolean();
            if (hasThermal) {
                in.readDouble();
                in.readDouble();
                in.readBoolean();
            }
        }
        if (version >= 3) {
            boolean hasHeatSource = in.readBoolean();
            if (hasHeatSource) {
                in.readBoolean();
                in.readDouble();
                in.readDouble();
                in.readLong();
                in.readLong();
                in.readDouble();
                in.readDouble();
                in.readDouble();
                in.readDouble();
                in.readDouble();
            }
        }
    }
}
