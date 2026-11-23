package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Central registry managing {@link StationActionContributor} extensions.
 */
public interface StationActionRegistry extends Registry<StationActionContributor>, Service {
}
