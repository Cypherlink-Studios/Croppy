package dev.darkblade.croppy.api.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for persistent storage and retrieval of player recipe knowledge records.
 */
public interface RecipeKnowledgeStorage {

    /**
     * Asynchronously loads all persisted recipe knowledge for the specified player.
     *
     * @param playerId unique player UUID
     * @return future containing a map of recipe keys to player recipe knowledge
     */
    CompletableFuture<Map<Key, PlayerRecipeKnowledge>> loadKnowledge(UUID playerId);

    /**
     * Asynchronously persists a single recipe knowledge record for a player.
     *
     * @param playerId  unique player UUID
     * @param knowledge recipe knowledge record to save
     * @return future that completes when persisted
     */
    CompletableFuture<Void> saveKnowledge(UUID playerId, PlayerRecipeKnowledge knowledge);

    /**
     * Asynchronously persists a collection of recipe knowledge records for a player.
     *
     * @param playerId  unique player UUID
     * @param knowledge collection of knowledge records to save
     * @return future that completes when persisted
     */
    CompletableFuture<Void> saveAllKnowledge(UUID playerId, Collection<PlayerRecipeKnowledge> knowledge);

    /**
     * Asynchronously deletes a recipe knowledge entry for a player (resetting to unknown).
     *
     * @param playerId  unique player UUID
     * @param recipeKey recipe key to remove
     * @return future that completes when deleted
     */
    CompletableFuture<Void> deleteKnowledge(UUID playerId, Key recipeKey);

    /**
     * Asynchronously clears all recipe knowledge records for a player.
     *
     * @param playerId unique player UUID
     * @return future that completes when cleared
     */
    CompletableFuture<Void> clearKnowledge(UUID playerId);
}
