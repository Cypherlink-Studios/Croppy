package dev.darkblade.croppy.api.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;

/**
 * Interceptor hook called prior to advancing a crop instance's developmental stage.
 * Can be used by platform layers (e.g. Paper events) to approve or veto the transition.
 */
@FunctionalInterface
public interface StageAdvanceInterceptor {

    /**
     * Determines whether the crop is permitted to advance to the next developmental stage.
     *
     * @param crop the crop instance
     * @param species the species definition
     * @param fromStage the current stage index
     * @param toStage the destination stage index
     * @return true to permit advancement, false to veto/cancel it
     */
    boolean shouldAdvance(CropInstance crop, Species species, int fromStage, int toStage);
}
