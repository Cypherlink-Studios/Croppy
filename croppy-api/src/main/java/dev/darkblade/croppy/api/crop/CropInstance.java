package dev.darkblade.croppy.api.crop;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;

/**
 * Represents a living crop instance planted in the world.
 */
public interface CropInstance {

    /**
     * @return the key of the crop species
     */
    Key getSpeciesKey();

    /**
     * @return spatial location of this crop in the world
     */
    BlockPosition getPosition();

    /**
     * @return current developmental stage index (0-based)
     */
    int getStageIndex();

    /**
     * Sets the current developmental stage index.
     *
     * @param stageIndex new stage index
     */
    void setStageIndex(int stageIndex);

    /**
     * @return accumulated progress points towards the next developmental stage
     */
    double getProgress();

    /**
     * Sets accumulated progress points.
     *
     * @param progress new progress value
     */
    void setProgress(double progress);

    /**
     * Adds progress points.
     *
     * @param amount amount to add
     * @return updated progress
     */
    default double addProgress(double amount) {
        setProgress(getProgress() + amount);
        return getProgress();
    }

    /**
     * @return epoch milliseconds when this crop was planted
     */
    long getPlantedAtMillis();

    /**
     * @return epoch milliseconds when this crop was last evaluated or updated
     */
    long getLastUpdatedMillis();

    /**
     * Updates the lastUpdated timestamp to the current system time.
     */
    void updateTimestamp();
}
