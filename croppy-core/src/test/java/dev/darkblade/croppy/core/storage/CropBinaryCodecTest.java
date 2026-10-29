package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CropBinaryCodecTest {

    @Test
    @DisplayName("Roundtrip serialization and deserialization of multiple crops")
    void testRoundtripMultipleCrops() {
        CropInstance c1 = new DefaultCropInstance(
                Key.croppy("wheat"),
                BlockPosition.of("world", 100, 64, -200),
                2,
                15.75,
                1000000L,
                2000000L
        );
        CropInstance c2 = new DefaultCropInstance(
                Key.of("custom", "carrot"),
                BlockPosition.of("world_nether", -50, 70, 300),
                0,
                0.0,
                3000000L,
                3000000L
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

        CropInstance d2 = deserialized.get(1);
        assertEquals(c2.getSpeciesKey(), d2.getSpeciesKey());
        assertEquals(c2.getPosition(), d2.getPosition());
        assertEquals(c2.getStageIndex(), d2.getStageIndex());
        assertEquals(c2.getProgress(), d2.getProgress(), 0.0001);
        assertEquals(c2.getPlantedAtMillis(), d2.getPlantedAtMillis());
        assertEquals(c2.getLastUpdatedMillis(), d2.getLastUpdatedMillis());
    }

    @Test
    @DisplayName("Empty collection returns empty byte array and empty list on deserialization")
    void testEmptyCollection() {
        byte[] empty = CropBinaryCodec.serialize(Collections.emptyList());
        assertEquals(0, empty.length);

        List<CropInstance> result = CropBinaryCodec.deserialize(empty);
        assertTrue(result.isEmpty());

        List<CropInstance> nullResult = CropBinaryCodec.deserialize(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    @DisplayName("Corrupt magic header throws IllegalArgumentException")
    void testCorruptMagicHeader() {
        byte[] corrupt = new byte[]{0x00, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00, 0x01};
        assertThrows(IllegalArgumentException.class, () -> CropBinaryCodec.deserialize(corrupt));
    }
}
