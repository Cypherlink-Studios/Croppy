package dev.darkblade.croppy.core.crop;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe core manager tracking live crop instances across worlds.
 */
public class CropManager implements Service {

    private final Map<BlockPosition, CropInstance> crops = new ConcurrentHashMap<>();

    /**
     * Registers a new crop instance.
     *
     * @param crop the crop instance to register
     * @throws IllegalStateException if a crop already exists at that position
     */
    public void registerCrop(CropInstance crop) {
        Objects.requireNonNull(crop, "CropInstance cannot be null");
        BlockPosition pos = Objects.requireNonNull(crop.getPosition(), "Crop position cannot be null");

        CropInstance existing = crops.putIfAbsent(pos, crop);
        if (existing != null) {
            throw new IllegalStateException("A crop is already tracked at position: " + pos);
        }
    }

    /**
     * Finds a crop instance at the given position.
     *
     * @param position block position
     * @return Optional containing the crop instance if present
     */
    public Optional<CropInstance> getCrop(BlockPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(crops.get(position));
    }

    /**
     * Removes and returns a crop instance at the given position.
     *
     * @param position block position
     * @return Optional containing the removed crop instance if it existed
     */
    public Optional<CropInstance> removeCrop(BlockPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(crops.remove(position));
    }

    /**
     * Checks if a crop is currently tracked at the given position.
     *
     * @param position block position
     * @return true if a crop exists at the position
     */
    public boolean hasCrop(BlockPosition position) {
        if (position == null) {
            return false;
        }
        return crops.containsKey(position);
    }

    /**
     * @return an unmodifiable collection of all active crop instances
     */
    public Collection<CropInstance> getAllCrops() {
        return Collections.unmodifiableCollection(crops.values());
    }

    /**
     * @return total count of active crop instances
     */
    public int count() {
        return crops.size();
    }

    /**
     * Clears all active crop instances.
     */
    public void clear() {
        crops.clear();
    }
}
