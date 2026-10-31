package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleSeverity;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
