package dev.darkblade.croppy.api.discovery;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.registry.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating recipe discovery evaluations, knowledge state queries, and method registrations.
 */
public interface RecipeDiscoveryEngine extends Service {

    /**
     * Registers a discovery method provider.
     *
     * @param method method provider to register
     */
    void registerMethod(RecipeDiscoveryMethod method);

    /**
     * Retrieves a registered discovery method by its unique key.
     *
     * @param methodKey method key
     * @return optional containing the method if found
     */
    Optional<RecipeDiscoveryMethod> getMethod(Key methodKey);

    /**
     * Retrieves all registered discovery methods.
     *
     * @return unmodifiable collection of registered methods
     */
    Collection<RecipeDiscoveryMethod> getMethods();

    /**
     * Retrieves the current knowledge state for a player regarding a specific recipe.
     *
     * @param playerId  player UUID
     * @param recipeKey recipe key
     * @return current KnowledgeState (defaults to UNKNOWN if not yet registered)
     */
    KnowledgeState getKnowledgeState(UUID playerId, Key recipeKey);

    /**
     * Retrieves the full knowledge record for a player and recipe.
     *
     * @param playerId  player UUID
     * @param recipeKey recipe key
     * @return optional containing knowledge record if recorded
     */
    Optional<PlayerRecipeKnowledge> getKnowledge(UUID playerId, Key recipeKey);

    /**
     * Retrieves an immutable view of all known recipes for a player.
     *
     * @param playerId player UUID
     * @return map of recipe keys to player recipe knowledge
     */
    Map<Key, PlayerRecipeKnowledge> getAllKnowledge(UUID playerId);

    /**
     * Loads a player's persisted knowledge map into memory cache.
     *
     * @param playerId  player UUID
     * @param knowledge map of recipe keys to knowledge records
     */
    void loadPlayerKnowledge(UUID playerId, Map<Key, PlayerRecipeKnowledge> knowledge);

    /**
     * Evicts a player's knowledge records from memory cache upon logout.
     *
     * @param playerId player UUID
     */
    void unloadPlayerKnowledge(UUID playerId);

    /**
     * Idempotently grants full discovery of a recipe to a player.
     *
     * @param playerId           player UUID
     * @param recipe             recipe to discover
     * @param discoveryMethodKey method granting this discovery
     * @return outcome of the discovery attempt (DISCOVERED, or ALREADY_KNOWN if previously unlocked)
     */
    DiscoveryResult discoverRecipe(UUID playerId, RecipeDefinition recipe, Key discoveryMethodKey);

    /**
     * Idempotently grants a hint for a recipe to a player.
     *
     * @param playerId           player UUID
     * @param recipe             recipe to hint
     * @param discoveryMethodKey method granting this hint
     * @param message            hint description or clue
     * @return outcome of the hint attempt (HINTED, or ALREADY_KNOWN if already discovered)
     */
    DiscoveryResult grantHint(UUID playerId, RecipeDefinition recipe, Key discoveryMethodKey, String message);

    /**
     * Revokes recipe knowledge from a player, resetting state to UNKNOWN.
     *
     * @param playerId  player UUID
     * @param recipeKey recipe key
     * @return true if knowledge was revoked, false if previously UNKNOWN
     */
    boolean revokeRecipe(UUID playerId, Key recipeKey);
}
