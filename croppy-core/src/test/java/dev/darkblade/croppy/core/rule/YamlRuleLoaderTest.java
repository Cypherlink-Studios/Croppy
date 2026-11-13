package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleSeverity;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.rule.StationScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlRuleLoaderTest {

    private final YamlRuleLoader loader = new YamlRuleLoader();

    @Test
    @DisplayName("Parses valid rule definition and pre-compiles conditions successfully")
    void testCompleteRuleParsing() {
        String yaml = """
                id: "croppy:overcooking_burn"
                name: "Overcooking Burn"
                trigger: "STATION_TICK"
                severity: "CRITICAL"
                priority: 100
                conditions:
                  - "process.finished == true and station.temperature > 120"
                actions:
                  - type: "REPLACE_OUTPUT"
                    target_item: "croppy:burnt_food"
                  - type: "PLAY_SOUND"
                    sound: "ENTITY_GENERIC_BURN"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        RuleDefinition rule = loader.load(in, "test.yml");

        assertEquals(Key.of("croppy", "overcooking_burn"), rule.key());
        assertEquals("Overcooking Burn", rule.name());
        assertEquals(RuleTrigger.STATION_TICK, rule.trigger());
        assertEquals(RuleSeverity.CRITICAL, rule.severity());
        assertEquals(100, rule.priority());
        assertEquals(1, rule.conditions().size());
        assertEquals(2, rule.actions().size());
        assertEquals("REPLACE_OUTPUT", rule.actions().get(0).type());
        assertEquals("croppy:burnt_food", rule.actions().get(0).parameters().get("target_item"));
        assertTrue(rule.stations().matches(Key.of("croppy", "cooking_pot")));
    }

    @Test
    @DisplayName("Parses stations shorthand list filter")
    void testStationsShorthandListParsing() {
        String yaml = """
                id: "croppy:tight_display"
                name: "Tight Display"
                trigger: "DISPLAY_LAYOUT"
                stations:
                  - "croppy:cooking_pot"
                  - "skillet"
                conditions:
                  - "display.headroom < 0.85"
                actions:
                  - type: "TEXT_DISPLAY_FIT"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        RuleDefinition rule = loader.load(in, "layout.yml");

        assertEquals(RuleTrigger.DISPLAY_LAYOUT, rule.trigger());
        assertEquals(StationScope.Mode.INCLUDE, rule.stations().mode());
        assertEquals(Set.of(Key.of("croppy", "cooking_pot"), Key.of("croppy", "skillet")), rule.stations().stationKeys());
        assertTrue(rule.stations().matches(Key.of("croppy", "cooking_pot")));
        assertTrue(rule.stations().matches(Key.of("croppy", "skillet")));
        assertFalse(rule.stations().matches(Key.of("croppy", "stove")));
    }

    @Test
    @DisplayName("Parses stations mapping with EXCLUDE mode")
    void testStationsExcludeMappingParsing() {
        String yaml = """
                id: "croppy:exclude_stove"
                trigger: "STATION_INTERACT"
                stations:
                  mode: EXCLUDE
                  ids:
                    - "croppy:stove"
                actions:
                  - type: "DAMAGE_PLAYER"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        RuleDefinition rule = loader.load(in, "exclude.yml");

        assertEquals(StationScope.Mode.EXCLUDE, rule.stations().mode());
        assertEquals(Set.of(Key.of("croppy", "stove")), rule.stations().stationKeys());
        assertFalse(rule.stations().matches(Key.of("croppy", "stove")));
        assertTrue(rule.stations().matches(Key.of("croppy", "cooking_pot")));
    }

    @Test
    @DisplayName("Fails fast on malformed condition expression")
    void testMalformedConditionFails() {
        String yaml = """
                id: "croppy:bad_condition"
                trigger: "STATION_TICK"
                conditions:
                  - "== station.temp +"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(in, "invalid.yml"));
    }
}
