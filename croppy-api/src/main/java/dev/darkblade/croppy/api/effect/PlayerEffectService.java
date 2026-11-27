package dev.darkblade.croppy.api.effect;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.registry.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing active {@link CustomStatusEffect} instances per player.
 */
public interface PlayerEffectService extends Service {

    ActiveStatusEffect apply(
            UUID playerId,
            Key effectKey,
            long durationTicks,
            int amplifier,
            double intensity,
            Map<String, Object> data
    );

    default ActiveStatusEffect apply(UUID playerId, Key effectKey, long durationTicks, int amplifier) {
        return apply(playerId, effectKey, durationTicks, amplifier, 0.0, Map.of());
    }

    Optional<ActiveStatusEffect> reduce(UUID playerId, Key effectKey, double intensityDelta, long ticksDelta);

    boolean remove(UUID playerId, Key effectKey, RemovalReason reason);

    int clearPlayer(UUID playerId, RemovalReason reason);

    Optional<ActiveStatusEffect> getActiveEffect(UUID playerId, Key effectKey);

    Collection<ActiveStatusEffect> getActiveEffects(UUID playerId);

    boolean hasEffect(UUID playerId, Key effectKey);

    Optional<StatusEffectStage> getCurrentStage(UUID playerId, Key effectKey);

    void handleMilkConsume(UUID playerId);

    void handlePlayerDeath(UUID playerId);

    void handlePlayerQuit(UUID playerId, boolean savePersistent);

    void handlePlayerJoin(UUID playerId);

    void tick(long deltaTicks);

    int purgeEffectDefinition(Key effectKey);

    void clearAll();
}
