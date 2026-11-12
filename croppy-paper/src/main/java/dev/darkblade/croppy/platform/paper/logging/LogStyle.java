package dev.darkblade.croppy.platform.paper.logging;

import java.util.Locale;
import java.util.Optional;

/**
 * Console presentation style for lifecycle summaries and diagnostics.
 */
public enum LogStyle {
    /**
     * Multi-line framed banner and aligned phase table using MiniMessage styling.
     */
    RICH,
    /**
     * Single-line {@code [Croppy]} log summaries without box formatting.
     */
    COMPACT;

    /**
     * Resolves a {@link LogStyle} case-insensitively from configuration input.
     *
     * @param value raw style string
     * @return matching style, or {@link Optional#empty()} if unrecognized
     */
    public static Optional<LogStyle> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        for (LogStyle style : values()) {
            if (style.name().equals(normalized)) {
                return Optional.of(style);
            }
        }
        return Optional.empty();
    }
}
