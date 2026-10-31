package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleSeverity;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRuleRegistryTest {

    @Test
    @DisplayName("Rules are filtered by trigger and sorted by priority descending")
    void testFilteringAndSorting() {
        DefaultRuleRegistry registry = new DefaultRuleRegistry();

        RuleDefinition lowPriTick = new RuleDefinition(
                Key.of("croppy", "smoke_tick"),
                "Smoke Tick",
                RuleTrigger.STATION_TICK,
                RuleSeverity.INFO,
                10,
                List.of(),
                List.of(RuleAction.of("EMIT_PARTICLES"))
        );

        RuleDefinition highPriTick = new RuleDefinition(
                Key.of("croppy", "overcooking_burn"),
                "Overcooking Burn",
                RuleTrigger.STATION_TICK,
                RuleSeverity.CRITICAL,
                100,
                List.of("station.temperature > 150"),
                List.of(RuleAction.of("REPLACE_OUTPUT"))
        );

        RuleDefinition interactRule = new RuleDefinition(
                Key.of("croppy", "burn_injury"),
                "Burn Injury",
                RuleTrigger.STATION_INTERACT,
                RuleSeverity.WARNING,
                50,
                List.of("station.temperature > 200"),
                List.of(RuleAction.of("DAMAGE_PLAYER"))
        );

        registry.register(lowPriTick);
        registry.register(highPriTick);
        registry.register(interactRule);

        List<RuleDefinition> tickRules = registry.getRulesByTrigger(RuleTrigger.STATION_TICK);
        assertEquals(2, tickRules.size());
        assertEquals("croppy:overcooking_burn", tickRules.get(0).key().asString()); // priority 100
        assertEquals("croppy:smoke_tick", tickRules.get(1).key().asString()); // priority 10

        List<RuleDefinition> interactRules = registry.getRulesByTrigger(RuleTrigger.STATION_INTERACT);
        assertEquals(1, interactRules.size());
        assertEquals("croppy:burn_injury", interactRules.get(0).key().asString());

        List<RuleDefinition> emptyRules = registry.getRulesByTrigger(RuleTrigger.PROCESS_FINISH);
        assertTrue(emptyRules.isEmpty());
    }
}
