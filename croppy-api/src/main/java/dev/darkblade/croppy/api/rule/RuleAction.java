package dev.darkblade.croppy.api.rule;

import java.util.Map;
import java.util.Objects;

/**
 * An action or consequence to execute when a rule triggers.
 *
 * @param type action type (e.g. "REPLACE_OUTPUT", "REDUCE_QUALITY", "DAMAGE_PLAYER", "PLAY_SOUND", "EMIT_PARTICLES", "SEND_MESSAGE")
 * @param parameters arbitrary parameters
 */
public record RuleAction(String type, Map<String, Object> parameters) {

    public RuleAction {
        Objects.requireNonNull(type, "type cannot be null");
        parameters = (parameters == null) ? Map.of() : Map.copyOf(parameters);
    }

    public static RuleAction of(String type) {
        return new RuleAction(type, Map.of());
    }

    public static RuleAction of(String type, Map<String, Object> parameters) {
        return new RuleAction(type, parameters);
    }
}
