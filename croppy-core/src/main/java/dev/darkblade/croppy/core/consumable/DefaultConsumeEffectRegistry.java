package dev.darkblade.croppy.core.consumable;

import dev.darkblade.croppy.api.consumable.ConsumeEffectHandler;
import dev.darkblade.croppy.api.consumable.ConsumeEffectRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Thread-safe default implementation of {@link ConsumeEffectRegistry}.
 */
public class DefaultConsumeEffectRegistry extends SimpleRegistry<ConsumeEffectHandler> implements ConsumeEffectRegistry {
}
