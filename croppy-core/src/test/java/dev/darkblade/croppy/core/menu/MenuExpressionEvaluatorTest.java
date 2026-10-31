package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.menu.ConditionalTemplate;
import dev.darkblade.croppy.api.menu.MenuAction;
import dev.darkblade.croppy.api.menu.MenuActionType;
import dev.darkblade.croppy.api.menu.MenuItemTemplate;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MenuExpressionEvaluatorTest {

    @Test
    @DisplayName("evaluateCondition evaluates boolean expressions accurately")
    void testEvaluateCondition() {
        EvaluationContext ctx = EvaluationContext.empty()
                .with("process.state", "RUNNING")
                .with("heat", 120);

        assertTrue(MenuExpressionEvaluator.evaluateCondition("process.state == 'RUNNING'", ctx));
        assertFalse(MenuExpressionEvaluator.evaluateCondition("process.state == 'IDLE'", ctx));
        assertTrue(MenuExpressionEvaluator.evaluateCondition("heat >= 100", ctx));
        assertFalse(MenuExpressionEvaluator.evaluateCondition("heat > 200", ctx));

        // Empty / null condition is always true
        assertTrue(MenuExpressionEvaluator.evaluateCondition(null, ctx));
        assertTrue(MenuExpressionEvaluator.evaluateCondition("  ", ctx));

        // Invalid expression safely returns false
        assertFalse(MenuExpressionEvaluator.evaluateCondition("invalid +++ syntax", ctx));
    }

    @Test
    @DisplayName("resolveTemplate prioritizes matching states and falls back to default")
    void testResolveTemplate() {
        MenuItemTemplate defaultItem = MenuItemTemplate.simple("minecraft:compass", "Idle");
        MenuItemTemplate runningItem = MenuItemTemplate.simple("minecraft:soul_campfire", "Cooking");
        MenuItemTemplate finishedItem = MenuItemTemplate.simple("minecraft:bell", "Done");

        SlotDefinition slot = new SlotDefinition(
                SlotType.STATUS,
                defaultItem,
                List.of(
                        new ConditionalTemplate("process.state == 'RUNNING'", runningItem),
                        new ConditionalTemplate("process.state == 'COMPLETED'", finishedItem)
                ),
                List.of(),
                Map.of()
        );

        // State: RUNNING
        EvaluationContext ctxRunning = EvaluationContext.empty().with("process.state", "RUNNING");
        var resolvedRunning = MenuExpressionEvaluator.resolveTemplate(slot, ctxRunning);
        assertTrue(resolvedRunning.isPresent());
        assertSame(runningItem, resolvedRunning.get());

        // State: COMPLETED
        EvaluationContext ctxCompleted = EvaluationContext.empty().with("process.state", "COMPLETED");
        var resolvedCompleted = MenuExpressionEvaluator.resolveTemplate(slot, ctxCompleted);
        assertTrue(resolvedCompleted.isPresent());
        assertSame(finishedItem, resolvedCompleted.get());

        // State: IDLE -> fallback to default
        EvaluationContext ctxIdle = EvaluationContext.empty().with("process.state", "IDLE");
        var resolvedDefault = MenuExpressionEvaluator.resolveTemplate(slot, ctxIdle);
        assertTrue(resolvedDefault.isPresent());
        assertSame(defaultItem, resolvedDefault.get());
    }

    @Test
    @DisplayName("isActionAllowed validates condition gates")
    void testIsActionAllowed() {
        MenuAction unconditional = MenuAction.of(MenuActionType.CLOSE, "close");
        MenuAction gated = new MenuAction(
                null,
                MenuActionType.STATION_ACTION,
                "START_PROCESS",
                "station.has_fuel == true"
        );

        EvaluationContext withFuel = EvaluationContext.empty().with("station.has_fuel", true);
        EvaluationContext withoutFuel = EvaluationContext.empty().with("station.has_fuel", false);

        assertTrue(MenuExpressionEvaluator.isActionAllowed(unconditional, withoutFuel));
        assertTrue(MenuExpressionEvaluator.isActionAllowed(gated, withFuel));
        assertFalse(MenuExpressionEvaluator.isActionAllowed(gated, withoutFuel));
    }
}
