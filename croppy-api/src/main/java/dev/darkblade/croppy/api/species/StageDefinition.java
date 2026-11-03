package dev.darkblade.croppy.api.species;

import java.util.Objects;
import java.util.Optional;

/**
 * Definition of an abstract developmental growth stage for a crop.
 *
 * @param id               unique stage identifier within the species (e.g. "seed", "sprout", "mature")
 * @param progressRequired progress points needed to advance to the next stage (0 for final mature stage)
 * @param blockAge         visual age of the block corresponding to this stage (e.g. 0 to 7)
 * @param model3D          optional 3D model definition for this stage
 */
public record StageDefinition(String id, double progressRequired, int blockAge, Model3DDefinition model3D) {
    public StageDefinition {
        Objects.requireNonNull(id, "Stage id cannot be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Stage id cannot be blank");
        }
        if (progressRequired < 0) {
            throw new IllegalArgumentException("Stage progressRequired cannot be negative");
        }
        if (blockAge < 0) {
            throw new IllegalArgumentException("Stage blockAge cannot be negative");
        }
    }

    /**
     * Backward-compatible constructor for vanilla block stages without 3D model.
     */
    public StageDefinition(String id, double progressRequired, int blockAge) {
        this(id, progressRequired, blockAge, null);
    }

    /**
     * @return optional containing the 3D model definition if present
     */
    public Optional<Model3DDefinition> getModel3D() {
        return Optional.ofNullable(model3D);
    }
}
