package dev.darkblade.croppy.core.effect;

import dev.darkblade.croppy.api.effect.ActiveStatusEffect;
import dev.darkblade.croppy.api.effect.CustomStatusEffect;
import dev.darkblade.croppy.api.effect.PlayerEffectService;
import dev.darkblade.croppy.api.effect.PlayerEffectStorage;
import dev.darkblade.croppy.api.effect.RemovalReason;
import dev.darkblade.croppy.api.effect.StackingPolicy;
import dev.darkblade.croppy.api.effect.StatusEffectContext;
import dev.darkblade.croppy.api.effect.StatusEffectRegistry;
import dev.darkblade.croppy.api.effect.StatusEffectStage;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Thread-safe core implementation of {@link PlayerEffectService} managing active
 * custom status effects, stacking policies, continuous decay, and opt-in persistence.
 */
public class DefaultPlayerEffectService implements PlayerEffectService {

    private final StatusEffectRegistry statusRegistry;
    private final CroppyLogger logger;
    private final Clock clock;
    private final Map<UUID, ConcurrentHashMap<Key, ActiveStatusEffect>> activeByPlayer = new ConcurrentHashMap<>();
    private final AtomicLong totalTicks = new AtomicLong(0L);

    private volatile PlayerEffectStorage storage;
    private volatile Function<UUID, Object> platformHandleResolver = id -> null;

    public DefaultPlayerEffectService(StatusEffectRegistry statusRegistry, CroppyLogger logger) {
        this(statusRegistry, logger, Clock.systemUTC());
    }

    public DefaultPlayerEffectService(StatusEffectRegistry statusRegistry, CroppyLogger logger, Clock clock) {
        this.statusRegistry = Objects.requireNonNull(statusRegistry, "statusRegistry cannot be null");
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public void setStorage(PlayerEffectStorage storage) {
        this.storage = storage;
    }

    public Optional<PlayerEffectStorage> getStorage() {
        return Optional.ofNullable(storage);
    }

    public void setPlatformHandleResolver(Function<UUID, Object> resolver) {
        this.platformHandleResolver = resolver != null ? resolver : id -> null;
    }

    @Override
    public ActiveStatusEffect apply(
            UUID playerId,
            Key effectKey,
            long durationTicks,
            int amplifier,
            double intensity,
            Map<String, Object> data
    ) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(effectKey, "effectKey cannot be null");

        CustomStatusEffect def = statusRegistry.get(effectKey)
                .orElseThrow(() -> new IllegalArgumentException("Unregistered status effect: " + effectKey));

        int clampedAmp = Math.max(0, Math.min(def.getMaxAmplifier(), amplifier));
        double clampedIntensity = Math.max(0.0, Math.min(def.getMaxIntensity(), intensity));
        long clampedDuration = Math.max(0L, Math.min(def.getMaxDurationTicks(), durationTicks));
        long now = clock.millis();

        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap =
                activeByPlayer.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());

        ActiveStatusEffect existing = playerMap.get(effectKey);
        StatusEffectStage prevStage = existing != null
                ? def.resolveStage(existing.intensity()).orElse(null)
                : null;

        ActiveStatusEffect next;
        if (existing == null) {
            long effectiveTicks = def.isIntensityDriven()
                    ? computeDecayTicks(clampedIntensity, def.getDecayPerSecond(), clampedDuration)
                    : clampedDuration;
            next = new ActiveStatusEffect(
                    effectKey, clampedAmp, clampedIntensity,
                    effectiveTicks, Math.max(effectiveTicks, clampedDuration),
                    now, now, data
            );
        } else {
            next = combine(def, existing, clampedDuration, clampedAmp, clampedIntensity, data, now);
        }

        playerMap.put(effectKey, next);

        StatusEffectStage newStage = def.resolveStage(next.intensity()).orElse(null);
        StatusEffectContext ctx = createContext(playerId, next, newStage, prevStage);

        safeInvoke(() -> def.onApply(ctx), "onApply", effectKey, playerId);
        if (!Objects.equals(prevStage, newStage)) {
            safeInvoke(() -> def.onStageChange(ctx, prevStage, newStage), "onStageChange", effectKey, playerId);
        }
        return next;
    }

    @Override
    public Optional<ActiveStatusEffect> reduce(UUID playerId, Key effectKey, double intensityDelta, long ticksDelta) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(effectKey, "effectKey cannot be null");

        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        if (playerMap == null) {
            return Optional.empty();
        }
        ActiveStatusEffect existing = playerMap.get(effectKey);
        if (existing == null) {
            return Optional.empty();
        }
        Optional<CustomStatusEffect> defOpt = statusRegistry.get(effectKey);
        if (defOpt.isEmpty()) {
            playerMap.remove(effectKey);
            return Optional.empty();
        }
        CustomStatusEffect def = defOpt.get();
        StatusEffectStage prevStage = def.resolveStage(existing.intensity()).orElse(null);

        double newIntensity = Math.max(0.0, existing.intensity() - Math.max(0.0, intensityDelta));
        long newTicks = def.isIntensityDriven()
                ? computeDecayTicks(newIntensity, def.getDecayPerSecond(), 0L)
                : Math.max(0L, existing.remainingTicks() - Math.max(0L, ticksDelta));

        ActiveStatusEffect updated = new ActiveStatusEffect(
                effectKey, existing.amplifier(), newIntensity,
                newTicks, existing.maxTicks(),
                existing.appliedAtEpochMillis(), clock.millis(), existing.data()
        );

        if (updated.isExpired(def.isIntensityDriven())) {
            playerMap.remove(effectKey);
            if (playerMap.isEmpty()) {
                activeByPlayer.remove(playerId, playerMap);
            }
            StatusEffectContext ctx = createContext(playerId, updated, null, prevStage);
            if (prevStage != null) {
                safeInvoke(() -> def.onStageChange(ctx, prevStage, null), "onStageChange", effectKey, playerId);
            }
            safeInvoke(() -> def.onRemove(ctx, RemovalReason.REMOVED), "onRemove", effectKey, playerId);
            return Optional.empty();
        }

        playerMap.put(effectKey, updated);
        StatusEffectStage newStage = def.resolveStage(updated.intensity()).orElse(null);
        if (!Objects.equals(prevStage, newStage)) {
            StatusEffectContext ctx = createContext(playerId, updated, newStage, prevStage);
            safeInvoke(() -> def.onStageChange(ctx, prevStage, newStage), "onStageChange", effectKey, playerId);
        }
        return Optional.of(updated);
    }

    @Override
    public boolean remove(UUID playerId, Key effectKey, RemovalReason reason) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(effectKey, "effectKey cannot be null");
        RemovalReason effectiveReason = reason != null ? reason : RemovalReason.REMOVED;

        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        if (playerMap == null) {
            return false;
        }
        ActiveStatusEffect removed = playerMap.remove(effectKey);
        if (removed == null) {
            return false;
        }
        if (playerMap.isEmpty()) {
            activeByPlayer.remove(playerId, playerMap);
        }

        statusRegistry.get(effectKey).ifPresent(def -> {
            StatusEffectStage prevStage = def.resolveStage(removed.intensity()).orElse(null);
            StatusEffectContext ctx = createContext(playerId, removed, null, prevStage);
            if (effectiveReason == RemovalReason.EXPIRED) {
                safeInvoke(() -> def.onExpire(ctx), "onExpire", effectKey, playerId);
            } else {
                safeInvoke(() -> def.onRemove(ctx, effectiveReason), "onRemove", effectKey, playerId);
            }
        });
        return true;
    }

    @Override
    public int clearPlayer(UUID playerId, RemovalReason reason) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        if (playerMap == null || playerMap.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Key key : new ArrayList<>(playerMap.keySet())) {
            if (remove(playerId, key, reason)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Optional<ActiveStatusEffect> getActiveEffect(UUID playerId, Key effectKey) {
        if (playerId == null || effectKey == null) {
            return Optional.empty();
        }
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        return playerMap != null ? Optional.ofNullable(playerMap.get(effectKey)) : Optional.empty();
    }

    @Override
    public Collection<ActiveStatusEffect> getActiveEffects(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        if (playerMap == null || playerMap.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableCollection(new ArrayList<>(playerMap.values()));
    }

    @Override
    public boolean hasEffect(UUID playerId, Key effectKey) {
        return getActiveEffect(playerId, effectKey).isPresent();
    }

    @Override
    public Optional<StatusEffectStage> getCurrentStage(UUID playerId, Key effectKey) {
        Optional<ActiveStatusEffect> activeOpt = getActiveEffect(playerId, effectKey);
        if (activeOpt.isEmpty()) {
            return Optional.empty();
        }
        return statusRegistry.get(effectKey)
                .flatMap(def -> def.resolveStage(activeOpt.get().intensity()));
    }

    @Override
    public void handleMilkConsume(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        if (playerMap == null || playerMap.isEmpty()) {
            return;
        }
        for (Key key : new ArrayList<>(playerMap.keySet())) {
            Optional<CustomStatusEffect> defOpt = statusRegistry.get(key);
            if (defOpt.isEmpty()) {
                playerMap.remove(key);
                continue;
            }
            CustomStatusEffect def = defOpt.get();
            if (!def.isMilkCurable()) {
                continue;
            }
            double reduction = Math.max(0.0, Math.min(1.0, def.getMilkReduction()));
            if (reduction >= 1.0) {
                remove(playerId, key, RemovalReason.MILK);
            } else if (reduction > 0.0) {
                ActiveStatusEffect active = playerMap.get(key);
                if (active != null) {
                    if (def.isIntensityDriven()) {
                        reduce(playerId, key, active.intensity() * reduction, 0L);
                    } else {
                        reduce(playerId, key, 0.0, Math.round(active.remainingTicks() * reduction));
                    }
                }
            }
        }
    }

    @Override
    public void handlePlayerDeath(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.get(playerId);
        if (playerMap == null || playerMap.isEmpty()) {
            return;
        }
        for (Key key : new ArrayList<>(playerMap.keySet())) {
            Optional<CustomStatusEffect> defOpt = statusRegistry.get(key);
            if (defOpt.isEmpty() || defOpt.get().isClearedOnDeath()) {
                remove(playerId, key, RemovalReason.DEATH);
            }
        }
    }

    @Override
    public void handlePlayerQuit(UUID playerId, boolean savePersistent) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = activeByPlayer.remove(playerId);
        if (playerMap == null || playerMap.isEmpty()) {
            if (savePersistent && storage != null) {
                storage.clear(playerId);
            }
            return;
        }

        List<ActiveStatusEffect> toPersist = new ArrayList<>();
        long now = clock.millis();
        for (ActiveStatusEffect active : playerMap.values()) {
            Optional<CustomStatusEffect> defOpt = statusRegistry.get(active.effectKey());
            if (defOpt.isPresent() && defOpt.get().isPersistent()) {
                toPersist.add(new ActiveStatusEffect(
                        active.effectKey(), active.amplifier(), active.intensity(),
                        active.remainingTicks(), active.maxTicks(),
                        active.appliedAtEpochMillis(), now, active.data()
                ));
            } else if (defOpt.isPresent()) {
                CustomStatusEffect def = defOpt.get();
                StatusEffectStage prevStage = def.resolveStage(active.intensity()).orElse(null);
                StatusEffectContext ctx = createContext(playerId, active, null, prevStage);
                safeInvoke(() -> def.onRemove(ctx, RemovalReason.DISCONNECTED), "onRemove", active.effectKey(), playerId);
            }
        }

        if (savePersistent && storage != null) {
            if (toPersist.isEmpty()) {
                storage.clear(playerId);
            } else {
                storage.save(playerId, toPersist);
            }
        }
    }

    @Override
    public void handlePlayerJoin(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        if (storage == null) {
            return;
        }
        List<ActiveStatusEffect> loaded = storage.load(playerId);
        if (loaded == null || loaded.isEmpty()) {
            return;
        }

        long now = clock.millis();
        ConcurrentHashMap<Key, ActiveStatusEffect> playerMap =
                activeByPlayer.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());

        for (ActiveStatusEffect saved : loaded) {
            Optional<CustomStatusEffect> defOpt = statusRegistry.get(saved.effectKey());
            if (defOpt.isEmpty()) {
                continue;
            }
            CustomStatusEffect def = defOpt.get();
            double elapsedSeconds = Math.max(0.0, (now - saved.lastTickEpochMillis()) / 1000.0);
            long elapsedTicks = Math.round(elapsedSeconds * 20.0);

            ActiveStatusEffect restored;
            if (def.isIntensityDriven()) {
                double remainingIntensity = Math.max(0.0, saved.intensity() - (elapsedSeconds * def.getDecayPerSecond()));
                long remainingTicks = computeDecayTicks(remainingIntensity, def.getDecayPerSecond(), 0L);
                restored = new ActiveStatusEffect(
                        saved.effectKey(), saved.amplifier(), remainingIntensity,
                        remainingTicks, saved.maxTicks(),
                        saved.appliedAtEpochMillis(), now, saved.data()
                );
            } else {
                long remainingTicks = Math.max(0L, saved.remainingTicks() - elapsedTicks);
                restored = new ActiveStatusEffect(
                        saved.effectKey(), saved.amplifier(), saved.intensity(),
                        remainingTicks, saved.maxTicks(),
                        saved.appliedAtEpochMillis(), now, saved.data()
                );
            }

            if (!restored.isExpired(def.isIntensityDriven())) {
                playerMap.put(saved.effectKey(), restored);
                StatusEffectStage stage = def.resolveStage(restored.intensity()).orElse(null);
                StatusEffectContext ctx = createContext(playerId, restored, stage, null);
                safeInvoke(() -> def.onApply(ctx), "onApply", saved.effectKey(), playerId);
            }
        }

        if (playerMap.isEmpty()) {
            activeByPlayer.remove(playerId, playerMap);
        }
    }

    @Override
    public void tick(long deltaTicks) {
        if (deltaTicks <= 0L) {
            return;
        }
        totalTicks.addAndGet(deltaTicks);
        long now = clock.millis();
        double deltaSeconds = deltaTicks / 20.0;

        for (Map.Entry<UUID, ConcurrentHashMap<Key, ActiveStatusEffect>> entry : activeByPlayer.entrySet()) {
            UUID playerId = entry.getKey();
            ConcurrentHashMap<Key, ActiveStatusEffect> playerMap = entry.getValue();
            if (playerMap == null || playerMap.isEmpty()) {
                activeByPlayer.remove(playerId, playerMap);
                continue;
            }

            for (Key key : new ArrayList<>(playerMap.keySet())) {
                ActiveStatusEffect current = playerMap.get(key);
                if (current == null) {
                    continue;
                }
                Optional<CustomStatusEffect> defOpt = statusRegistry.get(key);
                if (defOpt.isEmpty()) {
                    playerMap.remove(key);
                    continue;
                }
                CustomStatusEffect def = defOpt.get();
                StatusEffectStage prevStage = def.resolveStage(current.intensity()).orElse(null);

                ActiveStatusEffect updated;
                if (def.isIntensityDriven()) {
                    double nextIntensity = Math.max(0.0, current.intensity() - (def.getDecayPerSecond() * deltaSeconds));
                    long nextTicks = computeDecayTicks(nextIntensity, def.getDecayPerSecond(), 0L);
                    updated = new ActiveStatusEffect(
                            key, current.amplifier(), nextIntensity,
                            nextTicks, current.maxTicks(),
                            current.appliedAtEpochMillis(), now, current.data()
                    );
                } else {
                    long nextTicks = Math.max(0L, current.remainingTicks() - deltaTicks);
                    updated = new ActiveStatusEffect(
                            key, current.amplifier(), current.intensity(),
                            nextTicks, current.maxTicks(),
                            current.appliedAtEpochMillis(), now, current.data()
                    );
                }

                if (updated.isExpired(def.isIntensityDriven())) {
                    playerMap.remove(key);
                    StatusEffectContext ctx = createContext(playerId, updated, null, prevStage);
                    if (prevStage != null) {
                        safeInvoke(() -> def.onStageChange(ctx, prevStage, null), "onStageChange", key, playerId);
                    }
                    safeInvoke(() -> def.onExpire(ctx), "onExpire", key, playerId);
                } else {
                    playerMap.put(key, updated);
                    StatusEffectStage newStage = def.resolveStage(updated.intensity()).orElse(null);
                    StatusEffectContext ctx = createContext(playerId, updated, newStage, prevStage);
                    if (!Objects.equals(prevStage, newStage)) {
                        safeInvoke(() -> def.onStageChange(ctx, prevStage, newStage), "onStageChange", key, playerId);
                    }
                    safeInvoke(() -> def.onTick(ctx), "onTick", key, playerId);
                }
            }

            if (playerMap.isEmpty()) {
                activeByPlayer.remove(playerId, playerMap);
            }
        }
    }

    @Override
    public int purgeEffectDefinition(Key effectKey) {
        if (effectKey == null) {
            return 0;
        }
        int removedCount = 0;
        for (UUID playerId : new ArrayList<>(activeByPlayer.keySet())) {
            if (remove(playerId, effectKey, RemovalReason.ADDON_UNLOADED)) {
                removedCount++;
            }
        }
        return removedCount;
    }

    @Override
    public void clearAll() {
        activeByPlayer.clear();
    }

    private ActiveStatusEffect combine(
            CustomStatusEffect def,
            ActiveStatusEffect existing,
            long newDuration,
            int newAmplifier,
            double newIntensity,
            Map<String, Object> newData,
            long now
    ) {
        Map<String, Object> mergedData = new LinkedHashMap<>(existing.data());
        if (newData != null) {
            mergedData.putAll(newData);
        }

        StackingPolicy policy = def.getStackingPolicy();
        int amp = newAmplifier;
        double intensity = newIntensity;
        long remaining = newDuration;

        switch (policy) {
            case REPLACE -> {
                if (def.isIntensityDriven()) {
                    remaining = computeDecayTicks(intensity, def.getDecayPerSecond(), newDuration);
                }
            }
            case EXTEND -> {
                amp = Math.max(existing.amplifier(), newAmplifier);
                intensity = Math.min(def.getMaxIntensity(), Math.max(existing.intensity(), newIntensity));
                remaining = Math.min(def.getMaxDurationTicks(), existing.remainingTicks() + newDuration);
            }
            case UPGRADE -> {
                if (newAmplifier > existing.amplifier()) {
                    amp = Math.min(def.getMaxAmplifier(), newAmplifier);
                } else {
                    amp = Math.min(def.getMaxAmplifier(), existing.amplifier() + 1);
                }
                intensity = Math.min(def.getMaxIntensity(), Math.max(existing.intensity(), newIntensity));
                remaining = Math.max(existing.remainingTicks(), newDuration);
            }
            case STACK_INTENSITY -> {
                amp = Math.max(existing.amplifier(), newAmplifier);
                intensity = Math.min(def.getMaxIntensity(), existing.intensity() + newIntensity);
                if (def.isIntensityDriven()) {
                    remaining = computeDecayTicks(intensity, def.getDecayPerSecond(), newDuration);
                } else {
                    remaining = Math.max(existing.remainingTicks(), newDuration);
                }
            }
        }

        long maxTicks = Math.max(existing.maxTicks(), remaining);
        return new ActiveStatusEffect(
                existing.effectKey(), amp, intensity, remaining, maxTicks,
                existing.appliedAtEpochMillis(), now, mergedData
        );
    }

    private static long computeDecayTicks(double intensity, double decayPerSecond, long fallbackTicks) {
        if (decayPerSecond <= 0.0) {
            return fallbackTicks;
        }
        if (intensity <= 0.0) {
            return 0L;
        }
        return Math.max(1L, Math.round((intensity / decayPerSecond) * 20.0));
    }

    private StatusEffectContext createContext(
            UUID playerId,
            ActiveStatusEffect active,
            StatusEffectStage currentStage,
            StatusEffectStage previousStage
    ) {
        return new DefaultStatusEffectContext(
                playerId, active, currentStage, previousStage,
                totalTicks.get(), this, platformHandleResolver.apply(playerId)
        );
    }

    private void safeInvoke(Runnable action, String phase, Key effectKey, UUID playerId) {
        try {
            action.run();
        } catch (Exception e) {
            logger.error(LogChannel.CORE,
                    "Error in status effect " + phase + " for " + effectKey + " on player " + playerId, e);
        }
    }

    private record DefaultStatusEffectContext(
            UUID playerId,
            ActiveStatusEffect activeEffect,
            StatusEffectStage currentStage,
            StatusEffectStage previousStage,
            long tickCount,
            PlayerEffectService effectService,
            Object platformHandle
    ) implements StatusEffectContext {

        @Override
        public UUID getPlayerId() {
            return playerId;
        }

        @Override
        public ActiveStatusEffect getActiveEffect() {
            return activeEffect;
        }

        @Override
        public Optional<StatusEffectStage> getCurrentStage() {
            return Optional.ofNullable(currentStage);
        }

        @Override
        public Optional<StatusEffectStage> getPreviousStage() {
            return Optional.ofNullable(previousStage);
        }

        @Override
        public long getTickCount() {
            return tickCount;
        }

        @Override
        public PlayerEffectService getEffectService() {
            return effectService;
        }

        @Override
        public <T> Optional<T> getPlatformHandle(Class<T> handleType) {
            if (handleType != null && handleType.isInstance(platformHandle)) {
                return Optional.of(handleType.cast(platformHandle));
            }
            return Optional.empty();
        }
    }
}
