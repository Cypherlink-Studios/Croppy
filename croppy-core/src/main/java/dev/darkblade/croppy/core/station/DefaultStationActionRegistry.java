package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.station.StationActionContributor;
import dev.darkblade.croppy.api.station.StationActionRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Default thread-safe registry for {@link StationActionContributor} implementations.
 */
public class DefaultStationActionRegistry extends SimpleRegistry<StationActionContributor> implements StationActionRegistry {
}
