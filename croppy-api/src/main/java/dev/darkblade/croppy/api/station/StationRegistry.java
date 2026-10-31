package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Registry containing all registered {@link StationDefinition} templates.
 */
public interface StationRegistry extends Registry<StationDefinition>, Service {
}
