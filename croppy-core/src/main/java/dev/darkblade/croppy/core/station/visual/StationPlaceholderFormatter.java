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
        if (template == null || template.isEmpty()) {
            return "";
        }

        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int percent = Math.round(clampedProgress * 100.0f);
        String name = (recipeName == null || recipeName.isBlank()) ? "Idle" : recipeName;
        String display = (recipeDisplay == null || recipeDisplay.isBlank()) ? name : recipeDisplay;
        String zone = (thermalZone == null || thermalZone.isBlank()) ? "NONE" : thermalZone.toUpperCase(Locale.ROOT);
        String status = (statusMessage == null) ? "" : statusMessage;

        String progressBar = buildProgressBar(clampedProgress, 10);
        String tempStr = String.format(Locale.ROOT, "%.1f°C", temperature);
        String remainingTimeStr = formatRemainingTime(remainingTicks);

        return template
                .replace("{recipe_name}", name)
                .replace("{recipe_display}", display)
                .replace("{progress_percent}", String.valueOf(percent))
                .replace("{progress_bar}", progressBar)
                .replace("{temperature_c}", tempStr)
                .replace("{thermal_zone}", zone)
                .replace("{status_message}", status)
                .replace("{time_remaining}", remainingTimeStr);
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
