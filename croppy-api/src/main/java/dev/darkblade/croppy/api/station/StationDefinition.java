package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a crafting station or machine type.
 *
 * @param key              unique namespaced identifier (e.g., "croppy:keg", "croppy:cooking_pot")
 * @param displayName      MiniMessage formatted display name
 * @param baseBlockMaterial Minecraft material identifier (e.g., "minecraft:barrel", "minecraft:cauldron")
 * @param interactionType  supported player interaction style
 * @param inputCapacity    maximum concurrent input items/slots
 * @param outputCapacity   maximum concurrent output items/slots
 * @param hasDisplay       whether station renders floating items/text display entities
 * @param allowedRecipes   list of allowed process recipe keys, or empty for all
 * @param properties       arbitrary configuration properties
 */
public record StationDefinition(
        Key key,
        String displayName,
        String baseBlockMaterial,
        StationInteractionType interactionType,
        int inputCapacity,
        int outputCapacity,
        boolean hasDisplay,
        List<Key> allowedRecipes,
        Map<String, String> properties
) implements Keyed {

    public StationDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(baseBlockMaterial, "baseBlockMaterial cannot be null");
        Objects.requireNonNull(interactionType, "interactionType cannot be null");
        allowedRecipes = (allowedRecipes == null) ? List.of() : List.copyOf(allowedRecipes);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }

    @Override
    public Key getKey() {
        return key;
    }
}
