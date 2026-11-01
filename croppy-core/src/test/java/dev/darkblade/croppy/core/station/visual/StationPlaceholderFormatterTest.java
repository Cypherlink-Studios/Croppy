package dev.darkblade.croppy.core.station.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationPlaceholderFormatterTest {

    @Test
    void testFormatAllPlaceholders() {
        String template = "<yellow>{recipe_name}</yellow> [{progress_percent}%] {progress_bar} Temp: {temperature_c} ({thermal_zone}) Status: {status_message} Remaining: {time_remaining}";

        String result = StationPlaceholderFormatter.format(
                template,
                "Tomato Soup",
                "<red>Tomato Soup</red>",
                0.6f,
                85.42,
                "SIMMER",
                "Stirring required",
                200L // 10 seconds
        );

        assertTrue(result.contains("Tomato Soup"));
        assertTrue(result.contains("60%"));
        assertTrue(result.contains("<green>■■■■■■</green><gray>□□□□</gray>"));
        assertTrue(result.contains("85.4°C"));
        assertTrue(result.contains("SIMMER"));
        assertTrue(result.contains("Stirring required"));
        assertTrue(result.contains("10s"));
    }

    @Test
    void testIdleDefaultsWhenNullOrBlank() {
        String template = "{recipe_name} | {recipe_display} | {progress_percent}% | {thermal_zone} | {status_message}";

        String result = StationPlaceholderFormatter.format(
                template,
                null,
                null,
                0.0f,
                20.0,
                null,
                null,
                0L
        );

        assertEquals("Idle | Idle | 0% | NONE | ", result);
    }

    @Test
    void testProgressBarBoundaries() {
        assertEquals("<gray>□□□□□</gray>", StationPlaceholderFormatter.buildProgressBar(0.0f, 5));
        assertEquals("<green>■■■■■</green>", StationPlaceholderFormatter.buildProgressBar(1.0f, 5));
        assertEquals("<green>■■■</green><gray>□□</gray>", StationPlaceholderFormatter.buildProgressBar(0.6f, 5));
    }

    @Test
    void testFormatRemainingTime() {
        assertEquals("0s", StationPlaceholderFormatter.formatRemainingTime(0));
        assertEquals("1s", StationPlaceholderFormatter.formatRemainingTime(15));
        assertEquals("5s", StationPlaceholderFormatter.formatRemainingTime(100));
        assertEquals("1m 15s", StationPlaceholderFormatter.formatRemainingTime(1500));
    }
}
