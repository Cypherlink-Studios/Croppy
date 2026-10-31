package dev.darkblade.croppy.api.process;

import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a single phase within a sequential process recipe.
 *
 * @param id             unique identifier of the phase within its parent process
 * @param type           advancement behavior type
 * @param durationTicks  required duration in ticks for TIMED phases
 * @param timeoutTicks   maximum allowable ticks before triggering failure policy (or 0 if infinite)
 * @param condition      condition expression that must be satisfied
 * @param requiredItem   optional required item key for MANUAL_INTERACTION phases
 * @param requiredAmount quantity of required item to consume
 * @param failurePolicy  policy executed on timeout or condition failure
 * @param byproductItem  optional item produced when failure policy is SPOIL or BYPRODUCT
 * @param promptMessage  MiniMessage text shown to players when awaiting input
 * @param properties     arbitrary configuration properties (e.g. particles, sounds)
 */
public record PhaseDefinition(
        String id,
        PhaseType type,
        long durationTicks,
        long timeoutTicks,
        Expression condition,
        Key requiredItem,
        int requiredAmount,
        FailurePolicy failurePolicy,
        Key byproductItem,
        String promptMessage,
        Map<String, String> properties
) {

    public PhaseDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        condition = (condition == null) ? Expression.ALWAYS_TRUE : condition;
        failurePolicy = (failurePolicy == null) ? FailurePolicy.CANCEL : failurePolicy;
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }

    public static PhaseDefinition timed(String id, long durationTicks, Expression condition) {
        return new PhaseDefinition(id, PhaseType.TIMED, durationTicks, 0, condition, null, 0, FailurePolicy.CANCEL, null, null, Map.of());
    }

    public static PhaseDefinition interactive(String id, long timeoutTicks, Key requiredItem, int requiredAmount, String promptMessage) {
        return new PhaseDefinition(id, PhaseType.MANUAL_INTERACTION, 0, timeoutTicks, Expression.ALWAYS_TRUE, requiredItem, requiredAmount, FailurePolicy.SPOIL, null, promptMessage, Map.of());
    }

    public static PhaseDefinition environmental(String id, Expression condition) {
        return new PhaseDefinition(id, PhaseType.ENVIRONMENTAL, 0, 0, condition, null, 0, FailurePolicy.CANCEL, null, null, Map.of());
    }
}
