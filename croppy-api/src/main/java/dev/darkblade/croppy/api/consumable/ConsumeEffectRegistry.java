package dev.darkblade.croppy.api.consumable;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Thread-safe registry of {@link ConsumeEffectHandler}s keyed by namespaced {@code Key}.
 */
public interface ConsumeEffectRegistry extends Registry<ConsumeEffectHandler>, Service {
}
