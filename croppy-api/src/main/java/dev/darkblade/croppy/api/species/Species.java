package dev.darkblade.croppy.api.species;

import dev.darkblade.croppy.api.growth.UnwateredPolicy;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Optional;

/**
 * Domain definition of a crop species in Croppy.
 */
public interface Species extends Keyed {

    /**
     * @return human-readable name of the species
     */
    String getName();

    /**
     * @return display configuration mapping to visual representations
     */
    CropDisplay getDisplay();

    /**
     * @return ordered list of developmental growth stages
     */
    List<StageDefinition> getStages();

    /**
     * @return number of stages
     */
    default int getTotalStages() {
        return getStages().size();
    }

    /**
     * @param index stage index (0-based)
     * @return stage definition at given index
     */
    default StageDefinition getStage(int index) {
        return getStages().get(index);
    }

    /**
     * @param index stage index
     * @return true if this stage is the final mature stage
     */
    default boolean isMature(int index) {
        return index >= getTotalStages() - 1;
    }

    /**
     * Resolves a developmental stage by its unique identifier.
     *
     * @param id stage identifier
     * @return Optional containing the matching StageDefinition, or empty if not found
     */
    default Optional<StageDefinition> getStageById(String id) {
        if (id == null) return Optional.empty();
        return getStages().stream()
                .filter(s -> s.id().equalsIgnoreCase(id))
                .findFirst();
    }

    /**
     * Finds the 0-based index of a stage by its unique identifier.
     *
     * @param id stage identifier
     * @return 0-based stage index, or -1 if not found
     */
    default int getStageIndex(String id) {
        if (id == null) return -1;
        List<StageDefinition> stages = getStages();
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).id().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return environmental requirements for growth
     */
    CropRequirements getRequirements();

    /**
     * @return harvest drop definitions
     */
    HarvestDefinition getHarvest();

    /**
     * @return unwatered policy for this species, defaults to PAUSE if not specified
     */
    default UnwateredPolicy getUnwateredPolicy() {
        return UnwateredPolicy.pause();
    }

    /**
     * @return spatial footprint of this crop species, defaults to single block
     */
    default SpatialFootprint getSpatialFootprint() {
        return SpatialFootprint.SINGLE_BLOCK;
    }

    /**
     * @return vertical height in blocks occupied by this crop
     */
    default int getHeight() {
        return getSpatialFootprint().height();
    }
}
