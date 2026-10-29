package dev.darkblade.croppy.api.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;

/**
 * Port interface for synchronizing the visual world state when a crop instance changes stage.
 */
public interface DisplaySynchronizer {

    /**
     * Synchronizes the physical block appearance in the world to reflect the crop's new stage.
     *
     * @param crop the crop instance
     * @param species the species definition
     * @param stage the target stage definition
     */
    void synchronizeDisplay(CropInstance crop, Species species, StageDefinition stage);
}
