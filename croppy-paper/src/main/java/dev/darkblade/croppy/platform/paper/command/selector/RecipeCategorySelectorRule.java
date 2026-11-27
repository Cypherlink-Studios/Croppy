package dev.darkblade.croppy.platform.paper.command.selector;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ArgumentParseResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@link SelectorRule} resolving {@code @<categoryKey>} and unqualified {@code @<category>}
 * selectors into matching {@link RecipeDefinition} entries from {@link RecipeRegistry}.
 *
 * @param <C> command sender context type
 */
public final class RecipeCategorySelectorRule<C> implements SelectorRule<C, RecipeDefinition> {

    private final RecipeRegistry recipeRegistry;
    private final @Nullable RecipeCategoryRegistry categoryRegistry;

    public RecipeCategorySelectorRule(
            RecipeRegistry recipeRegistry,
            @Nullable RecipeCategoryRegistry categoryRegistry
    ) {
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "recipeRegistry cannot be null");
        this.categoryRegistry = categoryRegistry;
    }

    @Override
    public boolean matches(@NonNull String token) {
        return token.startsWith("@")
                && token.length() > 1
                && !AllSelectorRule.TOKEN.equalsIgnoreCase(token);
    }

    @Override
    public @NonNull ArgumentParseResult<List<RecipeDefinition>> resolve(
            @NonNull CommandContext<C> context,
            @NonNull String token
    ) {
        String rawCategory = token.substring(1);
        Key categoryKey;
        try {
            categoryKey = Key.parse(rawCategory);
        } catch (IllegalArgumentException e) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("Invalid recipe category selector: '" + token + "'")
            );
        }

        if (RecipeCategory.ALL.getKey().equals(categoryKey)) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("Unknown recipe category selector: '" + token + "'. Use '@all' instead.")
            );
        }

        if (categoryRegistry != null && !categoryRegistry.contains(categoryKey)) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("Unknown recipe category: '" + categoryKey.asString() + "'")
            );
        }

        List<RecipeDefinition> matched = new ArrayList<>();
        for (RecipeDefinition recipe : recipeRegistry.values()) {
            if (recipe.categoryKey().equals(categoryKey)) {
                matched.add(recipe);
            }
        }

        if (matched.isEmpty()) {
            return ArgumentParseResult.failure(
                    new IllegalArgumentException("No recipes registered in category: '" + categoryKey.asString() + "'")
            );
        }
        return ArgumentParseResult.success(List.copyOf(matched));
    }

    @Override
    public @NonNull List<String> suggestions(@NonNull CommandContext<C> context, @NonNull String remaining) {
        Set<String> candidates = new LinkedHashSet<>();
        if (categoryRegistry != null) {
            for (RecipeCategory category : categoryRegistry.values()) {
                addCategorySuggestions(candidates, category.getKey());
            }
        } else {
            for (RecipeDefinition recipe : recipeRegistry.values()) {
                addCategorySuggestions(candidates, recipe.categoryKey());
            }
        }
        return List.copyOf(candidates);
    }

    private static void addCategorySuggestions(Set<String> candidates, Key key) {
        if (key == null || RecipeCategory.ALL.getKey().equals(key)) {
            return;
        }
        candidates.add("@" + key.asString());
        if (Key.DEFAULT_NAMESPACE.equals(key.namespace())) {
            candidates.add("@" + key.value());
        }
    }
}
