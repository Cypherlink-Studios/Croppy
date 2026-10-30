package dev.darkblade.croppy.api.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;

/**
 * Listener invoked when a crop instance withers due to exceeding unwatered policy thresholds.
 */
@FunctionalInterface
public interface WitherHandler {

    /**
     * Handles crop withering.
     *
     * @param crop the withered crop instance
     * @param species the species definition
     */
    void onWither(CropInstance crop, Species species);
}
