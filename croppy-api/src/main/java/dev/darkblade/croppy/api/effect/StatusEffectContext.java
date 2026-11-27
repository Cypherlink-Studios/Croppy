package dev.darkblade.croppy.api.effect;

import java.util.Optional;
import java.util.UUID;

/**
 * Runtime context passed to {@link CustomStatusEffect} lifecycle callbacks.
 */
public interface StatusEffectContext {

    UUID getPlayerId();

    ActiveStatusEffect getActiveEffect();

    Optional<StatusEffectStage> getCurrentStage();

    Optional<StatusEffectStage> getPreviousStage();

    long getTickCount();

    PlayerEffectService getEffectService();

    default <T> Optional<T> getPlatformHandle(Class<T> handleType) {
        return Optional.empty();
    }
}
