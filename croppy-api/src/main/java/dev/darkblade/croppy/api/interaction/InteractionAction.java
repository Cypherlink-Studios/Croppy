package dev.darkblade.croppy.api.interaction;

import java.util.Map;
import java.util.Objects;

/**
 * An action to execute as part of an interaction pipeline.
 *
 * @param type action type name (e.g. "OPEN_GUI", "SHOW_TEMPERATURE", "INSERT_INPUT", "SERVE_OUTPUT", "ADVANCE_PROGRESS", "PLAY_SOUND", "EMIT_PARTICLES")
 * @param parameters arbitrary action parameters (e.g. ticks, message, sound, particle)
 */
public record InteractionAction(String type, Map<String, Object> parameters) {

    public InteractionAction {
        Objects.requireNonNull(type, "type cannot be null");
        parameters = (parameters == null) ? Map.of() : Map.copyOf(parameters);
    }

    public static InteractionAction of(String type) {
        return new InteractionAction(type, Map.of());
    }

    public static InteractionAction of(String type, Map<String, Object> parameters) {
        return new InteractionAction(type, parameters);
    }
}
