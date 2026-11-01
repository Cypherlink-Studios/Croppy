package dev.darkblade.croppy.api.discovery;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;

/**
 * Service provider interface defining a mechanism through which players can learn culinary recipes.
 */
public interface RecipeDiscoveryMethod extends Keyed {

    @Override
    Key getKey();

    /**
     * Evaluates whether a recipe can be discovered under the provided context.
     *
     * @param context contextual evaluation data
     * @param recipe  candidate recipe definition
     * @return outcome of the discovery attempt
     */
    DiscoveryResult evaluate(DiscoveryContext context, RecipeDefinition recipe);
}
