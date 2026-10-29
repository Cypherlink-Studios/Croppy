package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.core.crop.CropManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates in-memory crop instances with chunk loading and unloading.
 */
public class ChunkLifecycleListener implements Listener {

    private final CropManager cropManager;
    private final CropStorage storage;
    private final Logger logger;

    public ChunkLifecycleListener(CropManager cropManager, CropStorage storage, Logger logger) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        try {
            Collection<CropInstance> crops = storage.loadChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            for (CropInstance crop : crops) {
                if (!cropManager.hasCrop(crop.getPosition())) {
                    cropManager.registerCrop(crop);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading crops for chunk [" + chunk.getX() + ", " + chunk.getZ() + "]", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        try {
            List<CropInstance> chunkCrops = new ArrayList<>();
            for (CropInstance crop : cropManager.getAllCrops()) {
                if (crop.getPosition().worldName().equals(worldName)
                        && (crop.getPosition().x() >> 4) == chunkX
                        && (crop.getPosition().z() >> 4) == chunkZ) {
                    chunkCrops.add(crop);
                }
            }

            // Save chunk state to persistent storage
            storage.saveChunk(worldName, chunkX, chunkZ, chunkCrops);

            // Evict crops from active in-memory tracker to maintain minimal RAM usage
            for (CropInstance crop : chunkCrops) {
                cropManager.removeCrop(crop.getPosition());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving crops for unloaded chunk [" + chunkX + ", " + chunkZ + "]", e);
        }
    }
}
