package dev.darkblade.croppy.api.recipe;

/**
 * Represents a player's familiarity and unlocked state for a culinary recipe.
 */
public enum KnowledgeState {

    /**
     * The player has not encountered or discovered this recipe.
     */
    UNKNOWN,

    /**
     * The player has acquired partial information or hints about this recipe.
     */
    HINTED,

    /**
     * The player has fully discovered and unlocked this recipe.
     */
    DISCOVERED
}
