package dev.darkblade.croppy.core.interaction;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class YamlInteractionLoaderTest {

    private final YamlInteractionLoader loader = new YamlInteractionLoader();

    @Test
    @DisplayName("Parses complete interaction definition correctly")
    void testCompleteInteractionParsing() {
        String yaml = """
                id: "croppy:cooking_pot_gui"
                station: "croppy:cooking_pot"
                trigger: "SNEAK_RIGHT_CLICK"
                priority: 100
                conditions:
                  - "station.is_heated == true"
                actions:
                  - type: "OPEN_GUI"
                  - type: "PLAY_SOUND"
                    sound: "BLOCK_CHEST_OPEN"
                    pitch: 1.2
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        InteractionDefinition def = loader.load(in, "test.yml");

        assertEquals(Key.of("croppy", "cooking_pot_gui"), def.key());
        assertEquals(Key.of("croppy", "cooking_pot"), def.targetStation().orElseThrow());
        assertEquals(InteractionTrigger.SNEAK_RIGHT_CLICK, def.trigger());
        assertEquals(100, def.priority());
        assertFalse(def.requireEmptyHand());
        assertEquals(1, def.conditions().size());
        assertEquals("station.is_heated == true", def.conditions().get(0));

        assertEquals(2, def.actions().size());
        assertEquals("OPEN_GUI", def.actions().get(0).type());
        assertEquals("PLAY_SOUND", def.actions().get(1).type());
        assertEquals("BLOCK_CHEST_OPEN", def.actions().get(1).parameters().get("sound"));
    }

    @Test
    @DisplayName("Parses match item and material filters")
    void testMatchItemAndMaterial() {
        String yaml = """
                id: "croppy:measure_temp"
                station: "croppy:cooking_pot"
                trigger: "RIGHT_CLICK"
                match_item: "croppy:thermometer"
                actions:
                  - "SHOW_TEMPERATURE"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        InteractionDefinition def = loader.load(in, "test.yml");

        assertEquals(Key.of("croppy", "measure_temp"), def.key());
        assertEquals(Key.of("croppy", "thermometer"), def.matchItem().orElseThrow());
        assertTrue(def.matchMaterial().isEmpty());
        assertEquals(1, def.actions().size());
        assertEquals("SHOW_TEMPERATURE", def.actions().get(0).type());
    }

    @Test
    @DisplayName("Throws exception on missing ID")
    void testMissingIdThrows() {
        String yaml = """
                station: "croppy:cooking_pot"
                trigger: "RIGHT_CLICK"
                actions:
                  - "OPEN_GUI"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(in, "invalid.yml"));
    }
}
