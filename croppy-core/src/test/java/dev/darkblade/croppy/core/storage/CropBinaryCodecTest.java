package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CropBinaryCodecTest {

    @Test
    @DisplayName("Roundtrip serialization and deserialization of multiple crops with V2 fields")
    void testRoundtripMultipleCropsV2() {
        CropInstance c1 = new DefaultCropInstance(
                Key.croppy("wheat"),
                BlockPosition.of("world", 100, 64, -200),
                2,
                15.75,
                1000000L,
                2000000L,
                42L,
                Key.of("croppy", "speed_gro")
        );
        CropInstance c2 = new DefaultCropInstance(
                Key.of("custom", "carrot"),
                BlockPosition.of("world_nether", -50, 70, 300),
                0,
                0.0,
                3000000L,
                3000000L,
                -1L,
                null
        );

        byte[] serialized = CropBinaryCodec.serialize(List.of(c1, c2));
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        List<CropInstance> deserialized = CropBinaryCodec.deserialize(serialized);
        assertEquals(2, deserialized.size());

        CropInstance d1 = deserialized.get(0);
        assertEquals(c1.getSpeciesKey(), d1.getSpeciesKey());
        assertEquals(c1.getPosition(), d1.getPosition());
        assertEquals(c1.getStageIndex(), d1.getStageIndex());
        assertEquals(c1.getProgress(), d1.getProgress(), 0.0001);
        assertEquals(c1.getPlantedAtMillis(), d1.getPlantedAtMillis());
        assertEquals(c1.getLastUpdatedMillis(), d1.getLastUpdatedMillis());
        assertEquals(42L, d1.getLastWateredDay());
        assertEquals(Key.of("croppy", "speed_gro"), d1.getFertilizerKey());
        assertTrue(d1.hasFertilizer());

        CropInstance d2 = deserialized.get(1);
        assertEquals(c2.getSpeciesKey(), d2.getSpeciesKey());
        assertEquals(c2.getPosition(), d2.getPosition());
        assertEquals(c2.getStageIndex(), d2.getStageIndex());
        assertEquals(c2.getProgress(), d2.getProgress(), 0.0001);
        assertEquals(c2.getPlantedAtMillis(), d2.getPlantedAtMillis());
        assertEquals(c2.getLastUpdatedMillis(), d2.getLastUpdatedMillis());
        assertEquals(-1L, d2.getLastWateredDay());
        assertNull(d2.getFertilizerKey());
        assertFalse(d2.hasFertilizer());
    }

    @Test
    @DisplayName("Backward compatibility: should deserialize legacy Version 1 payload")
    void testLegacyVersion1Compatibility() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(0x43525059); // MAGIC
            out.writeInt(1);          // VERSION 1
            out.writeInt(1);          // Count: 1
            out.writeUTF("croppy:tomato");
            out.writeUTF("world");
            out.writeInt(10);
            out.writeInt(65);
            out.writeInt(20);
            out.writeInt(3);
            out.writeDouble(50.0);
            out.writeLong(1000L);
            out.writeLong(2000L);
        }

        byte[] v1Data = baos.toByteArray();
        List<CropInstance> result = CropBinaryCodec.deserialize(v1Data);

        assertEquals(1, result.size());
        CropInstance crop = result.get(0);
        assertEquals(Key.of("croppy", "tomato"), crop.getSpeciesKey());
        assertEquals(BlockPosition.of("world", 10, 65, 20), crop.getPosition());
        assertEquals(3, crop.getStageIndex());
        assertEquals(50.0, crop.getProgress(), 0.001);
        assertEquals(1000L, crop.getPlantedAtMillis());
        assertEquals(2000L, crop.getLastUpdatedMillis());
        // Version 1 defaults
        assertEquals(-1L, crop.getLastWateredDay());
        assertNull(crop.getFertilizerKey());
        assertFalse(crop.hasFertilizer());
    }

    @Test
    @DisplayName("Empty collection returns empty byte array and deserializing empty returns empty list")
    void testEmptyHandling() {
        byte[] emptyBytes = CropBinaryCodec.serialize(Collections.emptyList());
        assertEquals(0, emptyBytes.length);

        List<CropInstance> emptyList = CropBinaryCodec.deserialize(emptyBytes);
        assertTrue(emptyList.isEmpty());
        assertTrue(CropBinaryCodec.deserialize(null).isEmpty());
    }

    @Test
    @DisplayName("Invalid magic or unsupported version throws IllegalArgumentException")
    void testInvalidHeader() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(0x12345678); // wrong magic
            out.writeInt(2);
            out.writeInt(0);
        }
        assertThrows(IllegalArgumentException.class, () -> CropBinaryCodec.deserialize(baos.toByteArray()));

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos2)) {
            out.writeInt(0x43525059); // valid magic
            out.writeInt(999);        // unsupported future version
            out.writeInt(0);
        }
        assertThrows(IllegalArgumentException.class, () -> CropBinaryCodec.deserialize(baos2.toByteArray()));
    }
}
