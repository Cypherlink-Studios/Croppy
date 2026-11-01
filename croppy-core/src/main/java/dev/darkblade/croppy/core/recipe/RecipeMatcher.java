package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.*;

/**
 * Intelligent ingredient matching engine evaluating recipes with strict balance rules
 * and specificity priority to eliminate recipe subset shadowing.
 */
public final class RecipeMatcher {

    /**
     * Finds the best matching recipe for the given available inputs, prioritized by specificity.
     *
     * @param candidates      available recipe definitions
     * @param availableInputs items currently placed in the station
     * @return the highest-specificity matching recipe, or empty if none matches
     */
    public Optional<RecipeDefinition> findMatch(Collection<RecipeDefinition> candidates, List<StoredItem> availableInputs) {
        List<RecipeDefinition> matches = findAllMatches(candidates, availableInputs);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    /**
     * Finds all matching recipes sorted by specificity ranking (most specific first).
     *
     * @param candidates      available recipe definitions
     * @param availableInputs items currently placed in the station
     * @return list of matching recipes sorted by specificity
     */
    public List<RecipeDefinition> findAllMatches(Collection<RecipeDefinition> candidates, List<StoredItem> availableInputs) {
        if (candidates == null || candidates.isEmpty() || availableInputs == null || availableInputs.isEmpty()) {
            return List.of();
        }

        Map<Key, Integer> availableMap = aggregate(availableInputs);
        if (availableMap.isEmpty()) {
            return List.of();
        }

        List<RecipeDefinition> matches = new ArrayList<>();
        for (RecipeDefinition recipe : candidates) {
            if (matchesRecipe(recipe, availableMap)) {
                matches.add(recipe);
            }
        }

        matches.sort(Comparator
                .comparingInt(this::calculateIngredientTypeCount).reversed()
                .thenComparingInt(this::calculateTotalQuantity).reversed()
                .thenComparing(r -> r.key().asString())
        );

        return Collections.unmodifiableList(matches);
    }

    /**
     * Checks if a specific recipe is satisfied by the available inputs.
     *
     * @param recipe          candidate recipe definition
     * @param availableInputs available station inputs
     * @return true if the inputs satisfy the recipe
     */
    public boolean isMatch(RecipeDefinition recipe, List<StoredItem> availableInputs) {
        if (recipe == null || availableInputs == null || availableInputs.isEmpty()) {
            return false;
        }
        return matchesRecipe(recipe, aggregate(availableInputs));
    }

    private boolean matchesRecipe(RecipeDefinition recipe, Map<Key, Integer> availableMap) {
        Map<Key, Integer> requiredMap = aggregate(recipe.inputRequirements());
        if (requiredMap.isEmpty()) {
            return false;
        }

        // 1. Check if all required items are present in sufficient amounts
        for (Map.Entry<Key, Integer> req : requiredMap.entrySet()) {
            int available = availableMap.getOrDefault(req.getKey(), 0);
            if (available < req.getValue()) {
                return false;
            }
        }

        // 2. In strict mode (allowExcessInputs == false), reject any extraneous item types
        if (!recipe.allowExcessInputs()) {
            for (Key availableKey : availableMap.keySet()) {
                if (!requiredMap.containsKey(availableKey)) {
                    return false;
                }
            }
        }

        return true;
    }

    private int calculateIngredientTypeCount(RecipeDefinition recipe) {
        return recipe.inputRequirements().size();
    }

    private int calculateTotalQuantity(RecipeDefinition recipe) {
        int total = 0;
        for (StoredItem item : recipe.inputRequirements()) {
            total += item.amount();
        }
        return total;
    }

    private Map<Key, Integer> aggregate(List<StoredItem> items) {
        Map<Key, Integer> map = new HashMap<>();
        for (StoredItem item : items) {
            if (item != null && item.amount() > 0) {
                map.merge(item.itemKey(), item.amount(), Integer::sum);
            }
        }
        return map;
    }
}
