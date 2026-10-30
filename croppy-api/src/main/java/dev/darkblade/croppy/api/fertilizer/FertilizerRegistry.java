package dev.darkblade.croppy.api.fertilizer;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Service registry storing and managing all active fertilizer definitions.
 */
public interface FertilizerRegistry extends Registry<Fertilizer>, Service {
}
