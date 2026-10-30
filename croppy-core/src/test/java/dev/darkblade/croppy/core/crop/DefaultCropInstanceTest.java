package dev.darkblade.croppy.core.crop;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCropInstanceTest {

    @Test
    @DisplayName("Should initialize default values correctly")
    void shouldInitializeDefaultValues() {
        Key species = Key.of("croppy", "tomato");
        BlockPosition pos = BlockPosition.of("world", 10, 64, -20);
        DefaultCropInstance crop = new DefaultCropInstance(species, pos);

        assertEquals(species, crop.getSpeciesKey());
        assertEquals(pos, crop.getPosition());
        assertEquals(0, crop.getStageIndex());
        assertEquals(0.0, crop.getProgress());
        assertEquals(-1L, crop.getLastWateredDay());
        assertNull(crop.getFertilizerKey());
        assertFalse(crop.hasFertilizer());
        assertFalse(crop.isWateredOn(0));
    }

    @Test
    @DisplayName("Should update hydration day and fertilizer correctly")
    void shouldUpdateHydrationAndFertilizer() {
        Key species = Key.of("croppy", "corn");
        BlockPosition pos = BlockPosition.of("world", 5, 70, 15);
        DefaultCropInstance crop = new DefaultCropInstance(species, pos);

        crop.setLastWateredDay(42);
        assertEquals(42, crop.getLastWateredDay());
        assertTrue(crop.isWateredOn(42));
        assertFalse(crop.isWateredOn(43));

        Key fertilizer = Key.of("croppy", "speed_gro");
        crop.setFertilizerKey(fertilizer);
        assertEquals(fertilizer, crop.getFertilizerKey());
        assertTrue(crop.hasFertilizer());

        crop.setFertilizerKey(null);
        assertNull(crop.getFertilizerKey());
        assertFalse(crop.hasFertilizer());
    }

    @Test
    @DisplayName("Should construct with full argument list")
    void shouldConstructWithFullArguments() {
        Key species = Key.of("croppy", "wheat");
        BlockPosition pos = BlockPosition.of("farm", 0, 60, 0);
        Key fertilizer = Key.of("croppy", "retaining_soil");

        DefaultCropInstance crop = new DefaultCropInstance(species, pos, 2, 45.5, 1000L, 2000L, 10L, fertilizer);

        assertEquals(2, crop.getStageIndex());
        assertEquals(45.5, crop.getProgress());
        assertEquals(1000L, crop.getPlantedAtMillis());
        assertEquals(2000L, crop.getLastUpdatedMillis());
        assertEquals(10L, crop.getLastWateredDay());
        assertEquals(fertilizer, crop.getFertilizerKey());
        assertTrue(crop.hasFertilizer());
    }
}
