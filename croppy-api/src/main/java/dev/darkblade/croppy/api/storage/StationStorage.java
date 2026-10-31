package dev.darkblade.croppy.api.storage;

import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Collection;

/**
 * Port interface for persistent storage and retrieval of {@link StationInstance} records.
 */
public interface StationStorage {

    /**
     * Loads all station instances located in the specified chunk.
     *
     * @param worldName world name
     * @param chunkX   chunk X coordinate
     * @param chunkZ   chunk Z coordinate
     * @return collection of loaded station instances
     */
    Collection<StationInstance> loadChunk(String worldName, int chunkX, int chunkZ);

    /**
     * Persists all station instances located in the specified chunk.
     *
     * @param worldName world name
     * @param chunkX   chunk X coordinate
     * @param chunkZ   chunk Z coordinate
     * @param stations stations to persist
     */
    void saveChunk(String worldName, int chunkX, int chunkZ, Collection<StationInstance> stations);

    /**
     * Deletes all persisted station records for the specified chunk.
     *
     * @param worldName world name
     * @param chunkX   chunk X coordinate
     * @param chunkZ   chunk Z coordinate
     */
    void deleteChunk(String worldName, int chunkX, int chunkZ);

    /**
     * Saves or updates a single station instance.
     *
     * @param station station instance to persist
     */
    void saveStation(StationInstance station);

    /**
     * Deletes a single station at the specified block coordinate.
     *
     * @param position spatial coordinate of the station
     */
    void deleteStation(BlockPosition position);
}
