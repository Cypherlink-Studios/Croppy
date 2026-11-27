package dev.darkblade.croppy.platform.paper.command.selector;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.core.addon.DefaultAddonManager;
import dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.suggestion.Suggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class SelectableTargetParserTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Recipe target parser resolves @all, @category, @ns:category, and single keys")
    void testRecipeTargetParser() {
        DefaultRecipeRegistry recipeRegistry = new DefaultRecipeRegistry();
        DefaultRecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();

        Key customCat = Key.of("custom", "desserts");
        categoryRegistry.register(new RecipeCategory(customCat, "Desserts", Key.of("croppy", "apple")));

        RecipeDefinition soup = recipe("croppy:tomato_soup", RecipeCategory.SOUPS.getKey());
        RecipeDefinition stew = recipe("croppy:veg_stew", RecipeCategory.SOUPS.getKey());
        RecipeDefinition cake = recipe("custom:berry_cake", customCat);

        recipeRegistry.register(soup);
        recipeRegistry.register(stew);
        recipeRegistry.register(cake);

        SelectableTargetParser<Object, RecipeDefinition> parser =
                CroppyTargetSelectors.recipeParser(recipeRegistry, categoryRegistry);
        CommandContext<Object> ctx = null;

        // 1. @all selects every recipe
        TargetSelection<RecipeDefinition> all = parser.parse(ctx, CommandInput.of("@all")).parsedValue().orElseThrow();
        assertTrue(all.selector());
        assertEquals("@all", all.selectorToken().orElseThrow());
        assertEquals(3, all.size());

        // 2. @soups shorthand resolves croppy:soups
        TargetSelection<RecipeDefinition> soups = parser.parse(ctx, CommandInput.of("@soups")).parsedValue().orElseThrow();
        assertTrue(soups.selector());
        assertEquals(2, soups.size());
        assertTrue(soups.targets().containsAll(List.of(soup, stew)));

        // 3. @custom:desserts resolves namespaced category
        TargetSelection<RecipeDefinition> desserts = parser.parse(ctx, CommandInput.of("@custom:desserts")).parsedValue().orElseThrow();
        assertEquals(List.of(cake), desserts.targets());

        // 4. Single recipe key without '@'
        TargetSelection<RecipeDefinition> single = parser.parse(ctx, CommandInput.of("croppy:tomato_soup")).parsedValue().orElseThrow();
        assertFalse(single.selector());
        assertEquals(List.of(soup), single.targets());

        // 5. Unknown @-selector fails instead of falling back to single key parsing
        ArgumentParseResult<TargetSelection<RecipeDefinition>> badCat = parser.parse(ctx, CommandInput.of("@unknown_cat"));
        assertTrue(badCat.failure().isPresent());

        // 6. @croppy:all is rejected in favor of @all
        ArgumentParseResult<TargetSelection<RecipeDefinition>> explicitAllCat = parser.parse(ctx, CommandInput.of("@croppy:all"));
        assertTrue(explicitAllCat.failure().isPresent());

        // 7. Empty category fails cleanly
        ArgumentParseResult<TargetSelection<RecipeDefinition>> emptyCat = parser.parse(ctx, CommandInput.of("@bakery"));
        assertTrue(emptyCat.failure().isPresent());

        // 8. Suggestions include @all, category selectors (excluding @croppy:all), and recipe keys
        List<String> suggestions = StreamSupport.stream(
                        parser.suggestionsFuture(ctx, CommandInput.empty()).join().spliterator(), false)
                .map(Suggestion::suggestion)
                .toList();
        assertTrue(suggestions.contains("@all"));
        assertTrue(suggestions.contains("@soups"));
        assertTrue(suggestions.contains("@croppy:soups"));
        assertTrue(suggestions.contains("@custom:desserts"));
        assertFalse(suggestions.contains("@croppy:all"));
        assertTrue(suggestions.contains("croppy:tomato_soup"));
    }

    @Test
    @DisplayName("Addon target parser resolves @all, single addon ID, and rejects unprefixed all")
    void testAddonTargetParser() {
        DefaultAddonManager manager = new DefaultAddonManager(tempDir, NoOpCroppyLogger.INSTANCE);
        AddonDescription a = new AddonDescription("orchard", "Orchard", "1.0", "orchard", Optional.empty(), List.of(), "", List.of(), List.of());
        AddonDescription b = new AddonDescription("apiary", "Apiary", "1.0", "apiary", Optional.empty(), List.of(), "", List.of(), List.of());
        manager.registerDiscovered(a, tempDir.resolve("orchard"));
        manager.registerDiscovered(b, tempDir.resolve("apiary"));

        SelectableTargetParser<Object, AddonDescription> parser = CroppyTargetSelectors.addonParser(manager);
        CommandContext<Object> ctx = null;

        TargetSelection<AddonDescription> all = parser.parse(ctx, CommandInput.of("@all")).parsedValue().orElseThrow();
        assertTrue(all.selector());
        assertEquals(2, all.size());

        TargetSelection<AddonDescription> single = parser.parse(ctx, CommandInput.of("orchard")).parsedValue().orElseThrow();
        assertFalse(single.selector());
        assertEquals(List.of(a), single.targets());

        // Unprefixed 'all' is not treated as a wildcard
        assertTrue(parser.parse(ctx, CommandInput.of("all")).failure().isPresent());

        // Unknown @-selector fails
        assertTrue(parser.parse(ctx, CommandInput.of("@unknown")).failure().isPresent());

        List<String> suggestions = StreamSupport.stream(
                        parser.suggestionsFuture(ctx, CommandInput.empty()).join().spliterator(), false)
                .map(Suggestion::suggestion)
                .toList();
        assertTrue(suggestions.contains("@all"));
        assertTrue(suggestions.contains("orchard"));
        assertTrue(suggestions.contains("apiary"));
        assertFalse(suggestions.contains("all"));
    }

    private static RecipeDefinition recipe(String key, Key categoryKey) {
        return new RecipeDefinition(
                Key.parse(key),
                key,
                categoryKey,
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "process"),
                List.of(),
                Key.of("croppy", "item"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
    }
}
