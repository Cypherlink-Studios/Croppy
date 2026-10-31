package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.storage.StationStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.storage.StationBinaryCodec;
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
 * Paper adapter implementation persisting station instances within Chunk PersistentDataContainers (PDC).
 */
public final class PdcStationStorage implements StationStorage {

    private final Server server;
    private final NamespacedKey storageKey;
    private final StationRegistry stationRegistry;

    public PdcStationStorage(Plugin plugin, StationRegistry stationRegistry) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        this.server = plugin.getServer();
        this.storageKey = new NamespacedKey(plugin, "stations");
        this.stationRegistry = Objects.requireNonNull(stationRegistry, "stationRegistry cannot be null");
    }

    public PdcStationStorage(Server server, NamespacedKey storageKey, StationRegistry stationRegistry) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.storageKey = Objects.requireNonNull(storageKey, "storageKey cannot be null");
        this.stationRegistry = Objects.requireNonNull(stationRegistry, "stationRegistry cannot be null");
    }

    @Override
    public Collection<StationInstance> loadChunk(String worldName, int chunkX, int chunkZ) {
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

        return StationBinaryCodec.deserialize(bytes, stationRegistry::get);
    }

    @Override
    public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<StationInstance> stations) {
        World world = server.getWorld(worldName);
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (stations == null || stations.isEmpty()) {
            pdc.remove(storageKey);
            return;
        }

        byte[] bytes = StationBinaryCodec.serialize(stations);
        pdc.set(storageKey, PersistentDataType.BYTE_ARRAY, bytes);
    }

    @Override
    public void deleteChunk(String worldName, int chunkX, int chunkZ) {
        World world = server.getWorld(worldName);
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        chunk.getPersistentDataContainer().remove(storageKey);
    }

    @Override
    public void saveStation(StationInstance station) {
        Objects.requireNonNull(station, "station cannot be null");
        BlockPosition pos = station.getPosition();
        int chunkX = pos.x() >> 4;
        int chunkZ = pos.z() >> 4;

        List<StationInstance> current = new ArrayList<>(loadChunk(pos.worldName(), chunkX, chunkZ));
        current.removeIf(s -> s.getPosition().equals(pos));
        current.add(station);

        saveChunk(pos.worldName(), chunkX, chunkZ, current);
    }

    @Override
    public void deleteStation(BlockPosition position) {
        Objects.requireNonNull(position, "position cannot be null");
        int chunkX = position.x() >> 4;
        int chunkZ = position.z() >> 4;

        List<StationInstance> current = new ArrayList<>(loadChunk(position.worldName(), chunkX, chunkZ));
        boolean removed = current.removeIf(s -> s.getPosition().equals(position));
        if (removed) {
            saveChunk(position.worldName(), chunkX, chunkZ, current);
        }
    }
}
