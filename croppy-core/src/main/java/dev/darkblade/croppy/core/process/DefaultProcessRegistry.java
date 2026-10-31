package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Default in-memory implementation of {@link ProcessRegistry}.
 */
public final class DefaultProcessRegistry extends SimpleRegistry<ProcessDefinition> implements ProcessRegistry {

    /**
     * Finds all process recipes configured for the given station definition key.
     *
     * @param stationKey station key
     * @return list of matching process definitions
     */
    public List<ProcessDefinition> findByStation(Key stationKey) {
        Objects.requireNonNull(stationKey, "stationKey cannot be null");
        return values().stream()
                .filter(p -> p.stationKey().equals(stationKey))
                .toList();
    }
}
