package dev.darkblade.croppy.api.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Objects;

/**
 * Immutable classification category for culinary recipes.
 *
 * @param key         unique namespaced category key
 * @param displayName MiniMessage formatted display name
 * @param iconItemKey display item icon key in menu listings
 */
public record RecipeCategory(
        Key key,
        String displayName,
        Key iconItemKey
) implements Keyed {

    public RecipeCategory {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
    }

    public static final RecipeCategory ALL = new RecipeCategory(
            Key.of("croppy", "all"),
            "<gold>All Recipes</gold>",
            Key.of("minecraft", "book")
    );

    public static final RecipeCategory BAKERY = new RecipeCategory(
            Key.of("croppy", "bakery"),
            "<yellow>Bakery</yellow>",
            Key.of("minecraft", "bread")
    );

    public static final RecipeCategory SOUPS = new RecipeCategory(
            Key.of("croppy", "soups"),
            "<aqua>Soups & Stews</aqua>",
            Key.of("minecraft", "bowl")
    );

    public static final RecipeCategory COOKING = new RecipeCategory(
            Key.of("croppy", "cooking"),
            "<red>Cooking & Roasts</red>",
            Key.of("minecraft", "cooked_beef")
    );

    public static final RecipeCategory BEVERAGES = new RecipeCategory(
            Key.of("croppy", "beverages"),
            "<green>Beverages</green>",
            Key.of("minecraft", "potion")
    );

    public static final RecipeCategory PRESERVES = new RecipeCategory(
            Key.of("croppy", "preserves"),
            "<light_purple>Preserves & Jars</light_purple>",
            Key.of("minecraft", "glass_bottle")
    );

    @Override
    public Key getKey() {
        return key;
    }
}
