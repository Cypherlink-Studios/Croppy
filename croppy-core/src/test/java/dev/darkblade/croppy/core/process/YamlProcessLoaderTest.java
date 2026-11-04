package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class YamlProcessLoaderTest {

    private final YamlProcessLoader loader = new YamlProcessLoader();

    @Test
    @DisplayName("Should parse duration strings accurately to ticks")
    void shouldParseDurationStrings() {
        assertEquals(300L, YamlProcessLoader.parseDurationTicks("15s", 0));
        assertEquals(2400L, YamlProcessLoader.parseDurationTicks("2m", 0));
        assertEquals(100L, YamlProcessLoader.parseDurationTicks("100t", 0));
        assertEquals(50L, YamlProcessLoader.parseDurationTicks("50", 0));
        assertEquals(10L, YamlProcessLoader.parseDurationTicks(10, 0));
    }

    @Test
    @DisplayName("Should load single phase process with duration, display and effects")
    void shouldLoadSinglePhaseProcess() {
        String yaml = """
                id: "croppy:tomato_soup_cook"
                name: "Tomato Soup Cooking"
                station: "croppy:cooking_pot"
                start_conditions:
                  - "station.temperature >= 75"
                phases:
                  - id: "simmer"
                    name: "Simmering"
                    type: TIMED
                    duration: 15s
                    condition: "station.temperature >= 75"
                    pause_on_condition_failure: true
                    display:
                      template: "<gray>Cooking: {progress_percent}% [{progress_bar}] {progress_seconds_left}s</gray>"
                      bar:
                        length: 12
                        filled: "<green>■</green>"
                        empty: "<gray>■</gray>"
                    effects:
                      particles:
                        particle: "SPLASH"
                        count: 3
                        offset: [0.1, 0.2, 0.3]
                        speed: 0.05
                      sound:
                        sound: "BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE"
                        interval: 2s
                        volume: 0.5
                        pitch: 1.2
                """;

        ProcessDefinition def = loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "test.yml");
        assertEquals(Key.of("croppy", "tomato_soup_cook"), def.key());
        assertEquals("Tomato Soup Cooking", def.displayName());
        assertEquals(Key.of("croppy", "cooking_pot"), def.stationKey());
        assertEquals(1, def.phases().size());

        PhaseDefinition phase = def.phases().get(0);
        assertEquals("simmer", phase.id());
        assertEquals(PhaseType.TIMED, phase.type());
        assertEquals(300L, phase.durationTicks()); // 15s = 300 ticks
        assertTrue(phase.pauseOnConditionFailure());

        assertNotNull(phase.display());
        assertEquals("<gray>Cooking: {progress_percent}% [{progress_bar}] {progress_seconds_left}s</gray>", phase.display().template());
        assertEquals(12, phase.display().barLength());

        assertNotNull(phase.effects());
        assertTrue(phase.effects().hasParticle());
        assertEquals("SPLASH", phase.effects().particle().type());
        assertEquals(3, phase.effects().particle().count());

        assertTrue(phase.effects().hasSound());
        assertEquals("BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE", phase.effects().sound().sound());
        assertEquals(40, phase.effects().sound().intervalTicks()); // 2s = 40 ticks
    }

    @Test
    @DisplayName("Should load multi-phase interactive process")
    void shouldLoadMultiPhaseProcess() {
        String yaml = """
                id: "croppy:gourmet_dish"
                name: "Gourmet Dish"
                phases:
                  - id: "sear"
                    type: TIMED
                    duration: 10s
                  - id: "garnish"
                    type: MANUAL_INTERACTION
                    timeout: 8s
                    required_item: "croppy:parsley"
                    required_amount: 2
                    failure_policy: SPOIL
                    byproduct: "croppy:burnt_food"
                    prompt: "<yellow>Add 2 parsley!</yellow>"
                """;

        ProcessDefinition def = loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "test2.yml");
        assertEquals(2, def.phases().size());

        PhaseDefinition p1 = def.phases().get(0);
        assertEquals("sear", p1.id());
        assertEquals(200L, p1.durationTicks());

        PhaseDefinition p2 = def.phases().get(1);
        assertEquals("garnish", p2.id());
        assertEquals(PhaseType.MANUAL_INTERACTION, p2.type());
        assertEquals(160L, p2.timeoutTicks());
        assertEquals(Key.of("croppy", "parsley"), p2.requiredItem());
        assertEquals(2, p2.requiredAmount());
        assertEquals(FailurePolicy.SPOIL, p2.failurePolicy());
        assertEquals(Key.of("croppy", "burnt_food"), p2.byproductItem());
    }

    @Test
    @DisplayName("Should fail fast on missing ID")
    void shouldFailOnMissingId() {
        String yaml = """
                name: "No ID Process"
                phases:
                  - id: "step"
                """;
        assertThrows(NullPointerException.class, () ->
                loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "bad.yml")
        );
    }
}
