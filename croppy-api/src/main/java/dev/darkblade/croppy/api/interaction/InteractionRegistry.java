package dev.darkblade.croppy.api.interaction;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

import java.util.List;

/**
 * Registry containing all registered {@link InteractionDefinition} configurations.
 */
public interface InteractionRegistry extends Registry<InteractionDefinition>, Service {

    /**
     * Retrieves all interaction definitions applicable to a specific station type or global,
     * sorted by priority in descending order.
     *
     * @param stationKey station key, or null for global interactions
     * @return priority-sorted interaction definitions
     */
    List<InteractionDefinition> getInteractionsForStation(Key stationKey);

    /**
     * Retrieves all interaction definitions applicable to a specific crop species or global,
     * sorted by priority in descending order.
     *
     * @param cropKey crop species key, or null for global interactions
     * @return priority-sorted interaction definitions
     */
    default List<InteractionDefinition> getInteractionsForCrop(Key cropKey) {
        return List.of();
    }
}
