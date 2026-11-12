package dev.darkblade.croppy.api.logging;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Severity levels for Croppy domain and platform logging, ordered from lowest to highest priority.
 */
public enum LogLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    /**
     * @return the numeric severity rank used for threshold comparisons
     */
    public int severity() {
        return severity;
    }

    /**
     * Checks whether this level is at least as severe as the given minimum threshold.
     *
     * @param minimum the minimum threshold to compare against
     * @return {@code true} if {@code this.severity >= minimum.severity}
     */
    public boolean isAtLeast(LogLevel minimum) {
        Objects.requireNonNull(minimum, "minimum cannot be null");
        return this.severity >= minimum.severity;
    }

    /**
     * Checks whether an event at {@code level} should pass when this level is configured as the threshold.
     *
     * @param level the candidate event level
     * @return {@code true} if {@code level.isAtLeast(this)}
     */
    public boolean allows(LogLevel level) {
        Objects.requireNonNull(level, "level cannot be null");
        return level.severity >= this.severity;
    }

    /**
     * Resolves a {@link LogLevel} case-insensitively from configuration or command input.
     *
     * @param value raw level name
     * @return matching level, or {@link Optional#empty()} if unrecognized
     */
    public static Optional<LogLevel> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if ("WARNING".equals(normalized)) {
            return Optional.of(WARN);
        }
        for (LogLevel level : values()) {
            if (level.name().equals(normalized)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
