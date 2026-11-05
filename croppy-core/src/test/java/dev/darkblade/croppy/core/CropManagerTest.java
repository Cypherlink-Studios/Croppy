package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CropManagerTest {

    private CropManager manager;
    private BlockPosition position;
    private Key wheatKey;

    @BeforeEach
    void setUp() {
        manager = new CropManager();
        position = BlockPosition.of("world", 10, 64, 20);
        wheatKey = Key.croppy("wheat");
    }

    @Test
    @DisplayName("Register crop and lookup by position")
    void testRegisterAndGet() {
        CropInstance crop = new DefaultCropInstance(wheatKey, position);
        manager.registerCrop(crop);

        assertEquals(1, manager.count());
        assertTrue(manager.hasCrop(position));

        Optional<CropInstance> retrieved = manager.getCrop(position);
        assertTrue(retrieved.isPresent());
        assertEquals(wheatKey, retrieved.get().getSpeciesKey());
        assertEquals(position, retrieved.get().getPosition());
        assertEquals(0, retrieved.get().getStageIndex());
        assertEquals(0.0, retrieved.get().getProgress());
    }

    @Test
    @DisplayName("Duplicate crop at same position throws IllegalStateException")
    void testDuplicateCropThrows() {
        CropInstance crop1 = new DefaultCropInstance(wheatKey, position);
        CropInstance crop2 = new DefaultCropInstance(Key.croppy("carrot"), position);

        manager.registerCrop(crop1);

        assertThrows(IllegalStateException.class, () -> manager.registerCrop(crop2));
        assertEquals(wheatKey, manager.getCrop(position).orElseThrow().getSpeciesKey());
    }

    @Test
    @DisplayName("Remove crop cleanly")
    void testRemoveCrop() {
        CropInstance crop = new DefaultCropInstance(wheatKey, position);
        manager.registerCrop(crop);

        Optional<CropInstance> removed = manager.removeCrop(position);
        assertTrue(removed.isPresent());
        assertEquals(wheatKey, removed.get().getSpeciesKey());

        assertFalse(manager.hasCrop(position));
        assertEquals(0, manager.count());
        assertTrue(manager.getCrop(position).isEmpty());
    }

    @Test
    @DisplayName("Stage and progress advancement updates timestamp")
    void testProgressAndStageAdvancement() {
        CropInstance crop = new DefaultCropInstance(wheatKey, position);
        long initialTimestamp = crop.getLastUpdatedMillis();

        crop.addProgress(50.0);
        assertEquals(50.0, crop.getProgress());
        assertTrue(crop.getLastUpdatedMillis() >= initialTimestamp);

        crop.setStageIndex(1);
        assertEquals(1, crop.getStageIndex());
    }

    @Test
    @DisplayName("CropManager wired in CroppyCore lifecycle")
    void testCoreLifecycleWiring() {
        CroppyCore core = new CroppyCore();
        core.initialize();

        CropManager coreManager = core.getCropManager();
        assertNotNull(coreManager);
        coreManager.registerCrop(new DefaultCropInstance(wheatKey, position));
        assertEquals(1, coreManager.count());

        core.shutdown();
        assertEquals(0, coreManager.count(), "CropManager must be cleared on shutdown");
    }

    @Test
    @DisplayName("Multi-block crop registration, occupancy lookup, and collision rejection")
    void testMultiBlockCropOccupancy() {
        CropInstance grapeCrop = new DefaultCropInstance(Key.croppy("grapes"), position);
        BlockPosition abovePos = BlockPosition.of("world", 10, 65, 20);

        manager.registerCrop(grapeCrop, 2);

        assertEquals(1, manager.count());
        assertTrue(manager.hasCrop(position));
        assertTrue(manager.hasCrop(abovePos));
        assertTrue(manager.isOccupied(abovePos));

        // Direct root lookup
        Optional<CropInstance> rootLookup = manager.getCrop(position);
        assertTrue(rootLookup.isPresent());
        assertEquals(grapeCrop, rootLookup.get());

        // Secondary position lookup resolves to root instance
        Optional<CropInstance> secondaryLookup = manager.getCropAt(abovePos);
        assertTrue(secondaryLookup.isPresent());
        assertSame(grapeCrop, secondaryLookup.get());

        // Collision: trying to register another crop at the secondary position must fail
        CropInstance conflictingCrop = new DefaultCropInstance(Key.croppy("carrot"), abovePos);
        assertThrows(IllegalStateException.class, () -> manager.registerCrop(conflictingCrop));

        // Removing by secondary position removes the whole multi-block instance cleanly
        Optional<CropInstance> removed = manager.removeCrop(abovePos);
        assertTrue(removed.isPresent());
        assertSame(grapeCrop, removed.get());

        assertFalse(manager.isOccupied(position));
        assertFalse(manager.isOccupied(abovePos));
        assertEquals(0, manager.count());
    }
}
