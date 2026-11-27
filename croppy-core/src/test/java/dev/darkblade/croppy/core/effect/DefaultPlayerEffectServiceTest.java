package dev.darkblade.croppy.core.effect;

import dev.darkblade.croppy.api.effect.ActiveStatusEffect;
import dev.darkblade.croppy.api.effect.CustomStatusEffect;
import dev.darkblade.croppy.api.effect.PlayerEffectStorage;
import dev.darkblade.croppy.api.effect.RemovalReason;
import dev.darkblade.croppy.api.effect.StackingPolicy;
import dev.darkblade.croppy.api.effect.StatusEffectContext;
import dev.darkblade.croppy.api.effect.StatusEffectStage;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPlayerEffectServiceTest {

    private DefaultStatusEffectRegistry registry;
    private MutableClock clock;
    private DefaultPlayerEffectService service;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        registry = new DefaultStatusEffectRegistry();
        clock = new MutableClock(1_000_000L);
        service = new DefaultPlayerEffectService(registry, NoOpCroppyLogger.INSTANCE, clock);
        playerId = UUID.randomUUID();
    }

    @Test
    void testStackingPoliciesExtendUpgradeAndReplace() {
        Key extendKey = Key.of("croppy", "extend_buff");
        registry.register(new SimpleTestEffect(extendKey, StackingPolicy.EXTEND, 200L, 3, 0.0, false, true, 1.0, true));

        service.apply(playerId, extendKey, 100L, 0);
        ActiveStatusEffect extended = service.apply(playerId, extendKey, 150L, 1);
        assertEquals(200L, extended.remainingTicks());
        assertEquals(1, extended.amplifier());

        Key upgradeKey = Key.of("croppy", "upgrade_buff");
        registry.register(new SimpleTestEffect(upgradeKey, StackingPolicy.UPGRADE, 500L, 2, 0.0, false, true, 1.0, true));
        service.apply(playerId, upgradeKey, 100L, 0);
        assertEquals(1, service.apply(playerId, upgradeKey, 100L, 0).amplifier());
        assertEquals(2, service.apply(playerId, upgradeKey, 100L, 0).amplifier());
        assertEquals(2, service.apply(playerId, upgradeKey, 100L, 0).amplifier());
    }

    @Test
    void testIntensityDecayStagesMilkReductionAndOfflinePersistence() {
        Key alcoholKey = Key.of("croppy", "intoxication");
        List<String> stageTransitions = new ArrayList<>();
        List<StatusEffectStage> stages = List.of(
                new StatusEffectStage("tipsy", "Tipsy", 20.0, 1),
                new StatusEffectStage("drunk", "Drunk", 45.0, 2)
        );

        registry.register(new CustomStatusEffect() {
            @Override
            public Key getKey() {
                return alcoholKey;
            }

            @Override
            public StackingPolicy getStackingPolicy() {
                return StackingPolicy.STACK_INTENSITY;
            }

            @Override
            public double getMaxIntensity() {
                return 100.0;
            }

            @Override
            public double getDecayPerSecond() {
                return 1.0;
            }

            @Override
            public boolean isPersistent() {
                return true;
            }

            @Override
            public double getMilkReduction() {
                return 0.5;
            }

            @Override
            public List<StatusEffectStage> getStages() {
                return stages;
            }

            @Override
            public void onStageChange(StatusEffectContext context, StatusEffectStage prev, StatusEffectStage next) {
                stageTransitions.add((prev != null ? prev.id() : "none") + "->" + (next != null ? next.id() : "none"));
            }
        });

        Map<UUID, List<ActiveStatusEffect>> backingStore = new HashMap<>();
        service.setStorage(new PlayerEffectStorage() {
            @Override
            public List<ActiveStatusEffect> load(UUID id) {
                return backingStore.getOrDefault(id, List.of());
            }

            @Override
            public void save(UUID id, List<ActiveStatusEffect> effects) {
                backingStore.put(id, new ArrayList<>(effects));
            }

            @Override
            public void clear(UUID id) {
                backingStore.remove(id);
            }
        });

        service.apply(playerId, alcoholKey, 0L, 0, 25.0, Map.of());
        assertEquals("tipsy", service.getCurrentStage(playerId, alcoholKey).orElseThrow().id());

        service.apply(playerId, alcoholKey, 0L, 0, 25.0, Map.of());
        assertEquals(50.0, service.getActiveEffect(playerId, alcoholKey).orElseThrow().intensity(), 0.001);
        assertEquals("drunk", service.getCurrentStage(playerId, alcoholKey).orElseThrow().id());
        assertEquals(List.of("none->tipsy", "tipsy->drunk"), stageTransitions);

        // Milk halves intensity from 50 -> 25 (back to tipsy)
        service.handleMilkConsume(playerId);
        assertEquals(25.0, service.getActiveEffect(playerId, alcoholKey).orElseThrow().intensity(), 0.001);
        assertEquals("tipsy", service.getCurrentStage(playerId, alcoholKey).orElseThrow().id());

        // Quit & rejoin 10 seconds later: intensity decays by 10 -> 15 (no longer tipsy)
        service.handlePlayerQuit(playerId, true);
        assertFalse(service.hasEffect(playerId, alcoholKey));
        clock.advanceMillis(10_000L);
        service.handlePlayerJoin(playerId);
        assertEquals(15.0, service.getActiveEffect(playerId, alcoholKey).orElseThrow().intensity(), 0.001);
        assertTrue(service.getCurrentStage(playerId, alcoholKey).isEmpty());

        // Tick 16 seconds (320 ticks) -> decays to 0 and expires
        service.tick(320L);
        assertFalse(service.hasEffect(playerId, alcoholKey));
    }

    @Test
    void testPurgeDefinitionAndDeathHandling() {
        Key stickyKey = Key.of("croppy", "sticky");
        Key volatileKey = Key.of("addon", "volatile");
        registry.register(new SimpleTestEffect(stickyKey, StackingPolicy.REPLACE, 1000L, 1, 0.0, false, true, 1.0, false));
        registry.register(new SimpleTestEffect(volatileKey, StackingPolicy.REPLACE, 1000L, 1, 0.0, false, true, 1.0, true));

        service.apply(playerId, stickyKey, 200L, 0);
        service.apply(playerId, volatileKey, 200L, 0);

        service.handlePlayerDeath(playerId);
        assertTrue(service.hasEffect(playerId, stickyKey));
        assertFalse(service.hasEffect(playerId, volatileKey));

        assertEquals(1, service.purgeEffectDefinition(stickyKey));
        assertFalse(service.hasEffect(playerId, stickyKey));
    }

    private record SimpleTestEffect(
            Key key,
            StackingPolicy policy,
            long maxDuration,
            int maxAmp,
            double decay,
            boolean persistent,
            boolean milkCurable,
            double milkReduction,
            boolean clearedOnDeath
    ) implements CustomStatusEffect {
        @Override public Key getKey() { return key; }
        @Override public StackingPolicy getStackingPolicy() { return policy; }
        @Override public long getMaxDurationTicks() { return maxDuration; }
        @Override public int getMaxAmplifier() { return maxAmp; }
        @Override public double getDecayPerSecond() { return decay; }
        @Override public boolean isPersistent() { return persistent; }
        @Override public boolean isMilkCurable() { return milkCurable; }
        @Override public double getMilkReduction() { return milkReduction; }
        @Override public boolean isClearedOnDeath() { return clearedOnDeath; }
    }

    private static final class MutableClock extends Clock {
        private long millis;

        MutableClock(long initialMillis) {
            this.millis = initialMillis;
        }

        void advanceMillis(long delta) {
            this.millis += delta;
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }
}
