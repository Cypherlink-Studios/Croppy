package dev.darkblade.croppy.core.growth;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.growth.GrowthContext;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;

import java.util.Objects;

/**
 * GrowthModifier that inspects the crop's active fertilizer and applies speed multipliers.
 */
public class FertilizerGrowthModifier implements GrowthModifier {

    private final FertilizerRegistry fertilizerRegistry;

    public FertilizerGrowthModifier(FertilizerRegistry fertilizerRegistry) {
        this.fertilizerRegistry = Objects.requireNonNull(fertilizerRegistry, "fertilizerRegistry cannot be null");
    }

    @Override
    public double calculateModifier(GrowthContext context) {
        Key fertilizerKey = context.crop().getFertilizerKey();
        if (fertilizerKey == null) {
            return 1.0;
        }

        return fertilizerRegistry.get(fertilizerKey)
                .filter(f -> f.type() == FertilizerType.SPEED)
                .map(Fertilizer::speedMultiplier)
                .orElse(1.0);
    }
}
