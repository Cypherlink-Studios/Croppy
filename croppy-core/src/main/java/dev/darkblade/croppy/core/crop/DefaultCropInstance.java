package dev.darkblade.croppy.core.crop;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Objects;

/**
 * Mutable default implementation of {@link CropInstance}.
 */
public class DefaultCropInstance implements CropInstance {

    private final Key speciesKey;
    private final BlockPosition position;
    private final long plantedAtMillis;

    private int stageIndex;
    private double progress;
    private long lastUpdatedMillis;
    private long lastWateredDay;
    private Key fertilizerKey;

    public DefaultCropInstance(Key speciesKey, BlockPosition position, int stageIndex, double progress,
                               long plantedAtMillis, long lastUpdatedMillis, long lastWateredDay, Key fertilizerKey) {
        this.speciesKey = Objects.requireNonNull(speciesKey, "SpeciesKey cannot be null");
        this.position = Objects.requireNonNull(position, "BlockPosition cannot be null");
        this.stageIndex = stageIndex;
        this.progress = progress;
        this.plantedAtMillis = plantedAtMillis;
        this.lastUpdatedMillis = lastUpdatedMillis;
        this.lastWateredDay = lastWateredDay;
        this.fertilizerKey = fertilizerKey;
    }

    public DefaultCropInstance(Key speciesKey, BlockPosition position, int stageIndex, double progress,
                               long plantedAtMillis, long lastUpdatedMillis) {
        this(speciesKey, position, stageIndex, progress, plantedAtMillis, lastUpdatedMillis, -1L, null);
    }

    public DefaultCropInstance(Key speciesKey, BlockPosition position) {
        this(speciesKey, position, 0, 0.0, System.currentTimeMillis(), System.currentTimeMillis(), -1L, null);
    }

    @Override
    public Key getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public BlockPosition getPosition() {
        return position;
    }

    @Override
    public int getStageIndex() {
        return stageIndex;
    }

    @Override
    public void setStageIndex(int stageIndex) {
        this.stageIndex = stageIndex;
        updateTimestamp();
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public void setProgress(double progress) {
        this.progress = progress;
        updateTimestamp();
    }

    @Override
    public long getPlantedAtMillis() {
        return plantedAtMillis;
    }

    @Override
    public long getLastUpdatedMillis() {
        return lastUpdatedMillis;
    }

    @Override
    public void updateTimestamp() {
        this.lastUpdatedMillis = System.currentTimeMillis();
    }

    @Override
    public long getLastWateredDay() {
        return lastWateredDay;
    }

    @Override
    public void setLastWateredDay(long lastWateredDay) {
        this.lastWateredDay = lastWateredDay;
        updateTimestamp();
    }

    @Override
    public Key getFertilizerKey() {
        return fertilizerKey;
    }

    @Override
    public void setFertilizerKey(Key fertilizerKey) {
        this.fertilizerKey = fertilizerKey;
        updateTimestamp();
    }
}
