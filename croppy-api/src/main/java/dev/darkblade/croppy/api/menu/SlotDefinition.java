package dev.darkblade.croppy.api.menu;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable definition of an individual menu element or slot role.
 *
 * @param type            functional slot type
 * @param defaultTemplate base visual item template (may be null for dynamic or empty slots)
 * @param states          prioritized conditional templates evaluated against runtime context
 * @param actions         actions triggered when the slot is clicked
 * @param properties      arbitrary metadata properties
 */
public record SlotDefinition(
        SlotType type,
        MenuItemTemplate defaultTemplate,
        List<ConditionalTemplate> states,
        List<MenuAction> actions,
        Map<String, String> properties
) {

    public SlotDefinition {
        Objects.requireNonNull(type, "type cannot be null");
        states = (states == null) ? List.of() : List.copyOf(states);
        actions = (actions == null) ? List.of() : List.copyOf(actions);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }

    /**
     * Creates a simple decoration slot with a fixed item template.
     *
     * @param template visual template
     * @return constructed decoration slot
     */
    public static SlotDefinition decoration(MenuItemTemplate template) {
        return new SlotDefinition(SlotType.DECORATION, template, List.of(), List.of(), Map.of());
    }

    /**
     * Creates an input slot.
     *
     * @return constructed input slot
     */
    public static SlotDefinition input() {
        return new SlotDefinition(SlotType.INPUT, null, List.of(), List.of(), Map.of());
    }

    /**
     * Creates an output slot.
     *
     * @return constructed output slot
     */
    public static SlotDefinition output() {
        return new SlotDefinition(SlotType.OUTPUT, null, List.of(), List.of(), Map.of());
    }

    /**
     * Returns the optional default visual template.
     *
     * @return optional default template
     */
    public Optional<MenuItemTemplate> getDefaultTemplate() {
        return Optional.ofNullable(defaultTemplate);
    }
}
