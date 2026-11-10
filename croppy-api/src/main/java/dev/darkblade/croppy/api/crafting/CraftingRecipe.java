package dev.darkblade.croppy.api.crafting;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a standard Minecraft crafting table recipe.
 *
 * @param key                  unique namespaced recipe identifier
 * @param type                 crafting recipe type (shaped or shapeless)
 * @param group                optional recipe book group identifier
 * @param category             optional vanilla crafting book category (e.g., "misc", "building", "equipment")
 * @param resultItemKey        identifier of the resulting item
 * @param resultAmount         output quantity produced
 * @param shape                row patterns for shaped recipes (empty for shapeless)
 * @param shapedIngredients    mapping of shape characters to ingredient choices (empty for shapeless)
 * @param shapelessIngredients list of ingredient requirements (empty for shaped)
 */
public record CraftingRecipe(
        Key key,
        CraftingType type,
        String group,
        String category,
        Key resultItemKey,
        int resultAmount,
        List<String> shape,
        Map<Character, IngredientChoice> shapedIngredients,
        List<IngredientChoice> shapelessIngredients
) implements Keyed {

    public CraftingRecipe {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(resultItemKey, "resultItemKey cannot be null");

        if (resultAmount <= 0) {
            throw new IllegalArgumentException("resultAmount must be positive, got: " + resultAmount);
        }

        group = (group == null) ? "" : group;
        category = (category == null) ? "misc" : category;
        shape = (shape == null) ? List.of() : List.copyOf(shape);
        shapedIngredients = (shapedIngredients == null) ? Map.of() : Map.copyOf(shapedIngredients);
        shapelessIngredients = (shapelessIngredients == null) ? List.of() : List.copyOf(shapelessIngredients);

        if (type == CraftingType.SHAPED) {
            if (shape.isEmpty() || shape.size() > 3) {
                throw new IllegalArgumentException("Shaped recipe must have between 1 and 3 rows, got: " + shape.size());
            }
            int cols = shape.get(0).length();
            if (cols == 0 || cols > 3) {
                throw new IllegalArgumentException("Shaped recipe row width must be between 1 and 3, got: " + cols);
            }
            for (String row : shape) {
                if (row.length() != cols) {
                    throw new IllegalArgumentException("All rows in shaped recipe must have identical length");
                }
                for (int i = 0; i < row.length(); i++) {
                    char c = row.charAt(i);
                    if (c != ' ' && !shapedIngredients.containsKey(c)) {
                        throw new IllegalArgumentException("Shape character '" + c + "' has no mapped ingredient");
                    }
                }
            }
        } else if (type == CraftingType.SHAPELESS) {
            if (shapelessIngredients.isEmpty()) {
                throw new IllegalArgumentException("Shapeless recipe must have at least one ingredient");
            }
            int totalCount = 0;
            for (IngredientChoice choice : shapelessIngredients) {
                totalCount += choice.amount();
            }
            if (totalCount > 9) {
                throw new IllegalArgumentException("Shapeless recipe cannot require more than 9 items, got: " + totalCount);
            }
        }
    }

    @Override
    public Key getKey() {
        return key;
    }

    /**
     * Constructs a shaped recipe definition.
     */
    public static CraftingRecipe shaped(
            Key key,
            String group,
            String category,
            Key resultItemKey,
            int resultAmount,
            List<String> shape,
            Map<Character, IngredientChoice> ingredients
    ) {
        return new CraftingRecipe(
                key,
                CraftingType.SHAPED,
                group,
                category,
                resultItemKey,
                resultAmount,
                shape,
                ingredients,
                List.of()
        );
    }

    /**
     * Constructs a shapeless recipe definition.
     */
    public static CraftingRecipe shapeless(
            Key key,
            String group,
            String category,
            Key resultItemKey,
            int resultAmount,
            List<IngredientChoice> ingredients
    ) {
        return new CraftingRecipe(
                key,
                CraftingType.SHAPELESS,
                group,
                category,
                resultItemKey,
                resultAmount,
                List.of(),
                Map.of(),
                ingredients
        );
    }
}
