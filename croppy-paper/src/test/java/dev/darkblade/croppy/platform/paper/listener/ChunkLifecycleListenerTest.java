package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.RotationMode;
import dev.darkblade.croppy.api.species.SpatialFootprint;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.core.species.SpeciesDefinition;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkLifecycleListenerTest {

    private CropManager cropManager;
    private TestCropStorage cropStorage;
    private DefaultSpeciesRegistry speciesRegistry;
    private ChunkLifecycleListener listener;

    private AtomicBoolean syncDisplayCalled;
    private List<BlockPosition> removedDisplays;

    @BeforeEach
    void setUp() {
        cropManager = new CropManager();
        cropStorage = new TestCropStorage();
        speciesRegistry = new DefaultSpeciesRegistry();
        syncDisplayCalled = new AtomicBoolean(false);
        removedDisplays = new ArrayList<>();

        DisplaySynchronizer mockSynchronizer = (crop, species, stage) -> syncDisplayCalled.set(true);

        listener = new ChunkLifecycleListener(
                cropManager,
                cropStorage,
                speciesRegistry,
                mockSynchronizer,
                null,
                null,
                null,
                null,
                Logger.getLogger("TestLogger")
        );
    }

    private Chunk createMockChunk(String worldName, int chunkX, int chunkZ) {
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return worldName;
                    return null;
                }
        );
        return (Chunk) Proxy.newProxyInstance(
                Chunk.class.getClassLoader(),
                new Class<?>[]{Chunk.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return world;
                    if ("getX".equals(method.getName())) return chunkX;
                    if ("getZ".equals(method.getName())) return chunkZ;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Verify onChunkLoad registers multi-block crops with full height and triggers display sync")
    void testChunkLoadRestoresMultiBlockCropAndSyncsDisplay() {
        Key grapesKey = Key.croppy("grapes");
        Species grapes = new SpeciesDefinition(
                grapesKey,
                "Grapes",
                new CropDisplay("model_3d", "minecraft:air", "minecraft:wheat_seeds", RotationMode.CARDINAL_4),
                List.of(new StageDefinition("mature", 1, 0)),
                new CropRequirements(0, 0),
                new HarvestDefinition(List.of()),
                SpatialFootprint.ofHeight(2)
        );
        speciesRegistry.register(grapes);

        BlockPosition rootPos = BlockPosition.of("world_test", 16, 64, 32); // Chunk (1, 2)
        CropInstance crop = new DefaultCropInstance(grapesKey, rootPos);
        cropStorage.storedCrops.add(crop);

        Chunk mockChunk = createMockChunk("world_test", 1, 2);
        ChunkLoadEvent event = new ChunkLoadEvent(mockChunk, false);

        listener.onChunkLoad(event);

        // Verify crop is registered in manager
        assertTrue(cropManager.getCrop(rootPos).isPresent(), "Crop must be registered at root position");
        // Verify secondary multi-block coordinate Y=65 is mapped in occupancy tracker!
        BlockPosition secondaryPos = BlockPosition.of("world_test", 16, 65, 32);
        assertTrue(cropManager.getCropAt(secondaryPos).isPresent(), "Crop must occupy secondary position Y+1");
        assertEquals(rootPos, cropManager.getCropAt(secondaryPos).get().getPosition());

        // Verify immediate display synchronization fired
        assertTrue(syncDisplayCalled.get(), "Immediate display synchronization must fire on chunk load");
    }

    @Test
    @DisplayName("Verify onChunkUnload persists and evicts crops from CropManager")
    void testChunkUnloadPersistsAndEvictsCrops() {
        Key wheatKey = Key.croppy("wheat");
        BlockPosition posInChunk = BlockPosition.of("world_test", 18, 64, 34); // Chunk (1, 2)
        BlockPosition posOtherChunk = BlockPosition.of("world_test", 50, 64, 50); // Chunk (3, 3)

        CropInstance crop1 = new DefaultCropInstance(wheatKey, posInChunk);
        CropInstance crop2 = new DefaultCropInstance(wheatKey, posOtherChunk);

        cropManager.registerCrop(crop1, 1);
        cropManager.registerCrop(crop2, 1);

        Chunk mockChunk = createMockChunk("world_test", 1, 2);
        ChunkUnloadEvent event = new ChunkUnloadEvent(mockChunk, true);

        listener.onChunkUnload(event);

        // Crop in chunk (1, 2) should be evicted from memory and saved
        assertTrue(cropManager.getCrop(posInChunk).isEmpty(), "Crop in unloaded chunk must be evicted");
        assertEquals(1, cropStorage.savedCrops.size(), "Unloaded crop must be saved to storage");
        assertEquals(posInChunk, cropStorage.savedCrops.get(0).getPosition());

        // Crop in chunk (3, 3) should remain active
        assertTrue(cropManager.getCrop(posOtherChunk).isPresent(), "Crop in other chunk must remain active");
    }

    private static class TestCropStorage implements CropStorage {
        final List<CropInstance> storedCrops = new ArrayList<>();
        final List<CropInstance> savedCrops = new ArrayList<>();

        @Override
        public Collection<CropInstance> loadChunk(String worldName, int chunkX, int chunkZ) {
            return storedCrops.stream()
                    .filter(c -> c.getPosition().worldName().equals(worldName)
                            && (c.getPosition().x() >> 4) == chunkX
                            && (c.getPosition().z() >> 4) == chunkZ)
                    .toList();
        }

        @Override
        public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<CropInstance> crops) {
            savedCrops.addAll(crops);
        }

        @Override public void saveCrop(CropInstance crop) {}
        @Override public void deleteCrop(BlockPosition position) {}
    }
}
