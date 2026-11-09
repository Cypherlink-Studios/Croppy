package dev.darkblade.croppy.api.fertilizer;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Objects;

/**
 * Immutable domain definition of an enrichable soil fertilizer.
 *
 * @param key unique identifier of this fertilizer (e.g., croppy:growth_fertilizer)
 * @param name display name formatted with MiniMessage or color codes
 * @param description lore or descriptive text lines
 * @param type primary effect category
 * @param speedMultiplier growth point multiplier (for SPEED type, e.g. 1.25)
 * @param retentionChance probability of retaining daily hydration (for RETAINING type, 0.0 - 1.0)
 * @param qualityBoost chance bonus towards higher quality crops (for QUALITY type)
 * @param material Bukkit material name for the item representation (e.g. BONE_MEAL)
 * @param customModelData optional custom model data integer (0 if none)
 */
public record Fertilizer(
        Key key,
        String name,
        List<String> description,
        FertilizerType type,
        double speedMultiplier,
        double retentionChance,
        double qualityBoost,
        String material,
        int customModelData
) implements Keyed {

    public Fertilizer {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        description = description == null ? List.of() : List.copyOf(description);
        material = (material == null || material.isBlank()) ? "BONE_MEAL" : material;
    }

    @Override
    public Key getKey() {
        return key;
    }
}
