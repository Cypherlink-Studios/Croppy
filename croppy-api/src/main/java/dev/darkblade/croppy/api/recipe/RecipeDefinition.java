package dev.darkblade.croppy.api.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a discoverable culinary recipe.
 *
 * @param key                     unique namespaced recipe identifier
 * @param displayName             MiniMessage formatted display name
 * @param categoryKey             recipe category key
 * @param stationKey              station key required to craft this recipe
 * @param processKey              associated station execution process key
 * @param inputRequirements       required inputs for this recipe
 * @param resultItemKey           resulting item key produced
 * @param resultAmount            quantity produced
 * @param hint                    hint presentation configuration
 * @param allowExcessInputs       whether experimentation allows extra inputs in station slots
 * @param enabledDiscoveryMethods list of discovery method keys permitted to unlock this recipe
 * @param properties              arbitrary configuration properties
 */
public record RecipeDefinition(
        Key key,
        String displayName,
        Key categoryKey,
        Key stationKey,
        Key processKey,
        List<StoredItem> inputRequirements,
        Key resultItemKey,
        int resultAmount,
        HintDefinition hint,
        boolean allowExcessInputs,
        List<Key> enabledDiscoveryMethods,
        Map<String, String> properties
) implements Keyed {

    public RecipeDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(categoryKey, "categoryKey cannot be null");
        Objects.requireNonNull(stationKey, "stationKey cannot be null");
        Objects.requireNonNull(processKey, "processKey cannot be null");
        Objects.requireNonNull(resultItemKey, "resultItemKey cannot be null");
        inputRequirements = (inputRequirements == null) ? List.of() : List.copyOf(inputRequirements);
        hint = (hint == null) ? HintDefinition.DEFAULT : hint;
        enabledDiscoveryMethods = (enabledDiscoveryMethods == null) ? List.of() : List.copyOf(enabledDiscoveryMethods);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
        if (resultAmount <= 0) {
            throw new IllegalArgumentException("resultAmount must be positive, got: " + resultAmount);
        }
    }

    @Override
    public Key getKey() {
        return key;
    }
}
