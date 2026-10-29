package dev.darkblade.croppy.core.species;

import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link SpeciesRegistry} with fast secondary indices.
 */
public class DefaultSpeciesRegistry extends SimpleRegistry<Species> implements SpeciesRegistry {

    private final Map<String, Species> bySeedItem = new ConcurrentHashMap<>();
    private final Map<String, Species> byBlock = new ConcurrentHashMap<>();

    @Override
    public void register(Species entry) {
        super.register(entry);
        String seedItem = entry.getDisplay().seedItemId().toLowerCase();
        String block = entry.getDisplay().blockId().toLowerCase();
        bySeedItem.put(seedItem, entry);
        byBlock.put(block, entry);
    }

    @Override
    public Optional<Species> findBySeedItem(String seedItemId) {
        if (seedItemId == null) return Optional.empty();
        return Optional.ofNullable(bySeedItem.get(seedItemId.toLowerCase()));
    }

    @Override
    public Optional<Species> findByBlock(String blockId) {
        if (blockId == null) return Optional.empty();
        return Optional.ofNullable(byBlock.get(blockId.toLowerCase()));
    }

    @Override
    public void clear() {
        super.clear();
        bySeedItem.clear();
        byBlock.clear();
    }
}
