package dev.darkblade.croppy.api.discovery;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Contextual data provided when evaluating recipe discovery methods.
 *
 * @param playerId   unique player UUID attempting or triggering discovery
 * @param triggerKey optional trigger key (e.g. acquired item key or station key)
 * @param inputs     candidate inputs present (used during experimental discovery)
 * @param parameters additional contextual parameters
 */
public record DiscoveryContext(
        UUID playerId,
        Key triggerKey,
        List<StoredItem> inputs,
        Map<String, Object> parameters
) {

    public DiscoveryContext {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        inputs = (inputs == null) ? List.of() : List.copyOf(inputs);
        parameters = (parameters == null) ? Map.of() : Map.copyOf(parameters);
    }

    public static DiscoveryContext of(UUID playerId, Key triggerKey) {
        return new DiscoveryContext(playerId, triggerKey, List.of(), Map.of());
    }

    public static DiscoveryContext experimental(UUID playerId, Key stationKey, List<StoredItem> inputs) {
        return new DiscoveryContext(playerId, stationKey, inputs, Map.of());
    }

    public Optional<Key> getTriggerKey() {
        return Optional.ofNullable(triggerKey);
    }
}
