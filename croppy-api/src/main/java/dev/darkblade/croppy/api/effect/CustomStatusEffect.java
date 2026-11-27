package dev.darkblade.croppy.api.effect;

import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Extensible definition of a stateful custom status effect tracked per player.
 */
public interface CustomStatusEffect extends Keyed {

    default String getDisplayName() {
        return getKey().value();
    }

    default StackingPolicy getStackingPolicy() {
        return StackingPolicy.REPLACE;
    }

    default long getMaxDurationTicks() {
        return Long.MAX_VALUE;
    }

    default int getMaxAmplifier() {
        return 10;
    }

    default double getMaxIntensity() {
        return 100.0;
    }

    /**
     * Continuous intensity decay rate per second. When positive, the effect is intensity-driven
     * and expires when intensity decays to zero.
     */
    default double getDecayPerSecond() {
        return 0.0;
    }

    default boolean isIntensityDriven() {
        return getDecayPerSecond() > 0.0;
    }

    default boolean isPersistent() {
        return false;
    }

    default boolean isMilkCurable() {
        return true;
    }

    /**
     * Fraction in {@code [0.0, 1.0]} removed when drinking milk (1.0 clears fully).
     */
    default double getMilkReduction() {
        return 1.0;
    }

    default boolean isClearedOnDeath() {
        return true;
    }

    default List<StatusEffectStage> getStages() {
        return List.of();
    }

    /**
     * Resolves the highest matching stage for the given intensity.
     */
    default Optional<StatusEffectStage> resolveStage(double intensity) {
        List<StatusEffectStage> stages = getStages();
        if (stages == null || stages.isEmpty()) {
            return Optional.empty();
        }
        return stages.stream()
                .filter(stage -> intensity >= stage.minIntensity())
                .max(Comparator.comparingDouble(StatusEffectStage::minIntensity)
                        .thenComparingInt(StatusEffectStage::priority));
    }

    default void onApply(StatusEffectContext context) {
    }

    default void onTick(StatusEffectContext context) {
    }

    default void onStageChange(StatusEffectContext context, StatusEffectStage previousStage, StatusEffectStage newStage) {
    }

    default void onExpire(StatusEffectContext context) {
    }

    default void onRemove(StatusEffectContext context, RemovalReason reason) {
    }
}
