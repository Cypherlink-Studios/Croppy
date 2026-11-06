package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of {@link ItemRegistry} with category indexing.
 */
public class DefaultItemRegistry extends SimpleRegistry<ItemDefinition> implements ItemRegistry {

    private final Map<String, Set<Key>> categoryIndex = new ConcurrentHashMap<>();

    @Override
    public void register(ItemDefinition entry) {
        super.register(entry);
        String category = entry.category().toLowerCase(Locale.ROOT);
        categoryIndex.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet())
                .add(entry.getKey());
    }

    @Override
    public Collection<ItemDefinition> getByCategory(String category) {
        if (category == null) {
            return List.of();
        }
        Set<Key> keys = categoryIndex.get(category.toLowerCase(Locale.ROOT));
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(this::get)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categoryIndex.keySet());
    }

    @Override
    public boolean unregister(Key key) {
        Optional<ItemDefinition> existing = get(key);
        if (existing.isPresent()) {
            ItemDefinition item = existing.get();
            Set<Key> set = categoryIndex.get(item.category().toLowerCase(Locale.ROOT));
            if (set != null) {
                set.remove(key);
            }
            return super.unregister(key);
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        categoryIndex.clear();
    }
}
