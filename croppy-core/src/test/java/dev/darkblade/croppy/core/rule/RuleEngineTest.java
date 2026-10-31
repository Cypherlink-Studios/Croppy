package dev.darkblade.croppy.core.rule;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleDefinition;
import dev.darkblade.croppy.api.rule.RuleSeverity;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {

    private DefaultRuleRegistry ruleRegistry;
    private RuleEngine ruleEngine;
    private DefaultStationInstance station;
    private DefaultStorageComponent storage;

    @BeforeEach
    void setUp() {
        ruleRegistry = new DefaultRuleRegistry();
        ruleEngine = new RuleEngine(ruleRegistry);

        Key potKey = Key.of("croppy", "cooking_pot");
        StationDefinition def = new StationDefinition(
                potKey,
                "Cooking Pot",
                "minecraft:player_head",
                StationInteractionType.HYBRID,
                6,
                2,
                true,
                List.of(),
                Map.of()
        );
        station = new DefaultStationInstance(potKey, new BlockPosition("world", 0, 64, 0), def);
        storage = new DefaultStorageComponent(6, 2);
        station.setComponent(StorageComponent.class, storage);
    }

    @Test
    @DisplayName("Overcooking burn rule replaces outputs with burnt food")
    void testOvercookingBurnRule() {
        // Place finished meal in outputs
        storage.addOutput(StoredItem.of(Key.of("croppy", "beef_stew"), 1, 3));

        RuleDefinition burnRule = new RuleDefinition(
                Key.of("croppy", "overcooking_burn"),
                "Overcooking Burn",
                RuleTrigger.STATION_TICK,
                RuleSeverity.CRITICAL,
                100,
                List.of("station.temperature > 120", "process.idle_ticks > 200"),
                List.of(
                        RuleAction.of("REPLACE_OUTPUT", Map.of("target_item", "croppy:burnt_food")),
                        RuleAction.of("PLAY_SOUND", Map.of("sound", "ENTITY_GENERIC_BURN"))
                )
        );
        ruleRegistry.register(burnRule);

        // When conditions are not met
        EvaluationContext notMet = EvaluationContext.empty()
                .with("station.temperature", 100.0)
                .with("process.idle_ticks", 50);

        List<RuleExecution> noExec = ruleEngine.evaluateAndApply(RuleTrigger.STATION_TICK, station, notMet);
        assertTrue(noExec.isEmpty());
        assertEquals(Key.of("croppy", "beef_stew"), storage.getOutputs().get(0).itemKey());

        // When conditions are met
        EvaluationContext met = EvaluationContext.empty()
                .with("station.temperature", 150.0)
                .with("process.idle_ticks", 250);

        List<RuleExecution> exec = ruleEngine.evaluateAndApply(RuleTrigger.STATION_TICK, station, met);
        assertEquals(1, exec.size());
        assertEquals("croppy:overcooking_burn", exec.get(0).rule().key().asString());

        // Outputs should now be replaced with burnt food
        assertEquals(1, storage.getOutputs().size());
        assertEquals(Key.of("croppy", "burnt_food"), storage.getOutputs().get(0).itemKey());
        assertEquals(0, storage.getOutputs().get(0).quality());
    }

    @Test
    @DisplayName("Quality degradation rule drops output quality without destroying item")
    void testQualityDegradationRule() {
        storage.addOutput(StoredItem.of(Key.of("croppy", "soup"), 1, 3));

        RuleDefinition degradeRule = new RuleDefinition(
                Key.of("croppy", "stale_food"),
                "Stale Food",
                RuleTrigger.STATION_TICK,
                RuleSeverity.PENALTY,
                50,
                List.of("process.idle_ticks > 100"),
                List.of(RuleAction.of("REDUCE_QUALITY", Map.of("amount", 2)))
        );
        ruleRegistry.register(degradeRule);

        EvaluationContext ctx = EvaluationContext.empty().with("process.idle_ticks", 150);
        ruleEngine.evaluateAndApply(RuleTrigger.STATION_TICK, station, ctx);

        assertEquals(1, storage.getOutputs().size());
        assertEquals(Key.of("croppy", "soup"), storage.getOutputs().get(0).itemKey());
        assertEquals(1, storage.getOutputs().get(0).quality()); // 3 - 2 = 1
    }
}
