package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.process.PhaseEffects;
import dev.darkblade.croppy.api.station.visual.StationInteractionBox;
import dev.darkblade.croppy.api.station.visual.StationModelOverride;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable definition of a discrete station state triggered by conditional expressions or active priority.
 *
 * @param id        unique state identifier (e.g. "lit", "boiling", "idle")
 * @param priority  evaluation priority (higher priority evaluated first)
 * @param condition optional boolean condition expression string evaluated by the AST condition engine
 * @param model     optional 3D model, custom model data, and transformation overrides
 * @param hitbox    optional interaction entity bounding box overrides
 * @param effects   optional ambient particle and periodic audio loop sensory effects
 */
public record StationStateDefinition(
        String id,
        int priority,
        String condition,
        StationModelOverride model,
        StationInteractionBox hitbox,
        PhaseEffects effects
) {

    public StationStateDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        condition = (condition != null && condition.isBlank()) ? null : condition;
    }

    public StationStateDefinition(String id, int priority, String condition, StationModelOverride model) {
        this(id, priority, condition, model, null, null);
    }

    public StationStateDefinition(String id, int priority, String condition, StationModelOverride model, StationInteractionBox hitbox) {
        this(id, priority, condition, model, hitbox, null);
    }

    public Optional<String> getCondition() {
        return Optional.ofNullable(condition);
    }

    public Optional<StationModelOverride> getModel() {
        return Optional.ofNullable(model);
    }

    public Optional<StationInteractionBox> getHitbox() {
        return Optional.ofNullable(hitbox);
    }

    public Optional<PhaseEffects> getEffects() {
        return Optional.ofNullable(effects);
    }
}
