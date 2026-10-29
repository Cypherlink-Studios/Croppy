package dev.darkblade.croppy.api.species;

import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;

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
     * @return environmental requirements for growth
     */
    CropRequirements getRequirements();

    /**
     * @return harvest drop definitions
     */
    HarvestDefinition getHarvest();
}
