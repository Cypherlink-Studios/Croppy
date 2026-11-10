package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;
import dev.darkblade.croppy.api.species.RotationMode;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable definition of a crafting station or machine type.
 *
 * @param key               unique namespaced identifier (e.g., "croppy:keg", "croppy:cooking_pot")
 * @param displayName       MiniMessage formatted display name
 * @param baseBlockMaterial Minecraft material identifier (e.g., "minecraft:barrel", "minecraft:cauldron")
 * @param interactionType   supported player interaction style
 * @param inputCapacity     maximum concurrent input items/slots
 * @param outputCapacity    maximum concurrent output items/slots
 * @param menuKey           key of the bound {@link dev.darkblade.croppy.api.menu.MenuDefinition}
 * @param hasDisplay        whether station renders floating items/text display entities
 * @param allowedRecipes    list of allowed process recipe keys, or empty for all
 * @param properties        arbitrary configuration properties
 * @param visualDefinition  optional visual layers and packet display configuration
 * @param rotationMode      spatial placement rotation mode (defaults to CARDINAL_4)
 * @param states            declarative state definitions with condition-driven overrides
 */
public record StationDefinition(
        Key key,
        String displayName,
        String baseBlockMaterial,
        StationInteractionType interactionType,
        int inputCapacity,
        int outputCapacity,
        Key menuKey,
        boolean hasDisplay,
        List<Key> allowedRecipes,
        Map<String, String> properties,
        StationVisualDefinition visualDefinition,
        RotationMode rotationMode,
        List<StationStateDefinition> states
) implements Keyed {

    public static final Key DEFAULT_MENU_KEY = Key.of("croppy", "default_station");

    public StationDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(baseBlockMaterial, "baseBlockMaterial cannot be null");
        Objects.requireNonNull(interactionType, "interactionType cannot be null");
        menuKey = (menuKey == null) ? DEFAULT_MENU_KEY : menuKey;
        allowedRecipes = (allowedRecipes == null) ? List.of() : List.copyOf(allowedRecipes);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
        rotationMode = (rotationMode == null) ? RotationMode.CARDINAL_4 : rotationMode;
        states = (states == null) ? List.of() : List.copyOf(states);
    }

    /**
     * Backward-compatible constructor defaulting {@code states} to empty list.
     */
    public StationDefinition(
            Key key,
            String displayName,
            String baseBlockMaterial,
            StationInteractionType interactionType,
            int inputCapacity,
            int outputCapacity,
            Key menuKey,
            boolean hasDisplay,
            List<Key> allowedRecipes,
            Map<String, String> properties,
            StationVisualDefinition visualDefinition,
            RotationMode rotationMode
    ) {
        this(key, displayName, baseBlockMaterial, interactionType, inputCapacity, outputCapacity, menuKey, hasDisplay, allowedRecipes, properties, visualDefinition, rotationMode, List.of());
    }

    /**
     * Backward-compatible constructor defaulting {@code rotationMode} to {@link RotationMode#CARDINAL_4} and {@code states} to empty.
     */
    public StationDefinition(
            Key key,
            String displayName,
            String baseBlockMaterial,
            StationInteractionType interactionType,
            int inputCapacity,
            int outputCapacity,
            Key menuKey,
            boolean hasDisplay,
            List<Key> allowedRecipes,
            Map<String, String> properties,
            StationVisualDefinition visualDefinition
    ) {
        this(key, displayName, baseBlockMaterial, interactionType, inputCapacity, outputCapacity, menuKey, hasDisplay, allowedRecipes, properties, visualDefinition, RotationMode.CARDINAL_4, List.of());
    }

    /**
     * Backward-compatible constructor defaulting {@code visualDefinition} to null and {@code states} to empty.
     */
    public StationDefinition(
            Key key,
            String displayName,
            String baseBlockMaterial,
            StationInteractionType interactionType,
            int inputCapacity,
            int outputCapacity,
            Key menuKey,
            boolean hasDisplay,
            List<Key> allowedRecipes,
            Map<String, String> properties
    ) {
        this(key, displayName, baseBlockMaterial, interactionType, inputCapacity, outputCapacity, menuKey, hasDisplay, allowedRecipes, properties, null, RotationMode.CARDINAL_4, List.of());
    }

    /**
     * Backward-compatible constructor defaulting {@code menuKey} to {@link #DEFAULT_MENU_KEY}, {@code visualDefinition} to null, and {@code states} to empty.
     */
    public StationDefinition(
            Key key,
            String displayName,
            String baseBlockMaterial,
            StationInteractionType interactionType,
            int inputCapacity,
            int outputCapacity,
            boolean hasDisplay,
            List<Key> allowedRecipes,
            Map<String, String> properties
    ) {
        this(key, displayName, baseBlockMaterial, interactionType, inputCapacity, outputCapacity, DEFAULT_MENU_KEY, hasDisplay, allowedRecipes, properties, null, RotationMode.CARDINAL_4, List.of());
    }

    @Override
    public Key getKey() {
        return key;
    }

    public Optional<StationVisualDefinition> getVisualDefinition() {
        return Optional.ofNullable(visualDefinition);
    }
}
