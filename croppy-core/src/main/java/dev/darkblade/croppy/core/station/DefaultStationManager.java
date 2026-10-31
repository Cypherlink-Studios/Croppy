package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.api.world.ChunkPosition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory manager tracking active {@link StationInstance} objects by spatial coordinates.
 */
public final class DefaultStationManager implements Service {

    private final Map<BlockPosition, StationInstance> stations = new ConcurrentHashMap<>();
    private final Map<ChunkPosition, Set<BlockPosition>> chunkIndex = new ConcurrentHashMap<>();

    /**
     * Registers a station instance in the spatial index.
     *
     * @param station the station instance to add
     */
    public void addStation(StationInstance station) {
        Objects.requireNonNull(station, "station cannot be null");
        BlockPosition pos = station.getPosition();
        stations.put(pos, station);

        ChunkPosition chunkPos = pos.toChunkPosition();
        chunkIndex.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    /**
     * Retrieves the station instance at the given position, if one exists.
     *
     * @param position world block position
     * @return optional containing the station instance
     */
    public Optional<StationInstance> getStation(BlockPosition position) {
        Objects.requireNonNull(position, "position cannot be null");
        return Optional.ofNullable(stations.get(position));
    }

    /**
     * Removes and returns the station at the specified position.
     *
     * @param position world block position
     * @return optional containing the removed station
     */
    public Optional<StationInstance> removeStation(BlockPosition position) {
        Objects.requireNonNull(position, "position cannot be null");
        StationInstance removed = stations.remove(position);
        if (removed != null) {
            ChunkPosition chunkPos = position.toChunkPosition();
            Set<BlockPosition> positions = chunkIndex.get(chunkPos);
            if (positions != null) {
                positions.remove(position);
                if (positions.isEmpty()) {
                    chunkIndex.remove(chunkPos, Collections.emptySet());
                }
            }
        }
        return Optional.ofNullable(removed);
    }

    /**
     * Returns all active stations within the specified chunk.
     *
     * @param chunkPosition chunk coordinates
     * @return set of stations in that chunk
     */
    public Set<StationInstance> getStationsInChunk(ChunkPosition chunkPosition) {
        Objects.requireNonNull(chunkPosition, "chunkPosition cannot be null");
        Set<BlockPosition> positions = chunkIndex.get(chunkPosition);
        if (positions == null || positions.isEmpty()) {
            return Set.of();
        }

        Set<StationInstance> result = new HashSet<>();
        for (BlockPosition pos : positions) {
            StationInstance station = stations.get(pos);
            if (station != null) {
                result.add(station);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Removes and returns all stations located within the specified chunk.
     *
     * @param chunkPosition chunk coordinates
     * @return set of removed stations
     */
    public Set<StationInstance> removeStationsInChunk(ChunkPosition chunkPosition) {
        Objects.requireNonNull(chunkPosition, "chunkPosition cannot be null");
        Set<BlockPosition> positions = chunkIndex.remove(chunkPosition);
        if (positions == null || positions.isEmpty()) {
            return Set.of();
        }

        Set<StationInstance> removedStations = new HashSet<>();
        for (BlockPosition pos : positions) {
            StationInstance removed = stations.remove(pos);
            if (removed != null) {
                removedStations.add(removed);
            }
        }
        return Collections.unmodifiableSet(removedStations);
    }

    /**
     * @return unmodifiable collection view of all registered stations
     */
    public Collection<StationInstance> getAllStations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    /**
     * @return total number of active stations tracked
     */
    public int getStationCount() {
        return stations.size();
    }

    /**
     * Clears all stations from memory.
     */
    public void clear() {
        stations.clear();
        chunkIndex.clear();
    }
}
