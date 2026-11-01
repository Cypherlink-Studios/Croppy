package dev.darkblade.croppy.api.fuel;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Optional;

/**
 * Port resolving combustion properties for items acting as fuel.
 */
public interface FuelProvider {

    /**
     * Resolves fuel properties for an item by its namespaced key.
     *
     * @param itemKey namespaced identifier of the item
     * @return optional containing fuel properties if the item is recognized as fuel
     */
    Optional<FuelProperties> resolveFuel(Key itemKey);

    /**
     * Checks whether an item key is recognized as a valid fuel.
     *
     * @param itemKey namespaced identifier of the item
     * @return true if valid fuel
     */
    default boolean isFuel(Key itemKey) {
        return resolveFuel(itemKey).isPresent();
    }
}
