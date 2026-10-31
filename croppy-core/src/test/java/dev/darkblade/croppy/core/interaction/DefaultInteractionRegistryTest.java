package dev.darkblade.croppy.core.interaction;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionAction;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultInteractionRegistryTest {

    @Test
    @DisplayName("Interactions are filtered by station and sorted by priority descending")
    void testFilteringAndSorting() {
        DefaultInteractionRegistry registry = new DefaultInteractionRegistry();

        Key potKey = Key.of("croppy", "cooking_pot");
        Key skilletKey = Key.of("croppy", "skillet");

        InteractionDefinition lowPriPot = new InteractionDefinition(
                Key.of("croppy", "pot_insert"),
                Optional.of(potKey),
                InteractionTrigger.RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("INSERT_INPUT")),
                10
        );

        InteractionDefinition highPriPot = new InteractionDefinition(
                Key.of("croppy", "pot_gui"),
                Optional.of(potKey),
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("OPEN_GUI")),
                100
        );

        InteractionDefinition globalThermometer = new InteractionDefinition(
                Key.of("croppy", "thermometer_check"),
                Optional.empty(), // Global
                InteractionTrigger.RIGHT_CLICK,
                Optional.of(Key.of("croppy", "thermometer")),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("SHOW_TEMPERATURE")),
                50
        );

        InteractionDefinition skilletOnly = new InteractionDefinition(
                Key.of("croppy", "skillet_flip"),
                Optional.of(skilletKey),
                InteractionTrigger.RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("FLIP")),
                20
        );

        registry.register(lowPriPot);
        registry.register(highPriPot);
        registry.register(globalThermometer);
        registry.register(skilletOnly);

        // Fetch for cooking pot
        List<InteractionDefinition> potList = registry.getInteractionsForStation(potKey);
        assertEquals(3, potList.size());
        assertEquals("croppy:pot_gui", potList.get(0).key().asString()); // priority 100
        assertEquals("croppy:thermometer_check", potList.get(1).key().asString()); // priority 50
        assertEquals("croppy:pot_insert", potList.get(2).key().asString()); // priority 10

        // Fetch for skillet
        List<InteractionDefinition> skilletList = registry.getInteractionsForStation(skilletKey);
        assertEquals(2, skilletList.size());
        assertEquals("croppy:thermometer_check", skilletList.get(0).key().asString()); // priority 50
        assertEquals("croppy:skillet_flip", skilletList.get(1).key().asString()); // priority 20
    }
}
