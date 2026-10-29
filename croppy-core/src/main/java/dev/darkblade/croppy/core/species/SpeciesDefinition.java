package dev.darkblade.croppy.core.species;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable implementation of {@link Species}.
 */
public record SpeciesDefinition(
        Key key,
        String name,
        CropDisplay display,
        List<StageDefinition> stages,
        CropRequirements requirements,
        HarvestDefinition harvest
) implements Species {

    public SpeciesDefinition {
        Objects.requireNonNull(key, "Species key cannot be null");
        Objects.requireNonNull(name, "Species name cannot be null");
        Objects.requireNonNull(display, "Species display cannot be null");
        Objects.requireNonNull(stages, "Species stages cannot be null");
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Species must have at least one stage");
        }
        Objects.requireNonNull(requirements, "Species requirements cannot be null");
        Objects.requireNonNull(harvest, "Species harvest cannot be null");
        stages = Collections.unmodifiableList(stages);
    }

    @Override
    public Key getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CropDisplay getDisplay() {
        return display;
    }

    @Override
    public List<StageDefinition> getStages() {
        return stages;
    }

    @Override
    public CropRequirements getRequirements() {
        return requirements;
    }

    @Override
    public HarvestDefinition getHarvest() {
        return harvest;
    }
}
