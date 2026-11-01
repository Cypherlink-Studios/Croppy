package dev.darkblade.croppy.core.discovery.method;

import dev.darkblade.croppy.api.discovery.DiscoveryContext;
import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryMethod;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.Objects;
import java.util.Optional;

/**
 * Discovery method that unlocks a recipe automatically when the player acquires or harvests
 * a relevant ingredient or trigger item.
 */
public final class AutomaticDiscoveryMethod implements RecipeDiscoveryMethod {

    public static final Key KEY = Key.of("croppy", "automatic");

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

        Optional<Key> triggerKeyOpt = context.getTriggerKey();
        if (triggerKeyOpt.isEmpty()) {
            return DiscoveryResult.INELIGIBLE;
        }

        Key trigger = triggerKeyOpt.get();

        // 1. Check explicit trigger property if specified
        String explicitTrigger = recipe.properties().get("automatic_trigger");
        if (explicitTrigger != null && Key.parse(explicitTrigger).equals(trigger)) {
            return DiscoveryResult.DISCOVERED;
        }

        // 2. Check if acquired item is one of the recipe input requirements
        for (StoredItem input : recipe.inputRequirements()) {
            if (input.itemKey().equals(trigger)) {
                return DiscoveryResult.DISCOVERED;
            }
        }

        return DiscoveryResult.INELIGIBLE;
    }
}
