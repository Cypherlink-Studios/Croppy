package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.api.world.ChunkPosition;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultStationManagerTest {

    private DefaultStationManager stationManager;
    private StationDefinition testDefinition;

    @BeforeEach
    void setUp() {
        stationManager = new DefaultStationManager();
        testDefinition = new StationDefinition(
                Key.of("croppy", "keg"),
                "<yellow>Keg</yellow>",
                "minecraft:barrel",
                StationInteractionType.HYBRID,
                1,
                1,
                true,
                List.of(),
                Map.of()
        );
    }

    @Test
    void testAddAndRetrieveStation() {
        BlockPosition pos = new BlockPosition("world", 10, 64, 20);
        DefaultStationInstance instance = new DefaultStationInstance(testDefinition.key(), pos, testDefinition);
        instance.setComponent(StorageComponent.class, new DefaultStorageComponent(1, 1));
        instance.setComponent(DisplayComponent.class, new DefaultDisplayComponent());

        stationManager.addStation(instance);

        assertEquals(1, stationManager.getStationCount());
        Optional<StationInstance> retrieved = stationManager.getStation(pos);
        assertTrue(retrieved.isPresent());
        assertEquals(testDefinition.key(), retrieved.get().getStationKey());
        assertTrue(retrieved.get().getComponent(StorageComponent.class).isPresent());
        assertTrue(retrieved.get().getComponent(DisplayComponent.class).isPresent());
    }

    @Test
    void testChunkSpatialIndexing() {
        // Position at block (16, 64, 32) -> chunk (1, 2)
        BlockPosition pos1 = new BlockPosition("world", 16, 64, 32);
        BlockPosition pos2 = new BlockPosition("world", 17, 64, 33);
        // Position at block (0, 64, 0) -> chunk (0, 0)
        BlockPosition pos3 = new BlockPosition("world", 0, 64, 0);

        stationManager.addStation(new DefaultStationInstance(testDefinition.key(), pos1, testDefinition));
        stationManager.addStation(new DefaultStationInstance(testDefinition.key(), pos2, testDefinition));
        stationManager.addStation(new DefaultStationInstance(testDefinition.key(), pos3, testDefinition));

        ChunkPosition targetChunk = new ChunkPosition("world", 1, 2);
        Set<StationInstance> chunkStations = stationManager.getStationsInChunk(targetChunk);
        assertEquals(2, chunkStations.size());

        Set<StationInstance> removedChunk = stationManager.removeStationsInChunk(targetChunk);
        assertEquals(2, removedChunk.size());
        assertEquals(1, stationManager.getStationCount());
        assertTrue(stationManager.getStationsInChunk(targetChunk).isEmpty());
    }

    @Test
    void testRemoveStation() {
        BlockPosition pos = new BlockPosition("world", 5, 70, 5);
        stationManager.addStation(new DefaultStationInstance(testDefinition.key(), pos, testDefinition));

        Optional<StationInstance> removed = stationManager.removeStation(pos);
        assertTrue(removed.isPresent());
        assertEquals(0, stationManager.getStationCount());
        assertFalse(stationManager.getStation(pos).isPresent());
    }
}
