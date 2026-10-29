package dev.darkblade.croppy.api.growth;

import dev.darkblade.croppy.api.world.BlockPosition;

/**
 * Port interface for querying the ambient physical environment of a spatial position.
 */
public interface EnvironmentProvider {

    /**
     * Resolves the current physical environment at the given block position.
     * If the location is unloaded or inaccessible, returns an unloaded environment.
     *
     * @param position the block position to evaluate
     * @return the resolved CropEnvironment
     */
    CropEnvironment getEnvironment(BlockPosition position);
}
