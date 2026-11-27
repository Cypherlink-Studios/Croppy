package dev.darkblade.croppy.core.consumable;

import dev.darkblade.croppy.api.consumable.ConsumeEffectHandler;
import dev.darkblade.croppy.api.consumable.ConsumeExecutionContext;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ConsumableProperties;
import dev.darkblade.croppy.api.item.ConsumeEffectSpec;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.UseAnimation;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsumptionEvaluatorTest {

    @Test
    void testMinQualityChanceConditionAndMissingHandlerIsolation() {
        DefaultConsumeEffectRegistry registry = new DefaultConsumeEffectRegistry();
        List<String> executed = new ArrayList<>();

        registry.register(new ConsumeEffectHandler() {
            @Override
            public Key getKey() {
                return Key.of("croppy", "record");
            }

            @Override
            public void apply(ConsumeExecutionContext context, Map<String, Object> parameters) {
                executed.add(String.valueOf(parameters.get("tag")));
            }
        });
        registry.register(new ConsumeEffectHandler() {
            @Override
            public Key getKey() {
                return Key.of("croppy", "boom");
            }

            @Override
            public void apply(ConsumeExecutionContext context, Map<String, Object> parameters) {
                throw new RuntimeException("boom");
            }
        });

        ConsumableProperties props = new ConsumableProperties(
                null, 1.6f, UseAnimation.EAT, null, true, null, null,
                List.of(
                        new ConsumeEffectSpec(Key.of("croppy", "record"), 1.0, 0, null, Map.of("tag", "always")),
                        new ConsumeEffectSpec(Key.of("croppy", "record"), 1.0, 3, null, Map.of("tag", "iridium_only")),
                        new ConsumeEffectSpec(Key.of("croppy", "record"), 0.5, 0, null, Map.of("tag", "lucky")),
                        new ConsumeEffectSpec(Key.of("croppy", "record"), 1.0, 0, "item.quality >= 2", Map.of("tag", "cond_pass")),
                        new ConsumeEffectSpec(Key.of("croppy", "missing"), 1.0, 0, null, Map.of()),
                        new ConsumeEffectSpec(Key.of("croppy", "boom"), 1.0, 0, null, Map.of()),
                        new ConsumeEffectSpec(Key.of("croppy", "record"), 1.0, 0, null, Map.of("tag", "after_error"))
                )
        );

        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "test_food"), "ingredients", "APPLE", 0,
                null, null, "Test Food", List.of(), Map.of(), props
        );

        // Random returns 0.25 (< 0.5 -> passes lucky)
        Random fixedRandom = new Random() {
            @Override
            public double nextDouble() {
                return 0.25;
            }
        };

        ConsumptionEvaluator evaluator = new ConsumptionEvaluator(registry, NoOpCroppyLogger.INSTANCE, () -> fixedRandom);
        ConsumeExecutionContext ctx = new SimpleConsumeExecutionContext(
                UUID.randomUUID(), "Tester", def, null, 2,
                EvaluationContext.of(Map.of("item.quality", 2)), null
        );

        int count = evaluator.execute(props, ctx);
        assertEquals(4, count);
        assertEquals(List.of("always", "lucky", "cond_pass", "after_error"), executed);
    }
}
