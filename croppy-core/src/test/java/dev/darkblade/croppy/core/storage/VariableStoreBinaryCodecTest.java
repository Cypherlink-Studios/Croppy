package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.variable.OperationArgs;
import dev.darkblade.croppy.api.variable.SimpleDataType;
import dev.darkblade.croppy.api.variable.TypedVariableStore;
import dev.darkblade.croppy.api.variable.VariableOperation;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.variable.BooleanDataType;
import dev.darkblade.croppy.core.variable.DecimalDataType;
import dev.darkblade.croppy.core.variable.DefaultDataTypeRegistry;
import dev.darkblade.croppy.core.variable.DefaultTypedVariableStore;
import dev.darkblade.croppy.core.variable.IntegerDataType;
import dev.darkblade.croppy.core.variable.MapCompoundDataType;
import dev.darkblade.croppy.core.variable.StandardOperations;
import dev.darkblade.croppy.core.variable.StringDataType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VariableStoreBinaryCodecTest {

    @Test
    void roundTripStandardTypes() throws IOException {
        DefaultDataTypeRegistry registry = new DefaultDataTypeRegistry();
        DefaultTypedVariableStore store = new DefaultTypedVariableStore();

        store.define("count", IntegerDataType.INSTANCE, 42L);
        store.define("ratio", DecimalDataType.INSTANCE, 3.14159);
        store.define("lit", BooleanDataType.INSTANCE, true);
        store.define("tag", StringDataType.INSTANCE, "oak_cask");
        store.define("tank", MapCompoundDataType.INSTANCE, MapCompoundDataType.INSTANCE.parse(Map.of(
                "fluid", "croppy:cider",
                "ml", 750,
                "notes", List.of("aged", "sweet")
        )));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            VariableStoreBinaryCodec.write(out, store);
        }

        DefaultTypedVariableStore restored = new DefaultTypedVariableStore();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            VariableStoreBinaryCodec.read(in, restored, registry);
        }

        assertFalse(restored.isDirty());
        assertEquals(42L, restored.getTypedVariable("count", Long.class).orElseThrow().getValue());
        assertEquals(3.14159, restored.getTypedVariable("ratio", Double.class).orElseThrow().getValue(), 1e-9);
        assertEquals(true, restored.getTypedVariable("lit", Boolean.class).orElseThrow().getValue());
        assertEquals("oak_cask", restored.getTypedVariable("tag", String.class).orElseThrow().getValue());

        Map<?, ?> restoredTank = restored.getTypedVariable("tank", Map.class).orElseThrow().getValue();
        assertEquals("croppy:cider", restoredTank.get("fluid"));
        assertEquals(750L, restoredTank.get("ml"));
        assertEquals(List.of("aged", "sweet"), restoredTank.get("notes"));
    }

    @Test
    void preservesUnresolvedAddonVariablesAcrossSaveReloadCycles() throws IOException {
        Key customTypeKey = Key.of("magic_addon", "rune_charge");
        SimpleDataType<Integer> runeType = new SimpleDataType<>() {
            @Override
            public Key getKey() { return customTypeKey; }
            @Override
            public Class<Integer> getValueClass() { return Integer.class; }
            @Override
            public Integer getDefaultValue() { return 0; }
            @Override
            public Integer parse(Object rawInput) { return Integer.parseInt(rawInput.toString()); }
            @Override
            public void writeBinary(DataOutput out, Integer value) throws IOException { out.writeInt(value); }
            @Override
            public Integer readBinary(DataInput in) throws IOException { return in.readInt(); }
            @Override
            public Collection<VariableOperation<Integer>> getSupportedOperations() {
                return List.of(StandardOperations.set(this));
            }
        };

        // 1. Addon is installed -> serialize
        DefaultDataTypeRegistry withAddon = new DefaultDataTypeRegistry();
        withAddon.register(runeType);

        DefaultTypedVariableStore initial = new DefaultTypedVariableStore();
        initial.define("charges", runeType, 99);
        initial.define("label", StringDataType.INSTANCE, "Altar");

        byte[] bytes1;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(baos)) {
            VariableStoreBinaryCodec.write(out, initial);
            bytes1 = baos.toByteArray();
        }

        // 2. Addon is uninstalled -> deserialize with base registry only
        DefaultDataTypeRegistry withoutAddon = new DefaultDataTypeRegistry();
        DefaultTypedVariableStore unloadedAddonStore = new DefaultTypedVariableStore();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes1))) {
            VariableStoreBinaryCodec.read(in, unloadedAddonStore, withoutAddon);
        }

        assertEquals("Altar", unloadedAddonStore.getTypedVariable("label", String.class).orElseThrow().getValue());
        assertTrue(unloadedAddonStore.hasVariable("charges"));
        assertTrue(unloadedAddonStore.getUnresolvedVariables().containsKey("charges"));
        assertThrows(IllegalStateException.class, () ->
                unloadedAddonStore.applyOperation("charges", "SET", OperationArgs.of("value", 1))
        );

        // 3. Save again while addon is absent
        byte[] bytes2;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(baos)) {
            VariableStoreBinaryCodec.write(out, unloadedAddonStore);
            bytes2 = baos.toByteArray();
        }

        // 4. Addon is reinstalled -> deserialize with addon registry
        DefaultTypedVariableStore restoredWithAddon = new DefaultTypedVariableStore();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes2))) {
            VariableStoreBinaryCodec.read(in, restoredWithAddon, withAddon);
        }

        assertTrue(restoredWithAddon.getUnresolvedVariables().isEmpty());
        assertEquals(99, restoredWithAddon.getTypedVariable("charges", Integer.class).orElseThrow().getValue());
    }

    @Test
    void stationBinaryCodecRoundTripWithVariablesAndMissingDefinitionSkip() {
        Key potKey = Key.of("croppy", "cooking_pot");
        Key missingKey = Key.of("croppy", "removed_station");

        StationDefinition potDef = new StationDefinition(
                potKey, "Cooking Pot", "minecraft:barrier", StationInteractionType.HYBRID,
                6, 2, true, List.of(), Map.of()
        );
        StationDefinition missingDef = new StationDefinition(
                missingKey, "Removed", "minecraft:stone", StationInteractionType.PHYSICAL,
                1, 1, false, List.of(), Map.of()
        );

        DefaultStationInstance missingStation = new DefaultStationInstance(missingKey, BlockPosition.of("world", 0, 64, 0), missingDef);
        missingStation.ensureVariableStore().define("temp_var", IntegerDataType.INSTANCE, 123L);

        DefaultStationInstance potStation = new DefaultStationInstance(potKey, BlockPosition.of("world", 10, 64, 10), potDef);
        TypedVariableStore vars = potStation.ensureVariableStore();
        vars.define("cleanliness", IntegerDataType.INSTANCE, 85L);
        vars.define("covered", BooleanDataType.INSTANCE, true);

        byte[] encoded = StationBinaryCodec.serialize(List.of(missingStation, potStation));

        // Only potDef exists on reload; missingStation payload must be skipped cleanly
        List<StationInstance> restored = StationBinaryCodec.deserialize(encoded, k ->
                k.equals(potKey) ? Optional.of(potDef) : Optional.empty()
        );

        assertEquals(1, restored.size());
        StationInstance restoredPot = restored.get(0);
        assertEquals(potKey, restoredPot.getStationKey());
        TypedVariableStore restoredVars = restoredPot.getVariableStore().orElseThrow();
        assertEquals(85L, restoredVars.getTypedVariable("cleanliness", Long.class).orElseThrow().getValue());
        assertEquals(true, restoredVars.getTypedVariable("covered", Boolean.class).orElseThrow().getValue());
    }
}
