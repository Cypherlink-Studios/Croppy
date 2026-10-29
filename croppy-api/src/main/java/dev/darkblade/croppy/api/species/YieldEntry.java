package dev.darkblade.croppy.api.species;

import java.util.Objects;

/**
 * An individual item drop rule with minimum and maximum bounds.
 *
 * @param itemId identifier of the dropped item (e.g. "minecraft:wheat")
 * @param min minimum quantity dropped (must be >= 1)
 * @param max maximum quantity dropped (must be >= min)
 */
public record YieldEntry(String itemId, int min, int max) {
    public YieldEntry {
        Objects.requireNonNull(itemId, "Yield itemId cannot be null");
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("Yield itemId cannot be blank");
        }
        if (min < 1) {
            throw new IllegalArgumentException("Yield min must be at least 1, was " + min);
        }
        if (max < min) {
            throw new IllegalArgumentException("Yield max (" + max + ") cannot be less than min (" + min + ")");
        }
    }
}
