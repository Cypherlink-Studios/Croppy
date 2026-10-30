package dev.darkblade.croppy.api.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition representing a custom item in Croppy.
 *
 * @param key unique namespaced identifier
 * @param category the category folder/name (e.g., seeds, fertilizers, watering_cans, tools)
 * @param materialKey Minecraft material identifier (e.g., "minecraft:wheat_seeds")
 * @param customModelData custom model data integer, or 0 if unassigned
 * @param displayName MiniMessage formatted display name
 * @param lore MiniMessage formatted lore lines
 * @param pdcProperties arbitrary persistent data key-value properties configured in YAML
 */
public record ItemDefinition(
        Key key,
        String category,
        String materialKey,
        int customModelData,
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

    @Override
    public Key getKey() {
        return key;
    }
}
