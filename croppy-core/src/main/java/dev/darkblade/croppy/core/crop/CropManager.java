package dev.darkblade.croppy.core.crop;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe core manager tracking live crop instances and their multi-block spatial occupancy across worlds.
 */
public class CropManager implements Service {

    private final Map<BlockPosition, CropInstance> primaryCrops = new ConcurrentHashMap<>();
    private final Map<BlockPosition, BlockPosition> occupancyMap = new ConcurrentHashMap<>();

    /**
     * Registers a new single-block crop instance.
     *
     * @param crop the crop instance to register
     * @throws IllegalStateException if a crop already exists at that position or any occupied coordinate
     */
    public void registerCrop(CropInstance crop) {
        registerCrop(crop, 1);
    }

    /**
     * Registers a crop instance spanning a vertical height.
     *
     * @param crop   the crop instance to register
     * @param height vertical height in blocks (must be >= 1)
     * @throws IllegalStateException if any coordinate in the vertical footprint is already occupied
     */
    public void registerCrop(CropInstance crop, int height) {
        Objects.requireNonNull(crop, "CropInstance cannot be null");
        if (height < 1) {
            throw new IllegalArgumentException("Height must be at least 1, got: " + height);
        }
        BlockPosition rootPos = Objects.requireNonNull(crop.getPosition(), "Crop position cannot be null");

        List<BlockPosition> positions = new ArrayList<>(height);
        for (int dy = 0; dy < height; dy++) {
            positions.add(new BlockPosition(rootPos.worldName(), rootPos.x(), rootPos.y() + dy, rootPos.z()));
        }
        registerCrop(crop, positions);
    }

    /**
     * Registers a crop instance spanning explicit occupied coordinates.
     *
     * @param crop              the crop instance to register
     * @param occupiedPositions all block positions occupied by this crop
     * @throws IllegalStateException if any coordinate is already occupied
     */
    public synchronized void registerCrop(CropInstance crop, Collection<BlockPosition> occupiedPositions) {
        Objects.requireNonNull(crop, "CropInstance cannot be null");
        BlockPosition rootPos = Objects.requireNonNull(crop.getPosition(), "Crop position cannot be null");
        Objects.requireNonNull(occupiedPositions, "OccupiedPositions cannot be null");

        if (primaryCrops.containsKey(rootPos) || occupancyMap.containsKey(rootPos)) {
            throw new IllegalStateException("A crop is already tracked at root position: " + rootPos);
        }

        for (BlockPosition pos : occupiedPositions) {
            if (primaryCrops.containsKey(pos) || occupancyMap.containsKey(pos)) {
                throw new IllegalStateException("A crop already occupies spatial position: " + pos);
            }
        }

        primaryCrops.put(rootPos, crop);
        occupancyMap.put(rootPos, rootPos);
        for (BlockPosition pos : occupiedPositions) {
            occupancyMap.put(pos, rootPos);
        }
    }

    /**
     * Finds a crop instance by its root position.
     *
     * @param position root block position
     * @return Optional containing the crop instance if present
     */
    public Optional<CropInstance> getCrop(BlockPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(primaryCrops.get(position));
    }

    /**
     * Finds a crop instance occupying the given position (either as root or multi-block footprint).
     *
     * @param position block position
     * @return Optional containing the crop instance occupying the coordinate
     */
    public Optional<CropInstance> getCropAt(BlockPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        CropInstance primary = primaryCrops.get(position);
        if (primary != null) {
            return Optional.of(primary);
        }
        BlockPosition rootPos = occupancyMap.get(position);
        if (rootPos != null) {
            return Optional.ofNullable(primaryCrops.get(rootPos));
        }
        return Optional.empty();
    }

    /**
     * Checks if any crop footprint occupies the given position.
     *
     * @param position block position
     * @return true if the coordinate is occupied by a crop
     */
    public boolean isOccupied(BlockPosition position) {
        if (position == null) {
            return false;
        }
        return primaryCrops.containsKey(position) || occupancyMap.containsKey(position);
    }

    /**
     * Checks if a crop is currently tracked at the given position.
     *
     * @param position block position
     * @return true if a crop exists or occupies the position
     */
    public boolean hasCrop(BlockPosition position) {
        return isOccupied(position);
    }

    /**
     * Removes and returns a crop instance occupying the given position (either as root or secondary footprint).
     *
     * @param position block position
     * @return Optional containing the removed crop instance if it existed
     */
    public synchronized Optional<CropInstance> removeCrop(BlockPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        BlockPosition rootPos = occupancyMap.remove(position);
        if (rootPos == null) {
            rootPos = position;
        }
        CropInstance removed = primaryCrops.remove(rootPos);
        if (removed != null) {
            final BlockPosition finalRoot = rootPos;
            occupancyMap.values().removeIf(target -> target.equals(finalRoot));
        }
        return Optional.ofNullable(removed);
    }

    /**
     * @return an unmodifiable collection of all active primary crop instances
     */
    public Collection<CropInstance> getAllCrops() {
        return Collections.unmodifiableCollection(primaryCrops.values());
    }

    /**
     * @return total count of active crop instances
     */
    public int count() {
        return primaryCrops.size();
    }

    /**
     * Clears all active crop instances and spatial occupancy records.
     */
    public synchronized void clear() {
        primaryCrops.clear();
        occupancyMap.clear();
    }
}
