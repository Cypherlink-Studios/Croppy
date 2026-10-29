package dev.darkblade.croppy.core.registry;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.api.registry.Registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory registry implementation rejecting duplicate entries.
 *
 * @param <T> the type of keyed domain object
 */
public class SimpleRegistry<T extends Keyed> implements Registry<T> {

    private final Map<Key, T> entries = new ConcurrentHashMap<>();

    @Override
    public void register(T entry) {
        Objects.requireNonNull(entry, "Registry entry cannot be null");
        Key key = Objects.requireNonNull(entry.getKey(), "Registry entry key cannot be null");

        T existing = entries.putIfAbsent(key, entry);
        if (existing != null) {
            throw new IllegalStateException("Duplicate entry registration for key: " + key);
        }
    }

    @Override
    public Optional<T> get(Key key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public boolean contains(Key key) {
        if (key == null) {
            return false;
        }
        return entries.containsKey(key);
    }

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    @Override
    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}
