package dev.darkblade.croppy.core.effect;

import dev.darkblade.croppy.api.effect.CustomStatusEffect;
import dev.darkblade.croppy.api.effect.StatusEffectRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Thread-safe default implementation of {@link StatusEffectRegistry}.
 */
public class DefaultStatusEffectRegistry extends SimpleRegistry<CustomStatusEffect> implements StatusEffectRegistry {
}
