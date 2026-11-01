package dev.darkblade.croppy.api.recipe;

import dev.darkblade.croppy.api.identifier.Key;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing a player's knowledge record for a specific recipe.
 *
 * @param playerId           unique identifier of the player
 * @param recipeKey          unique identifier of the recipe
 * @param state              current knowledge state (UNKNOWN, HINTED, DISCOVERED)
 * @param discoveredAt       timestamp when the knowledge state was acquired (nullable for UNKNOWN)
 * @param discoveryMethodKey key of the method that granted this discovery (nullable for UNKNOWN)
 * @param metadata           arbitrary metadata associated with this knowledge entry (e.g. partial hints)
 */
public record PlayerRecipeKnowledge(
        UUID playerId,
        Key recipeKey,
        KnowledgeState state,
        Instant discoveredAt,
        Key discoveryMethodKey,
        Map<String, String> metadata
) {

    public PlayerRecipeKnowledge {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipeKey, "recipeKey cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Constructs an initial UNKNOWN knowledge entry.
     *
     * @param playerId  player UUID
     * @param recipeKey recipe key
     * @return unknown recipe knowledge
     */
    public static PlayerRecipeKnowledge unknown(UUID playerId, Key recipeKey) {
        return new PlayerRecipeKnowledge(playerId, recipeKey, KnowledgeState.UNKNOWN, null, null, Map.of());
    }

    /**
     * Constructs a HINTED knowledge entry.
     *
     * @param playerId           player UUID
     * @param recipeKey          recipe key
     * @param discoveryMethodKey method key
     * @param metadata           hint metadata
     * @return hinted recipe knowledge
     */
    public static PlayerRecipeKnowledge hinted(UUID playerId, Key recipeKey, Key discoveryMethodKey, Map<String, String> metadata) {
        return new PlayerRecipeKnowledge(playerId, recipeKey, KnowledgeState.HINTED, Instant.now(), discoveryMethodKey, metadata);
    }

    /**
     * Constructs a DISCOVERED knowledge entry.
     *
     * @param playerId           player UUID
     * @param recipeKey          recipe key
     * @param discoveryMethodKey method key
     * @return discovered recipe knowledge
     */
    public static PlayerRecipeKnowledge discovered(UUID playerId, Key recipeKey, Key discoveryMethodKey) {
        return new PlayerRecipeKnowledge(playerId, recipeKey, KnowledgeState.DISCOVERED, Instant.now(), discoveryMethodKey, Map.of());
    }
}
