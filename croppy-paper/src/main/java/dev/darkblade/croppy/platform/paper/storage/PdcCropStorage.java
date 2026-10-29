package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.storage.CropBinaryCodec;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Storage implementation persisting crop instances within Chunk PersistentDataContainers (PDC).
 */
public class PdcCropStorage implements CropStorage {

    private final Server server;
    private final NamespacedKey storageKey;

    public PdcCropStorage(Plugin plugin) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.server = plugin.getServer();
        this.storageKey = new NamespacedKey(plugin, "crops");
    }

    public PdcCropStorage(Server server, NamespacedKey storageKey) {
        this.server = Objects.requireNonNull(server, "Server cannot be null");
        this.storageKey = Objects.requireNonNull(storageKey, "Storage key cannot be null");
    }

    @Override
    public Collection<CropInstance> loadChunk(String worldName, int chunkX, int chunkZ) {
        World world = server.getWorld(worldName);
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
            return Collections.emptyList();
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        byte[] bytes = pdc.get(storageKey, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) {
            return Collections.emptyList();
        }

        return CropBinaryCodec.deserialize(bytes);
    }

    @Override
    public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<CropInstance> crops) {
        World world = server.getWorld(worldName);
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        if (crops == null || crops.isEmpty()) {
            pdc.remove(storageKey);
        } else {
            byte[] bytes = CropBinaryCodec.serialize(crops);
            pdc.set(storageKey, PersistentDataType.BYTE_ARRAY, bytes);
        }
    }

    @Override
    public void saveCrop(CropInstance crop) {
        Objects.requireNonNull(crop, "Crop cannot be null");
        BlockPosition pos = crop.getPosition();
        int chunkX = pos.x() >> 4;
        int chunkZ = pos.z() >> 4;

        List<CropInstance> crops = new ArrayList<>(loadChunk(pos.worldName(), chunkX, chunkZ));
        crops.removeIf(existing -> existing.getPosition().equals(pos));
        crops.add(crop);

        saveChunk(pos.worldName(), chunkX, chunkZ, crops);
    }

    @Override
    public void deleteCrop(BlockPosition position) {
        Objects.requireNonNull(position, "Position cannot be null");
        int chunkX = position.x() >> 4;
        int chunkZ = position.z() >> 4;

        List<CropInstance> crops = new ArrayList<>(loadChunk(position.worldName(), chunkX, chunkZ));
        boolean removed = crops.removeIf(existing -> existing.getPosition().equals(position));

        if (removed) {
            saveChunk(position.worldName(), chunkX, chunkZ, crops);
        }
    }
}
