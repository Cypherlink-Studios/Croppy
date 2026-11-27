package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Optional;

/**
 * Cooldown specification applied after consuming an item.
 *
 * @param seconds cooldown duration in seconds (must be positive)
 * @param group   optional shared cooldown group key
 */
public record ConsumeCooldown(
        float seconds,
        Key group
) {
    public ConsumeCooldown {
        if (!Float.isFinite(seconds) || seconds <= 0.0f) {
            throw new IllegalArgumentException("cooldown seconds must be finite and positive: " + seconds);
        }
    }

    public Optional<Key> getGroup() {
        return Optional.ofNullable(group);
    }
}
