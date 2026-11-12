package dev.darkblade.croppy.api.logging;

import java.util.Locale;
import java.util.Optional;

/**
 * Subsystem channels used to categorize and selectively enable verbose logs.
 */
public enum LogChannel {
    CORE("core"),
    LOADER("loader"),
    GROWTH("growth"),
    STATION("station"),
    DISPLAY("display"),
    PACKET("packet"),
    STORAGE("storage"),
    ADDON("addon"),
    PACK("pack"),
    INTEGRATION("integration"),
    UPDATE("update"),
    I18N("i18n"),
    COMMAND("command");

    private final String id;

    LogChannel(String id) {
        this.id = id;
    }

    /**
     * @return lowercase channel identifier for configuration and commands
     */
    public String id() {
        return id;
    }

    /**
     * Resolves a {@link LogChannel} case-insensitively from configuration or command arguments.
     *
     * @param value raw channel name
     * @return matching channel, or {@link Optional#empty()} if unknown
     */
    public static Optional<LogChannel> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        for (LogChannel channel : values()) {
            if (channel.name().equals(normalized)) {
                return Optional.of(channel);
            }
        }
        return Optional.empty();
    }
}
