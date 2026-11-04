package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDisplayFormatterTest {

    private final ProcessDisplayFormatter formatter = new ProcessDisplayFormatter();

    @Test
    @DisplayName("Should replace direct and inverted progress placeholders accurately")
    void shouldReplaceProgressPlaceholders() {
        PhaseDisplay display = new PhaseDisplay(
                "{progress_percent}% - {progress_percent_left}% left | {progress_seconds}s - {progress_seconds_left}s left",
                10,
                "[#]",
                "[-]"
        );
        PhaseDefinition phase = new PhaseDefinition(
                "simmer",
                PhaseType.TIMED,
                200, // 10 seconds
                0,
                Expression.ALWAYS_TRUE,
                null,
                0,
                FailurePolicy.CANCEL,
                null,
                null,
                display,
                PhaseEffects.EMPTY,
                true,
                Map.of("name", "Simmering Pot")
        );

        DefaultProcessInstance instance = new DefaultProcessInstance(Key.of("croppy", "soup_cook"));
        instance.addPhaseElapsedTicks(50); // 25% (2.5 seconds, 7.5 seconds left = 8 seconds ceil)

        ProcessDefinition procDef = new ProcessDefinition(
                Key.of("croppy", "soup_cook"),
                "Tomato Soup",
                Key.of("croppy", "cooking_pot"),
                List.of(),
                Expression.ALWAYS_TRUE,
                List.of(phase),
                null,
                null,
                1,
                Map.of()
        );

        EvaluationContext context = EvaluationContext.empty().with("station.temperature", 85.5);

        String result = formatter.format(phase, procDef, instance, context);
        assertEquals("25% - 75% left | 2s - 8s left", result);
    }

    @Test
    @DisplayName("Should render progress bar with exact segment counts")
    void shouldRenderProgressBar() {
        PhaseDisplay display = new PhaseDisplay(
                "[{progress_bar}]",
                10,
                "F",
                "E"
        );
        PhaseDefinition phase = new PhaseDefinition(
                "boil",
                PhaseType.TIMED,
                100,
                0,
                Expression.ALWAYS_TRUE,
                null,
                0,
                FailurePolicy.CANCEL,
                null,
                null,
                display,
                PhaseEffects.EMPTY,
                false,
                Map.of()
        );

        DefaultProcessInstance instance = new DefaultProcessInstance(Key.of("croppy", "test"));
        instance.addPhaseElapsedTicks(60); // 60% = 6 filled, 4 empty

        String result = formatter.format(phase, null, instance, EvaluationContext.empty());
        assertEquals("[FFFFFFEEEE]", result);
    }

    @Test
    @DisplayName("Should resolve recipe and phase metadata placeholders")
    void shouldResolveMetadataPlaceholders() {
        PhaseDisplay display = new PhaseDisplay(
                "{recipe_name} - Phase {phase_index}/{phase_total} ({phase_name}) @ {temperature}°C",
                5,
                "X",
                "_"
        );
        PhaseDefinition phase1 = PhaseDefinition.timed("preheat", 100, Expression.ALWAYS_TRUE);
        PhaseDefinition phase2 = new PhaseDefinition(
                "fry",
                PhaseType.TIMED,
                100,
                0,
                Expression.ALWAYS_TRUE,
                null,
                0,
                FailurePolicy.CANCEL,
                null,
                null,
                display,
                PhaseEffects.EMPTY,
                false,
                Map.of("name", "Deep Frying")
        );

        ProcessDefinition procDef = new ProcessDefinition(
                Key.of("croppy", "fry_food"),
                "Fried Egg",
                Key.of("croppy", "skillet"),
                List.of(),
                Expression.ALWAYS_TRUE,
                List.of(phase1, phase2),
                null,
                null,
                1,
                Map.of()
        );

        DefaultProcessInstance instance = new DefaultProcessInstance(Key.of("croppy", "fry_food"));
        instance.setCurrentPhaseIndex(1); // 2nd phase (index 1)
        instance.addPhaseElapsedTicks(10);
        instance.setContextVariable("recipe_name", "Golden Fried Egg");

        EvaluationContext context = EvaluationContext.empty().with("station.temperature", 160.0);

        String result = formatter.format(phase2, procDef, instance, context);
        assertEquals("Golden Fried Egg - Phase 2/2 (Deep Frying) @ 160.0°C", result);
    }
}
