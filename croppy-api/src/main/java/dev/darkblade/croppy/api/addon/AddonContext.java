package dev.darkblade.croppy.api.addon;

import dev.darkblade.croppy.api.crafting.CraftingRecipeRegistry;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationRegistry;

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
