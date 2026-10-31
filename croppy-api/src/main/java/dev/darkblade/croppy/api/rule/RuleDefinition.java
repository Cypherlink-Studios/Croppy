package dev.darkblade.croppy.api.rule;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Objects;

/**
 * Declarative rule definition evaluated during simulation events.
 *
 * @param key unique rule identifier
 * @param name human-readable name
 * @param trigger event trigger point
 * @param severity rule severity level
 * @param priority execution order priority (higher evaluated first)
 * @param conditions list of expression strings evaluated via Pratt AST
 * @param actions consequence actions executed when conditions are met
 */
public record RuleDefinition(
        Key key,
        String name,
        RuleTrigger trigger,
        RuleSeverity severity,
        int priority,
        List<String> conditions,
        List<RuleAction> actions
) implements Keyed {

    public RuleDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(trigger, "trigger cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        conditions = (conditions == null) ? List.of() : List.copyOf(conditions);
        actions = (actions == null) ? List.of() : List.copyOf(actions);
    }

    @Override
    public Key getKey() {
        return key;
    }
}
