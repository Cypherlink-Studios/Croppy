package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Default in-memory implementation of {@link StationRegistry}.
 */
public final class DefaultStationRegistry extends SimpleRegistry<StationDefinition> implements StationRegistry {
}
