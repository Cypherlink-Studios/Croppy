package dev.darkblade.croppy.api.effect;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Thread-safe registry of {@link CustomStatusEffect} definitions keyed by namespaced {@code Key}.
 */
public interface StatusEffectRegistry extends Registry<CustomStatusEffect>, Service {
}
