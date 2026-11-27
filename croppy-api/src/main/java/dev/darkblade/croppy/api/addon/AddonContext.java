package dev.darkblade.croppy.api.addon;

import dev.darkblade.croppy.api.consumable.ConsumeEffectRegistry;
import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.effect.PlayerEffectService;
import dev.darkblade.croppy.api.effect.StatusEffectRegistry;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.api.item.ItemMetadataRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessListener;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationActionRegistry;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.storage.DatabaseService;
import dev.darkblade.croppy.api.variable.DataTypeRegistry;

import java.nio.file.Path;
import java.util.Optional;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;

import java.util.logging.Logger;

/**
 * Scoped domain environment and service container provided to an enabled addon.
 */
public interface AddonContext {

    /**
     * @return the immutable metadata describing this addon
     */
    AddonDescription getDescription();

    /**
     * @return the dedicated logger for this addon
     */
    Logger getLogger();

    /**
     * Returns the structured {@link CroppyLogger} scoped to this addon.
     *
     * @return the addon's {@link CroppyLogger}
     */
    default CroppyLogger getCroppyLogger() {
        return new JulCroppyLogger(getLogger());
    }

    /**
     * @return the root data directory dedicated to this addon
     */
    Path getDataFolder();

    /**
     * @return the global species registry
     */
    SpeciesRegistry getSpeciesRegistry();

    /**
     * @return the global item registry
     */
    ItemRegistry getItemRegistry();

    /**
     * @return the global item category registry
     */
    ItemCategoryRegistry getItemCategoryRegistry();

    /**
     * @return the global fertilizer registry
     */
    FertilizerRegistry getFertilizerRegistry();

    /**
     * @return the global recipe registry
     */
    RecipeRegistry getRecipeRegistry();

    /**
     * @return the global crafting recipe registry
     */
    CraftingRecipeRegistry getCraftingRecipeRegistry();

    /**
     * @return the global recipe category registry
     */
    RecipeCategoryRegistry getRecipeCategoryRegistry();

    /**
     * @return the global process registry
     */
    ProcessRegistry getProcessRegistry();

    /**
     * @return the global station registry
     */
    StationRegistry getStationRegistry();

    /**
     * @return the global interaction registry
     */
    InteractionRegistry getInteractionRegistry();

    /**
     * @return the global rule registry
     */
    RuleRegistry getRuleRegistry();

    /**
     * @return the global menu registry
     */
    MenuRegistry getMenuRegistry();

    /**
     * @return the global typed variable data type registry
     */
    DataTypeRegistry getDataTypeRegistry();

    /**
     * @return the global recipe discovery engine
     */
    RecipeDiscoveryEngine getRecipeDiscoveryEngine();

    /**
     * @return the global item metadata contributor registry
     */
    ItemMetadataRegistry getItemMetadataRegistry();

    /**
     * @return the global station menu action contributor registry
     */
    StationActionRegistry getStationActionRegistry();

    /**
     * @return the consume effect handler registry (tracked per addon when supported)
     */
    default ConsumeEffectRegistry getConsumeEffectRegistry() {
        return null;
    }

    /**
     * @return the custom status effect registry (tracked per addon when supported)
     */
    default StatusEffectRegistry getStatusEffectRegistry() {
        return null;
    }

    /**
     * @return the player status effect service
     */
    default PlayerEffectService getPlayerEffectService() {
        return null;
    }

    /**
     * Returns an addon-scoped relational {@link DatabaseService} enforcing table-prefix isolation,
     * or empty if relational database storage is unavailable.
     *
     * @return optional scoped database service
     */
    Optional<DatabaseService> getDatabaseService();

    /**
     * Registers a {@link ProcessListener} under this addon's lifecycle.
     * Automatically unregistered when the addon is disabled.
     *
     * @param listener process listener
     */
    void registerProcessListener(ProcessListener listener);

    /**
     * Submits a managed asynchronous task under this addon's lifecycle.
     *
     * @param task the task to execute
     */
    void runAsync(Runnable task);

    /**
     * Returns an optional platform-specific adapter object if available.
     *
     * @param adapterType expected adapter class
     * @param <T> adapter type
     * @return the adapter instance or empty if incompatible
     */
    <T> Optional<T> getPlatformAdapter(Class<T> adapterType);

    /**
     * @return the active configuration instance for this addon
     */
    AddonConfig getConfig();

    /**
     * Extracts the bundled default config.yml to this addon's data folder if it does not already exist.
     */
    void saveDefaultConfig();

    /**
     * Reloads this addon's configuration from disk.
     */
    void reloadConfig();

    /**
     * Persists this addon's in-memory configuration modifications to disk.
     */
    void saveConfig();
}
