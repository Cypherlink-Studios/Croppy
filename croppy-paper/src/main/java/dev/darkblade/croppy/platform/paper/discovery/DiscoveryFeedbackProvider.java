package dev.darkblade.croppy.platform.paper.discovery;

import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import org.bukkit.entity.Player;

/**
 * Contract for delivering sensory feedback when a player discovers recipes or unlocks hints.
 */
public interface DiscoveryFeedbackProvider {

    /**
     * Delivers celebratory fanfare upon discovering a recipe.
     *
     * @param player player who discovered the recipe
     * @param recipe recipe definition
     */
    void playDiscoveryFanfare(Player player, RecipeDefinition recipe);

    /**
     * Delivers subtle clue feedback upon unlocking a hint.
     *
     * @param player player who unlocked the hint
     * @param hint   hint text
     */
    default void playHintFeedback(Player player, String hint) {}
}
