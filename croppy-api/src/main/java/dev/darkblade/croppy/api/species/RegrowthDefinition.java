package dev.darkblade.croppy.api.species;

import java.util.Objects;

/**
 * Defines cyclic regrowth behavior for a crop, resetting to a target stage upon harvest.
 *
 * @param targetStageId the stage identifier to which the crop reverts
 */
public record RegrowthDefinition(String targetStageId) {

    public RegrowthDefinition {
        Objects.requireNonNull(targetStageId, "targetStageId cannot be null");
        if (targetStageId.isBlank()) {
            throw new IllegalArgumentException("targetStageId cannot be blank");
        }
    }
}
