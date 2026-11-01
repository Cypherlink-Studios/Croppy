package dev.darkblade.croppy.api.recipe;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration governing how a recipe is presented to a player when in the HINTED knowledge state.
 *
 * @param description            custom hint description in MiniMessage format
 * @param revealedIngredientKeys partial list of ingredient item keys revealed in the hint
 * @param hideResultItem         whether to hide the recipe result item icon in the menu
 * @param properties             arbitrary metadata properties
 */
public record HintDefinition(
        String description,
        List<Key> revealedIngredientKeys,
        boolean hideResultItem,
        Map<String, String> properties
) {

    public static final HintDefinition DEFAULT = new HintDefinition(
            "<gray>A culinary mystery waiting to be unraveled...</gray>",
            List.of(),
            true,
            Map.of()
    );

    public HintDefinition {
        Objects.requireNonNull(description, "description cannot be null");
        revealedIngredientKeys = (revealedIngredientKeys == null) ? List.of() : List.copyOf(revealedIngredientKeys);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }
}
