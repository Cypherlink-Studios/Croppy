package dev.darkblade.croppy.api.storage;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Collection;

/**
 * Service port interface for persistent crop state storage.
 * Supports chunk-level batch loading and individual/batch saving.
 */
public interface CropStorage extends Service {

    /**
     * Initializes the storage engine, database connections, or storage keys.
     */
    default void initialize() {}

    /**
     * Shuts down the storage engine, flushing active buffers and closing connection pools.
     */
    default void shutdown() {}

    /**
     * Loads all persistent crop instances located in the specified chunk.
     *
     * @param worldName the name of the world
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return collection of restored crop instances
     */
    Collection<CropInstance> loadChunk(String worldName, int chunkX, int chunkZ);

    /**
     * Saves all crop instances belonging to the specified chunk.
     * If the collection is empty, any previously stored crops for this chunk should be removed.
     *
     * @param worldName the name of the world
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @param crops the active crops in the chunk
     */
    void saveChunk(String worldName, int chunkX, int chunkZ, Collection<CropInstance> crops);

    /**
     * Saves or updates a single crop instance.
     *
     * @param crop the crop instance to save
     */
    void saveCrop(CropInstance crop);

    /**
     * Deletes a crop instance at the given position.
     *
     * @param position the block position of the crop to delete
     */
    void deleteCrop(BlockPosition position);
}
