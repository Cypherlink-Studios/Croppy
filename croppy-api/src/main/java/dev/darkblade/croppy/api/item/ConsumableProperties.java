package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.List;
import java.util.Optional;

/**
 * Immutable consumption attributes and server-side effect specifications attached to an {@link ItemDefinition}.
 *
 * @param food                optional food nutrition and quality scaling properties
 * @param consumeSeconds      duration in seconds required to consume the item (must be positive)
 * @param animation           client use animation (e.g. {@link UseAnimation#EAT} or {@link UseAnimation#DRINK})
 * @param sound               optional sound key played during consumption
 * @param hasConsumeParticles whether consumption particles are emitted on the client
 * @param remainderItemKey    optional item key returned after consumption (e.g. {@code minecraft:bowl})
 * @param cooldown            optional consumption cooldown specification
 * @param effects             ordered list of effect specifications executed server-side on completion
 */
public record ConsumableProperties(
        FoodProperties food,
        float consumeSeconds,
        UseAnimation animation,
        Key sound,
        boolean hasConsumeParticles,
        Key remainderItemKey,
        ConsumeCooldown cooldown,
        List<ConsumeEffectSpec> effects
) {
    public static final float DEFAULT_CONSUME_SECONDS = 1.6f;

    public ConsumableProperties {
        if (!Float.isFinite(consumeSeconds) || consumeSeconds <= 0.0f) {
            throw new IllegalArgumentException("consumeSeconds must be finite and positive: " + consumeSeconds);
        }
        animation = (animation == null) ? UseAnimation.EAT : animation;
        effects = (effects == null) ? List.of() : List.copyOf(effects);
    }

    public Optional<FoodProperties> getFood() {
        return Optional.ofNullable(food);
    }

    public Optional<Key> getSound() {
        return Optional.ofNullable(sound);
    }

    public Optional<Key> getRemainderItemKey() {
        return Optional.ofNullable(remainderItemKey);
    }

    public Optional<ConsumeCooldown> getCooldown() {
        return Optional.ofNullable(cooldown);
    }
}
