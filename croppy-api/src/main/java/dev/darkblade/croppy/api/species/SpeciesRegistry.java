package dev.darkblade.croppy.api.species;

import dev.darkblade.croppy.api.registry.Registry;

import java.util.Optional;

/**
 * Specialized registry for crop species with fast lookup indices.
 */
public interface SpeciesRegistry extends Registry<Species> {

    /**
     * Finds a species by its triggering seed item identifier.
     *
     * @param seedItemId the seed item ID (e.g. "minecraft:wheat_seeds")
     * @return the matching species, if present
     */
    Optional<Species> findBySeedItem(String seedItemId);

    /**
     * Finds a species by its planted block identifier.
     *
     * @param blockId the block ID (e.g. "minecraft:wheat")
     * @return the matching species, if present
     */
    Optional<Species> findByBlock(String blockId);
}
