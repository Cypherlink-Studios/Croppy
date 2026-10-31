package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.core.registry.SimpleRegistry;

/**
 * Thread-safe default in-memory implementation of {@link MenuRegistry}.
 */
public final class DefaultMenuRegistry extends SimpleRegistry<MenuDefinition> implements MenuRegistry {
}
