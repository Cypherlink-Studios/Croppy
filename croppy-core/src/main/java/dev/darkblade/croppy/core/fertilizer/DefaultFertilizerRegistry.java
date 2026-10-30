package dev.darkblade.croppy.core.fertilizer;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Standard thread-safe implementation of {@link FertilizerRegistry}.
 */
public class DefaultFertilizerRegistry extends SimpleRegistry<Fertilizer> implements FertilizerRegistry {
}
