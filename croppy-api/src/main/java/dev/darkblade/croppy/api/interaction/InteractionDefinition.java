package dev.darkblade.croppy.api.interaction;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Declarative definition for player interaction routing with stations or crops.
 *
 * @param key unique interaction key
 * @param targetStation key of target station, or empty
 * @param targetCrop key of target crop species, or empty
 * @param trigger interaction trigger mode
 * @param matchItem optional specific custom item key required in player hand
 * @param matchMaterial optional vanilla material identifier in hand (e.g. "minecraft:bowl")
 * @param requireEmptyHand whether player must have an empty hand
 * @param conditions list of expression strings evaluated via Pratt AST
 * @param actions sequence of actions to execute
 * @param priority higher priority matches first
 */
public record InteractionDefinition(
        Key key,
        Optional<Key> targetStation,
        Optional<Key> targetCrop,
        InteractionTrigger trigger,
        Optional<Key> matchItem,
        Optional<String> matchMaterial,
        boolean requireEmptyHand,
        List<String> conditions,
        List<InteractionAction> actions,
        int priority
) implements Keyed {

    public InteractionDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(targetStation, "targetStation cannot be null");
        Objects.requireNonNull(targetCrop, "targetCrop cannot be null");
        Objects.requireNonNull(trigger, "trigger cannot be null");
        Objects.requireNonNull(matchItem, "matchItem cannot be null");
        Objects.requireNonNull(matchMaterial, "matchMaterial cannot be null");
        conditions = (conditions == null) ? List.of() : List.copyOf(conditions);
        actions = (actions == null) ? List.of() : List.copyOf(actions);
    }

    /**
     * Backward-compatible constructor for station interactions without target crop.
     */
    public InteractionDefinition(
            Key key,
            Optional<Key> targetStation,
            InteractionTrigger trigger,
            Optional<Key> matchItem,
            Optional<String> matchMaterial,
            boolean requireEmptyHand,
            List<String> conditions,
            List<InteractionAction> actions,
            int priority
    ) {
        this(key, targetStation, Optional.empty(), trigger, matchItem, matchMaterial, requireEmptyHand, conditions, actions, priority);
    }

    @Override
    public Key getKey() {
        return key;
    }
}
