package dev.darkblade.croppy.api.process;

import dev.darkblade.croppy.api.registry.Registry;
import dev.darkblade.croppy.api.registry.Service;

/**
 * Registry containing all registered {@link ProcessDefinition} recipes.
 */
public interface ProcessRegistry extends Registry<ProcessDefinition>, Service {
}
