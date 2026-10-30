package dev.darkblade.croppy.api.crop;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.Species;
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

    /**
     * Advances the developmental stage index by 1, resets progress to 0.0, and updates timestamp.
     */
    default void advanceStage() {
        setStageIndex(getStageIndex() + 1);
        setProgress(0.0);
        updateTimestamp();
    }

    /**
     * Checks if this crop instance has reached the mature stage of the given species.
     *
     * @param species the species definition
     * @return true if mature
     */
    default boolean isMature(Species species) {
        return species != null && species.isMature(getStageIndex());
    }

    /**
     * @return world day index (world.getFullTime() / 24000L) when this crop was last watered, or -1 if unwatered
     */
    long getLastWateredDay();

    /**
     * Sets the world day index when this crop was last watered.
     *
     * @param lastWateredDay world day index
     */
    void setLastWateredDay(long lastWateredDay);

    /**
     * Checks if this crop is considered watered on the specified world day.
     *
     * @param currentDay the current world day index
     * @return true if last watered day equals current day
     */
    default boolean isWateredOn(long currentDay) {
        return getLastWateredDay() == currentDay;
    }

    /**
     * @return the key of the active fertilizer applied to this crop, or null if none
     */
    Key getFertilizerKey();

    /**
     * Sets the active fertilizer key applied to this crop.
     *
     * @param fertilizerKey fertilizer key or null
     */
    void setFertilizerKey(Key fertilizerKey);

    /**
     * Checks if this crop has an active fertilizer applied.
     *
     * @return true if fertilizer key is not null
     */
    default boolean hasFertilizer() {
        return getFertilizerKey() != null;
    }
}
