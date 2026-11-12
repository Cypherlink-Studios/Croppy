package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.CroppyAddon;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.core.CroppyCore;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Executes the 5-phase decommissioning protocol for Croppy addons during disable or unload.
 */
public final class AddonDecommissioner {

    private AddonDecommissioner() {}

    /**
     * Decommissions an addon by executing the 5-phase cleanup protocol.
     *
     * @param addonInstance optional Java addon instance (null for declarative content packs)
     * @param context the addon's tracked context
     * @param classLoader the addon's classloader (null if declarative pack or already detached)
     * @param core the core Croppy engine
     * @param logger logger for diagnostic feedback
     * @param isUnload whether to execute Phase 5 (severing classloader and references)
     */
    public static void decommission(CroppyAddon addonInstance,
                                    PaperAddonContext context,
                                    AddonClassLoader classLoader,
                                    CroppyCore core,
                                    CroppyLogger logger,
                                    boolean isUnload) {
        Objects.requireNonNull(context, "context cannot be null");
        CroppyLogger log = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
        String id = context.getDescription().id();

        // Phase 1: Notification
        if (addonInstance != null) {
            try {
                addonInstance.onDisable(context);
            } catch (Throwable t) {
                log.error(LogChannel.ADDON, "Exception thrown during onDisable for addon '" + id + "'", t);
            }
        }

        // Phase 2: Listener & Task Deregistration
        for (Listener listener : context.getRegisteredListeners()) {
            try {
                HandlerList.unregisterAll(listener);
            } catch (Throwable t) {
                log.warn(LogChannel.ADDON, "Failed to unregister listener for addon '" + id + "'", t);
            }
        }

        for (BukkitTask task : context.getRegisteredTasks()) {
            try {
                task.cancel();
            } catch (Throwable ignored) {}
        }

        // Phase 3: Thread & Executor Draining
        context.shutdownExecutor();

        // Phase 4: Domain Registry Purging
        if (core != null) {
            // Remove growth modifiers
            for (GrowthModifier modifier : context.getRegisteredGrowthModifiers()) {
                core.getGrowthEngine().removeModifier(modifier);
            }

            // Unregister keyed entries
            for (Key key : context.getRegisteredSpecies()) {
                core.getSpeciesRegistry().unregister(key);
            }
            for (Key key : context.getRegisteredItems()) {
                core.getItemRegistry().unregister(key);
            }
            for (Key key : context.getRegisteredRecipes()) {
                core.getRecipeRegistry().unregister(key);
            }
            if (core.getCraftingRecipeRegistry() != null) {
                for (Key key : context.getRegisteredCraftingRecipes()) {
                    core.getCraftingRecipeRegistry().unregister(key);
                    try {
                        org.bukkit.Bukkit.removeRecipe(new org.bukkit.NamespacedKey(key.namespace(), key.value()));
                    } catch (Throwable ignored) {}
                }
            }
            for (Key key : context.getRegisteredProcesses()) {
                core.getProcessRegistry().unregister(key);
            }
            for (Key key : context.getRegisteredStations()) {
                core.getStationRegistry().unregister(key);
            }
            if (core.getMenuRegistry() != null) {
                for (Key key : context.getRegisteredMenus()) {
                    core.getMenuRegistry().unregister(key);
                }
            }
            if (core.getInteractionRegistry() != null) {
                for (Key key : context.getRegisteredInteractions()) {
                    core.getInteractionRegistry().unregister(key);
                }
            }
            if (core.getRecipeCategoryRegistry() != null) {
                for (Key key : context.getRegisteredCategories()) {
                    core.getRecipeCategoryRegistry().unregister(key);
                }
            }
            if (core.getItemCategoryRegistry() != null) {
                for (Key key : context.getRegisteredItemCategories()) {
                    core.getItemCategoryRegistry().unregister(key);
                }
            }
        }

        // Phase 5: ClassLoader Severing (Unload only)
        if (isUnload && classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn(LogChannel.ADDON, "Error closing class loader for addon '" + id + "'", e);
            }
        }
    }

    /**
     * Backwards-compatible overload accepting a JUL {@link Logger}.
     */
    public static void decommission(CroppyAddon addonInstance,
                                    PaperAddonContext context,
                                    AddonClassLoader classLoader,
                                    CroppyCore core,
                                    Logger logger,
                                    boolean isUnload) {
        decommission(
                addonInstance,
                context,
                classLoader,
                core,
                logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE,
                isUnload
        );
    }
}
