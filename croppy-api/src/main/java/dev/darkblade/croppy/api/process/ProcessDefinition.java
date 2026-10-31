package dev.darkblade.croppy.api.process;

import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a recipe or multi-phase processing specification.
 *
 * @param key                 unique namespaced recipe identifier
 * @param displayName         MiniMessage formatted display name
 * @param stationKey          required station definition key where this recipe can run
 * @param inputRequirements   input items required to initiate the process
 * @param startConditions     expression condition that must evaluate to true to initiate
 * @param phases              sequential list of phases
 * @param outputResolver      output resolver calculating results, or null for default
 * @param defaultResultItem   fallback item key produced upon completion
 * @param defaultResultAmount fallback item quantity produced
 * @param properties          arbitrary configuration properties
 */
public record ProcessDefinition(
        Key key,
        String displayName,
        Key stationKey,
        List<StoredItem> inputRequirements,
        Expression startConditions,
        List<PhaseDefinition> phases,
        OutputResolver outputResolver,
        Key defaultResultItem,
        int defaultResultAmount,
        Map<String, String> properties
) implements Keyed {

    public ProcessDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(stationKey, "stationKey cannot be null");
        inputRequirements = (inputRequirements == null) ? List.of() : List.copyOf(inputRequirements);
        startConditions = (startConditions == null) ? Expression.ALWAYS_TRUE : startConditions;
        phases = (phases == null || phases.isEmpty())
                ? List.of(PhaseDefinition.timed("default", 200, Expression.ALWAYS_TRUE))
                : List.copyOf(phases);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }

    @Override
    public Key getKey() {
        return key;
    }
}
