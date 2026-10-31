package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable value representation of an item stored within a station.
 *
 * @param itemKey    namespaced item identifier
 * @param amount     item stack count
 * @param quality    quality grade (0: Normal, 1: Silver, 2: Gold, 3: Iridium)
 * @param properties arbitrary metadata properties
 */
public record StoredItem(Key itemKey, int amount, int quality, Map<String, Object> properties) {

    public StoredItem {
        Objects.requireNonNull(itemKey, "itemKey cannot be null");
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative: " + amount);
        }
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }

    public static StoredItem of(Key itemKey, int amount) {
        return new StoredItem(itemKey, amount, 0, Map.of());
    }

    public static StoredItem of(Key itemKey, int amount, int quality) {
        return new StoredItem(itemKey, amount, quality, Map.of());
    }

    public StoredItem withAmount(int newAmount) {
        return new StoredItem(itemKey, newAmount, quality, properties);
    }
}
