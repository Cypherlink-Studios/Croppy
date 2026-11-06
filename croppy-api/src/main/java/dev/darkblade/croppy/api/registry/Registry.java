package dev.darkblade.croppy.api.registry;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Generic contract for an in-memory registry of domain objects.
 *
 * @param <T> the type of keyed domain object
 */
public interface Registry<T extends Keyed> {

    /**
     * Registers an entry into the registry.
     *
     * @param entry the entry to register
     * @throws IllegalStateException if an entry with the same key is already registered
     * @throws IllegalArgumentException if entry or its key is null
     */
    void register(T entry);

    /**
     * Retrieves an entry by its key.
     *
     * @param key the unique key
     * @return an Optional containing the entry if present, or empty if absent
     */
    Optional<T> get(Key key);

    /**
     * Checks if a key exists in the registry.
     *
     * @param key the key to check
     * @return true if the key is registered, false otherwise
     */
    boolean contains(Key key);

    /**
     * @return an unmodifiable collection of all registered entries
     */
    Collection<T> values();

    /**
     * @return a stream of all registered entries
     */
    default Stream<T> stream() {
        return values().stream();
    }

    /**
     * @return the number of registered entries
     */
    int size();

    /**
     * @return true if the registry contains no entries
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Clears all registered entries from this registry.
     */
    void clear();

    /**
     * Unregisters an entry by its unique key.
     *
     * @param key the unique key to unregister
     * @return true if an entry was removed, false if no entry was found for the key
     */
    boolean unregister(Key key);

    /**
     * Unregisters all entries matching the given predicate filter.
     *
     * @param filter the predicate defining entries to remove
     * @return the number of removed entries
     */
    default int unregisterAll(java.util.function.Predicate<T> filter) {
        java.util.Objects.requireNonNull(filter, "Filter predicate cannot be null");
        int count = 0;
        for (T entry : values()) {
            if (filter.test(entry)) {
                if (unregister(entry.getKey())) {
                    count++;
                }
            }
        }
        return count;
    }
}
