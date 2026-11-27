package dev.darkblade.croppy.platform.paper.command.selector;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.AddonManager;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.parser.ArgumentParseResult;

import java.util.List;
import java.util.Optional;

/**
 * Factory creating pre-configured {@link SelectableTargetParser} instances for Croppy commands.
 */
public final class CroppyTargetSelectors {

    private CroppyTargetSelectors() {
    }

    /**
     * Builds a target parser for {@link AddonDescription} supporting {@code <addonId>} and {@code @all}.
     */
    public static <C> SelectableTargetParser<C, AddonDescription> addonParser(@Nullable AddonManager addonManager) {
        return SelectableTargetParser.<C, AddonDescription>builder()
                .addRule(new AllSelectorRule<>(
                        () -> addonManager != null ? addonManager.getDiscoveredAddons() : List.of(),
                        "No addons discovered."
                ))
                .singleResolver((ctx, token) -> {
                    if (addonManager == null) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException("Addon manager is not initialized.")
                        );
                    }
                    Optional<AddonDescription> addonOpt = addonManager.getAddon(token);
                    if (addonOpt.isEmpty()) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException("Unknown addon: '" + token + "'")
                        );
                    }
                    return ArgumentParseResult.success(addonOpt.get());
                })
                .singleSuggestions(() -> addonManager == null
                        ? List.of()
                        : addonManager.getDiscoveredAddons().stream().map(AddonDescription::id).toList())
                .build();
    }

    /**
     * Builds a target parser for {@link RecipeDefinition} supporting {@code <recipeKey>},
     * {@code @all}, and {@code @<categoryKey>} (including unqualified {@code @<category>}).
     */
    public static <C> SelectableTargetParser<C, RecipeDefinition> recipeParser(
            @Nullable RecipeRegistry recipeRegistry,
            @Nullable RecipeCategoryRegistry categoryRegistry
    ) {
        SelectableTargetParser.Builder<C, RecipeDefinition> builder = SelectableTargetParser.<C, RecipeDefinition>builder()
                .addRule(new AllSelectorRule<>(
                        () -> recipeRegistry != null ? recipeRegistry.values() : List.of(),
                        "No recipes are currently registered."
                ));

        if (recipeRegistry != null) {
            builder.addRule(new RecipeCategorySelectorRule<>(recipeRegistry, categoryRegistry));
        }

        return builder
                .singleResolver((ctx, token) -> {
                    if (recipeRegistry == null) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException("Recipe registry is not initialized.")
                        );
                    }
                    Key key;
                    try {
                        key = Key.parse(token);
                    } catch (IllegalArgumentException e) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException("Invalid recipe key format: '" + token + "'")
                        );
                    }
                    Optional<RecipeDefinition> recipeOpt = recipeRegistry.get(key);
                    if (recipeOpt.isEmpty()) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException("Unknown recipe: '" + key.asString() + "'")
                        );
                    }
                    return ArgumentParseResult.success(recipeOpt.get());
                })
                .singleSuggestions(() -> recipeRegistry == null
                        ? List.of()
                        : recipeRegistry.values().stream().map(r -> r.key().asString()).toList())
                .build();
    }
}
