package dev.darkblade.croppy.core.station.visual;

import java.util.Locale;

/**
 * Pure Java utility for formatting station status MiniMessage strings using contextual placeholders.
 */
public final class StationPlaceholderFormatter {

    private StationPlaceholderFormatter() {}

    /**
     * Replaces placeholder tokens in the given template string.
     *
     * @param template       template string containing {tokens}
     * @param recipeName     raw recipe name or null
     * @param recipeDisplay  formatted recipe name or null
     * @param progress       progress fraction between 0.0f and 1.0f
     * @param temperature    current temperature in Celsius
     * @param thermalZone    thermal zone identifier or null
     * @param statusMessage  status or error message or null
     * @param remainingTicks remaining duration in ticks (or -1 if none)
     * @return populated string
     */
    public static String format(
            String template,
            String recipeName,
            String recipeDisplay,
            float progress,
            double temperature,
            String thermalZone,
            String statusMessage,
            long remainingTicks
    ) {
        return format(
                template,
                recipeName,
                recipeDisplay,
                progress,
                temperature,
                thermalZone,
                statusMessage,
                remainingTicks,
                -1L,
                null,
                1,
                1,
                null
        );
    }

    /**
     * Replaces placeholder tokens in the given template string with extended process and state metrics.
     *
     * @param template       template string containing {tokens}
     * @param recipeName     raw recipe name or null
     * @param recipeDisplay  formatted recipe name or null
     * @param progress       progress fraction between 0.0f and 1.0f
     * @param temperature    current temperature in Celsius
     * @param thermalZone    thermal zone identifier or null
     * @param statusMessage  status or error message or null
     * @param remainingTicks remaining duration in ticks (or -1 if none)
     * @param elapsedTicks   elapsed duration in ticks (or -1 if none)
     * @param phaseName      active phase name or null
     * @param phaseIndex     1-based active phase index
     * @param phaseTotal     total number of phases
     * @param state          station or process state string
     * @return populated string
     */
    public static String format(
            String template,
            String recipeName,
            String recipeDisplay,
            float progress,
            double temperature,
            String thermalZone,
            String statusMessage,
            long remainingTicks,
            long elapsedTicks,
            String phaseName,
            int phaseIndex,
            int phaseTotal,
            String state
    ) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int percent = Math.round(clampedProgress * 100.0f);
        int percentLeft = Math.max(0, 100 - percent);

        long validRemainingTicks = Math.max(0, remainingTicks);
        long validElapsedTicks = Math.max(0, elapsedTicks);

        long secondsLeft = (validRemainingTicks + 19) / 20;
        long secondsElapsed = validElapsedTicks / 20;

        String name = (recipeName == null || recipeName.isBlank()) ? "Idle" : recipeName;
        String display = (recipeDisplay == null || recipeDisplay.isBlank()) ? name : recipeDisplay;
        String zone = (thermalZone == null || thermalZone.isBlank()) ? "NONE" : thermalZone.toUpperCase(Locale.ROOT);
        String status = (statusMessage == null) ? "" : statusMessage;
        String phase = (phaseName == null || phaseName.isBlank()) ? "" : phaseName;
        String stateStr = (state == null || state.isBlank()) ? "IDLE" : state;

        String progressBar = buildProgressBar(clampedProgress, 10);
        String tempFormatted = String.format(Locale.ROOT, "%.1f°C", temperature);
        String tempPlain = String.format(Locale.ROOT, "%.1f", temperature);
        String remainingTimeStr = formatRemainingTime(remainingTicks);

        return template
                .replace("{recipe_name}", name)
                .replace("{recipe_display}", display)
                .replace("{progress_percent}", String.valueOf(percent))
                .replace("{progress_percent_left}", String.valueOf(percentLeft))
                .replace("{progress_seconds}", String.valueOf(secondsElapsed))
                .replace("{progress_seconds_left}", String.valueOf(secondsLeft))
                .replace("{progress_ticks}", String.valueOf(validElapsedTicks))
                .replace("{progress_ticks_left}", String.valueOf(validRemainingTicks))
                .replace("{progress_bar}", progressBar)
                .replace("{temperature_c}", tempFormatted)
                .replace("{temperature}", tempPlain)
                .replace("{thermal_zone}", zone)
                .replace("{status_message}", status)
                .replace("{time_remaining}", remainingTimeStr)
                .replace("{phase_name}", phase)
                .replace("{phase_index}", String.valueOf(Math.max(1, phaseIndex)))
                .replace("{phase_total}", String.valueOf(Math.max(1, phaseTotal)))
                .replace("{state}", stateStr);
    }

    /**
     * Constructs a stylized colored progress bar string using square blocks.
     *
     * @param progress  progress fraction between 0.0f and 1.0f
     * @param totalBars total number of bar segments
     * @return MiniMessage formatted progress bar string
     */
    public static String buildProgressBar(float progress, int totalBars) {
        int bars = Math.max(1, totalBars);
        int filled = Math.min(bars, Math.round(progress * bars));
        int empty = bars - filled;

        StringBuilder sb = new StringBuilder();
        if (filled > 0) {
            sb.append("<green>").append("■".repeat(filled)).append("</green>");
        }
        if (empty > 0) {
            sb.append("<gray>").append("□".repeat(empty)).append("</gray>");
        }
        return sb.toString();
    }

    /**
     * Formats server tick duration into human-readable seconds or minutes.
     *
     * @param ticks server ticks
     * @return formatted duration string (e.g. "15s", "1m 30s")
     */
    public static String formatRemainingTime(long ticks) {
        if (ticks <= 0) {
            return "0s";
        }
        long seconds = (ticks + 19) / 20;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remSec = seconds % 60;
        return minutes + "m " + remSec + "s";
    }
}
