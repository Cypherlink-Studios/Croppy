package dev.darkblade.croppy.api.process;

import java.util.Objects;

/**
 * Immutable display configuration for a process phase.
 *
 * @param template  MiniMessage template string with placeholders
 * @param barLength total character length of the visual progress bar
 * @param barFilled MiniMessage snippet or character for filled progress
 * @param barEmpty  MiniMessage snippet or character for empty progress
 */
public record PhaseDisplay(
        String template,
        int barLength,
        String barFilled,
        String barEmpty
) {

    public static final PhaseDisplay DEFAULT = new PhaseDisplay(
            "<gray>{phase_name}: <yellow>{progress_percent}%</yellow> ({progress_seconds_left}s)</gray>",
            10,
            "<green>■</green>",
            "<dark_gray>■</dark_gray>"
    );

    public PhaseDisplay {
        Objects.requireNonNull(template, "template cannot be null");
        barFilled = (barFilled == null || barFilled.isEmpty()) ? "<green>■</green>" : barFilled;
        barEmpty = (barEmpty == null || barEmpty.isEmpty()) ? "<dark_gray>■</dark_gray>" : barEmpty;
        if (barLength <= 0) {
            barLength = 10;
        }
    }

    public static PhaseDisplay of(String template) {
        return new PhaseDisplay(template, 10, "<green>■</green>", "<dark_gray>■</dark_gray>");
    }
}
