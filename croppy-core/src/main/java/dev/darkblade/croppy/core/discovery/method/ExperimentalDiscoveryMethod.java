package dev.darkblade.croppy.core.discovery.method;

import dev.darkblade.croppy.api.discovery.DiscoveryContext;
import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryMethod;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.core.recipe.RecipeMatcher;

import java.util.Objects;
import java.util.Optional;

/**
 * Discovery method that unlocks a recipe when a player tests a valid combination
 * of ingredients inside a compatible crafting station.
 */
public final class ExperimentalDiscoveryMethod implements RecipeDiscoveryMethod {

    public static final Key KEY = Key.of("croppy", "experimental");

    private final RecipeMatcher matcher;

    public ExperimentalDiscoveryMethod(RecipeMatcher matcher) {
        this.matcher = Objects.requireNonNull(matcher, "matcher cannot be null");
    }

    public ExperimentalDiscoveryMethod() {
        this(new RecipeMatcher());
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public DiscoveryResult evaluate(DiscoveryContext context, RecipeDefinition recipe) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");

        if (!recipe.enabledDiscoveryMethods().contains(KEY)) {
            return DiscoveryResult.INELIGIBLE;
        }

        // Verify station key if provided in triggerKey
        Optional<Key> stationKeyOpt = context.getTriggerKey();
        if (stationKeyOpt.isPresent() && !recipe.stationKey().equals(stationKeyOpt.get())) {
            return DiscoveryResult.INELIGIBLE;
        }

        // Check if inputs match recipe
        if (matcher.isMatch(recipe, context.inputs())) {
            return DiscoveryResult.DISCOVERED;
        }

        return DiscoveryResult.INELIGIBLE;
    }
}
