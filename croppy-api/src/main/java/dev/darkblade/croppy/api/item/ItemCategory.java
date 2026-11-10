package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable classification category for custom items.
 *
 * @param key           unique namespaced category key
 * @param iconMaterial  Minecraft material identifier (e.g., "minecraft:compass")
 * @param iconItemModel optional native 1.21.4+ item model identifier
 * @param displayName   MiniMessage formatted display name
 * @param priority      sorting order priority (lower values appear first)
 */
public record ItemCategory(
        Key key,
        String iconMaterial,
        Key iconItemModel,
        String displayName,
        int priority
) implements Keyed, Comparable<ItemCategory> {

    public ItemCategory {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(iconMaterial, "iconMaterial cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
    }

    public ItemCategory(Key key, String iconMaterial, String displayName, int priority) {
        this(key, iconMaterial, null, displayName, priority);
    }

    public static final ItemCategory ALL = new ItemCategory(
            Key.of("croppy", "all"),
            "minecraft:compass",
            null,
            "<gold>All Items</gold>",
            0
    );

    public static final ItemCategory SEEDS = new ItemCategory(
            Key.of("croppy", "seeds"),
            "minecraft:wheat_seeds",
            null,
            "<green>Seeds</green>",
            10
    );

    public static final ItemCategory FERTILIZERS = new ItemCategory(
            Key.of("croppy", "fertilizers"),
            "minecraft:bone_meal",
            null,
            "<aqua>Fertilizers</aqua>",
            20
    );

    public static final ItemCategory WATERING_CANS = new ItemCategory(
            Key.of("croppy", "watering_cans"),
            "minecraft:copper_ingot",
            Key.of("croppy", "copper_can"),
            "<blue>Watering Cans</blue>",
            30
    );

    public static final ItemCategory TOOLS = new ItemCategory(
            Key.of("croppy", "tools"),
            "minecraft:shears",
            Key.of("croppy", "inspector"),
            "<yellow>Tools</yellow>",
            40
    );

    public static final ItemCategory INGREDIENTS = new ItemCategory(
            Key.of("croppy", "ingredients"),
            "minecraft:apple",
            null,
            "<red>Ingredients</red>",
            50
    );

    public static final ItemCategory STATIONS = new ItemCategory(
            Key.of("croppy", "stations"),
            "minecraft:crafting_table",
            Key.of("croppy", "stove"),
            "<gold>Stations</gold>",
            60
    );

    public static final ItemCategory MISC = new ItemCategory(
            Key.of("croppy", "misc"),
            "minecraft:bundle",
            null,
            "<light_purple>Miscellaneous</light_purple>",
            70
    );

    public Optional<Key> getIconItemModel() {
        return Optional.ofNullable(iconItemModel);
    }

    @Override
    public Key getKey() {
        return key;
    }

    @Override
    public int compareTo(ItemCategory other) {
        int cmp = Integer.compare(this.priority, other.priority);
        if (cmp != 0) {
            return cmp;
        }
        return this.key.compareTo(other.key);
    }
}
