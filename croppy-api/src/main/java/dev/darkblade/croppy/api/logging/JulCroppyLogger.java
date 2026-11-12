package dev.darkblade.croppy.api.logging;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pure Java adapter translating {@link CroppyLogger} calls to a {@link java.util.logging.Logger}.
 */
public final class JulCroppyLogger implements CroppyLogger {

    private final Logger delegate;

    /**
     * Creates a JUL-backed {@link CroppyLogger}.
     *
     * @param delegate target {@link java.util.logging.Logger}
     */
    public JulCroppyLogger(Logger delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    /**
     * Converts a domain {@link LogLevel} to the corresponding {@link Level}.
     *
     * @param level domain severity level
     * @return mapped JUL severity level
     */
    public static Level toJulLevel(LogLevel level) {
        Objects.requireNonNull(level, "level cannot be null");
        return switch (level) {
            case TRACE -> Level.FINEST;
            case DEBUG -> Level.FINE;
            case INFO -> Level.INFO;
            case WARN -> Level.WARNING;
            case ERROR -> Level.SEVERE;
        };
    }

    /**
     * @return the underlying JUL logger
     */
    public Logger delegate() {
        return delegate;
    }

    @Override
    public boolean isEnabled(LogLevel level, LogChannel channel) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        return delegate.isLoggable(toJulLevel(level));
    }

    @Override
    public void log(LogLevel level, LogChannel channel, String message) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Level julLevel = toJulLevel(level);
        if (delegate.isLoggable(julLevel)) {
            delegate.log(julLevel, formatMessage(channel, message));
        }
    }

    @Override
    public void log(LogLevel level, LogChannel channel, String message, Throwable throwable) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(throwable, "throwable cannot be null");
        Level julLevel = toJulLevel(level);
        if (delegate.isLoggable(julLevel)) {
            delegate.log(julLevel, formatMessage(channel, message), throwable);
        }
    }

    private static String formatMessage(LogChannel channel, String message) {
        return channel == LogChannel.CORE ? message : "[" + channel.id() + "] " + message;
    }
}
