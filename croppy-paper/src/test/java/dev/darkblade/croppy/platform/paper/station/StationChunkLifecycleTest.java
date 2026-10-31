package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.storage.StationStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.listener.ChunkLifecycleListener;
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
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StationChunkLifecycleTest {

    private CropManager cropManager;
    private DefaultStationManager stationManager;
    private TestCropStorage cropStorage;
    private TestStationStorage stationStorage;
    private ChunkLifecycleListener listener;

    @BeforeEach
    void setUp() {
        cropManager = new CropManager();
        stationManager = new DefaultStationManager();
        cropStorage = new TestCropStorage();
        stationStorage = new TestStationStorage();
        listener = new ChunkLifecycleListener(
                cropManager,
                cropStorage,
                stationManager,
                stationStorage,
                Logger.getLogger("TestLogger")
        );
    }

    @Test
    @DisplayName("Verify onChunkLoad restores station instances into StationManager")
    void testChunkLoadRestoresStations() {
        Key potKey = Key.of("croppy", "cooking_pot");
        StationDefinition def = new StationDefinition(
                potKey, "Cooking Pot", "minecraft:player_head",
                StationInteractionType.HYBRID, 6, 2, true, List.of(), Map.of()
        );
        BlockPosition pos = BlockPosition.of("world_test", 16, 64, 32); // Chunk (1, 2)
        DefaultStationInstance station = new DefaultStationInstance(potKey, pos, def);
        stationStorage.storedStations.add(station);

        Chunk mockChunk = createMockChunk("world_test", 1, 2);
        ChunkLoadEvent event = new ChunkLoadEvent(mockChunk, false);

        listener.onChunkLoad(event);

        assertTrue(stationManager.getStation(pos).isPresent(), "Station should be loaded into manager on chunk load");
        assertEquals(potKey, stationManager.getStation(pos).get().getStationKey());
    }

    @Test
    @DisplayName("Verify onChunkUnload persists and evicts station instances from StationManager")
    void testChunkUnloadPersistsAndEvictsStations() {
        Key skilletKey = Key.of("croppy", "skillet");
        StationDefinition def = new StationDefinition(
                skilletKey, "Skillet", "minecraft:heavy_weighted_pressure_plate",
                StationInteractionType.PHYSICAL, 4, 2, true, List.of(), Map.of()
        );
        BlockPosition posInChunk = BlockPosition.of("world_test", 18, 64, 34); // Chunk (1, 2)
        BlockPosition posOtherChunk = BlockPosition.of("world_test", 50, 64, 50); // Chunk (3, 3)

        DefaultStationInstance station1 = new DefaultStationInstance(skilletKey, posInChunk, def);
        DefaultStationInstance station2 = new DefaultStationInstance(skilletKey, posOtherChunk, def);

        stationManager.addStation(station1);
        stationManager.addStation(station2);

        Chunk mockChunk = createMockChunk("world_test", 1, 2);
        ChunkUnloadEvent event = new ChunkUnloadEvent(mockChunk, true);

        listener.onChunkUnload(event);

        // Station in chunk (1, 2) should be persisted and evicted
        assertTrue(stationManager.getStation(posInChunk).isEmpty(), "Station in unloaded chunk should be evicted");
        assertEquals(1, stationStorage.savedStations.size(), "Unloaded stations should be persisted to storage");
        assertEquals(posInChunk, stationStorage.savedStations.get(0).getPosition());

        // Station in chunk (3, 3) should still remain active in memory
        assertTrue(stationManager.getStation(posOtherChunk).isPresent(), "Station in other chunk must remain active");
    }

    private Chunk createMockChunk(String worldName, int chunkX, int chunkZ) {
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return worldName;
                    return null;
                }
        );
        return (Chunk) Proxy.newProxyInstance(
                Chunk.class.getClassLoader(),
                new Class<?>[]{Chunk.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    if (method.getName().equals("getX")) return chunkX;
                    if (method.getName().equals("getZ")) return chunkZ;
                    return null;
                }
        );
    }

    private static class TestCropStorage implements CropStorage {
        @Override public Collection<CropInstance> loadChunk(String worldName, int chunkX, int chunkZ) { return List.of(); }
        @Override public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<CropInstance> crops) {}
        @Override public void saveCrop(CropInstance crop) {}
        @Override public void deleteCrop(BlockPosition position) {}
    }

    private static class TestStationStorage implements StationStorage {
        final List<StationInstance> storedStations = new ArrayList<>();
        final List<StationInstance> savedStations = new ArrayList<>();

        @Override
        public Collection<StationInstance> loadChunk(String worldName, int chunkX, int chunkZ) {
            return storedStations.stream()
                    .filter(s -> s.getPosition().worldName().equals(worldName)
                            && (s.getPosition().x() >> 4) == chunkX
                            && (s.getPosition().z() >> 4) == chunkZ)
                    .toList();
        }

        @Override
        public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<StationInstance> stations) {
            savedStations.addAll(stations);
        }

        @Override public void deleteChunk(String worldName, int chunkX, int chunkZ) {}
        @Override public void saveStation(StationInstance station) {}
        @Override public void deleteStation(BlockPosition position) {}
    }
}
