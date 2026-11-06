package dev.darkblade.croppy.api.addon;

import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.rule.RuleRegistry;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationRegistry;

import java.nio.file.Path;
import java.util.Optional;
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
     * @return the global fertilizer registry
     */
    FertilizerRegistry getFertilizerRegistry();

    /**
     * @return the global recipe registry
     */
    RecipeRegistry getRecipeRegistry();

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
}
