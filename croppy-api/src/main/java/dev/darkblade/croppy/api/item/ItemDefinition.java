package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable definition representing a custom item in Croppy.
 *
 * @param key unique namespaced identifier
 * @param category the category folder/name (e.g., seeds, fertilizers, watering_cans, tools)
 * @param materialKey Minecraft material identifier (e.g., "minecraft:wheat_seeds")
 * @param customModelData custom model data integer, or 0 if unassigned
 * @param itemModel optional native Minecraft 1.21.4+ item model identifier (e.g., "croppy:copper_can")
 * @param skullTexture optional base64 string or Mojang URL texture for player_head items
 * @param displayName MiniMessage formatted display name
 * @param lore MiniMessage formatted lore lines
 * @param pdcProperties arbitrary persistent data key-value properties configured in YAML
 */
public record ItemDefinition(
        Key key,
        String category,
        String materialKey,
        int customModelData,
        Key itemModel,
        String skullTexture,
        String displayName,
        List<String> lore,
        Map<String, String> pdcProperties
) implements Keyed {

    public ItemDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(materialKey, "materialKey cannot be null");
        lore = (lore == null) ? List.of() : List.copyOf(lore);
        pdcProperties = (pdcProperties == null) ? Map.of() : Map.copyOf(pdcProperties);
    }

    /**
     * Backward-compatible constructor without {@code skullTexture}.
     */
    public ItemDefinition(
            Key key,
            String category,
            String materialKey,
            int customModelData,
            Key itemModel,
            String displayName,
            List<String> lore,
            Map<String, String> pdcProperties
    ) {
        this(key, category, materialKey, customModelData, itemModel, null, displayName, lore, pdcProperties);
    }

    /**
     * Backward-compatible constructor without native {@code itemModel} and {@code skullTexture}.
     */
    public ItemDefinition(
            Key key,
            String category,
            String materialKey,
            int customModelData,
            String displayName,
            List<String> lore,
            Map<String, String> pdcProperties
    ) {
        this(key, category, materialKey, customModelData, null, null, displayName, lore, pdcProperties);
    }

    /**
     * Gets the optional native 1.21.4+ item model key.
     *
     * @return Optional containing the item model Key, or empty if not configured
     */
    public Optional<Key> getItemModel() {
        return Optional.ofNullable(itemModel);
    }

    /**
     * Gets the optional skull texture base64 string or Mojang URL.
     *
     * @return Optional containing the skull texture, or empty if not configured
     */
    public Optional<String> getSkullTexture() {
        return Optional.ofNullable(skullTexture);
    }

    @Override
    public Key getKey() {
        return key;
    }
}
