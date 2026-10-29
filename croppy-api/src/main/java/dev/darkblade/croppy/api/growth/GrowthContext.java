package dev.darkblade.croppy.api.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import java.util.Objects;

/**
 * Contextual data available during crop growth evaluation.
 *
 * @param crop the active crop instance being evaluated
 * @param species the species definition of the crop
 * @param environment the current physical environment
 * @param baseProgress the configured base progress points per evaluation cycle
 */
public record GrowthContext(
        CropInstance crop,
        Species species,
        CropEnvironment environment,
        double baseProgress
) {
    public GrowthContext {
        Objects.requireNonNull(crop, "crop must not be null");
        Objects.requireNonNull(species, "species must not be null");
        Objects.requireNonNull(environment, "environment must not be null");
        if (baseProgress < 0.0) {
            throw new IllegalArgumentException("baseProgress must not be negative");
        }
    }
}
