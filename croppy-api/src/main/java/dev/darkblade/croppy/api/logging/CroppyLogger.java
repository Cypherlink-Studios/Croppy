package dev.darkblade.croppy.api.logging;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Pure domain logging port decoupled from platform and JUL implementations.
 * <p>
 * Supports severity thresholds, subsystem {@link LogChannel} filtering, lazy message construction for
 * high-frequency debug/trace paths, and scoped child loggers.
 */
public interface CroppyLogger {

    /**
     * Returns whether an event at {@code level} on {@code channel} would currently be emitted.
     *
     * @param level severity level
     * @param channel subsystem channel
     * @return {@code true} if the message would be recorded
     */
    boolean isEnabled(LogLevel level, LogChannel channel);

    /**
     * Returns whether an event at {@code level} on {@link LogChannel#CORE} would currently be emitted.
     *
     * @param level severity level
     * @return {@code true} if the message would be recorded
     */
    default boolean isEnabled(LogLevel level) {
        return isEnabled(level, LogChannel.CORE);
    }

    /**
     * Logs a message at the specified severity level and channel.
     *
     * @param level severity level
     * @param channel subsystem channel
     * @param message message text
     */
    void log(LogLevel level, LogChannel channel, String message);

    /**
     * Logs a message with an associated cause at the specified severity level and channel.
     *
     * @param level severity level
     * @param channel subsystem channel
     * @param message message text
     * @param throwable associated exception or error
     */
    void log(LogLevel level, LogChannel channel, String message, Throwable throwable);

    /**
     * Logs a lazily constructed message when {@code level} and {@code channel} are enabled.
     *
     * @param level severity level
     * @param channel subsystem channel
     * @param messageSupplier lazy message supplier, invoked only when enabled
     */
    default void log(LogLevel level, LogChannel channel, Supplier<String> messageSupplier) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(messageSupplier, "messageSupplier cannot be null");
        if (isEnabled(level, channel)) {
            log(level, channel, messageSupplier.get());
        }
    }

    /**
     * Logs a lazily constructed message with an associated cause when {@code level} and {@code channel} are enabled.
     *
     * @param level severity level
     * @param channel subsystem channel
     * @param messageSupplier lazy message supplier, invoked only when enabled
     * @param throwable associated exception or error
     */
    default void log(LogLevel level, LogChannel channel, Supplier<String> messageSupplier, Throwable throwable) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(messageSupplier, "messageSupplier cannot be null");
        Objects.requireNonNull(throwable, "throwable cannot be null");
        if (isEnabled(level, channel)) {
            log(level, channel, messageSupplier.get(), throwable);
        }
    }

    /**
     * Creates a child logger that prefixes emitted messages with {@code [<scope>] }.
     *
     * @param scope non-blank scope identifier (for example {@code "Addon:coffee"})
     * @return scoped child logger delegating filtering and emission to this logger
     */
    default CroppyLogger scoped(String scope) {
        Objects.requireNonNull(scope, "scope cannot be null");
        String trimmed = scope.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
        String prefix = "[" + trimmed + "] ";
        CroppyLogger delegate = this;
        return new CroppyLogger() {
            @Override
            public boolean isEnabled(LogLevel level, LogChannel channel) {
                return delegate.isEnabled(level, channel);
            }

            @Override
            public void log(LogLevel level, LogChannel channel, String message) {
                Objects.requireNonNull(message, "message cannot be null");
                delegate.log(level, channel, prefix + message);
            }

            @Override
            public void log(LogLevel level, LogChannel channel, String message, Throwable throwable) {
                Objects.requireNonNull(message, "message cannot be null");
                delegate.log(level, channel, prefix + message, throwable);
            }
        };
    }

    // ---- Convenience TRACE methods ----

    default void trace(String message) {
        log(LogLevel.TRACE, LogChannel.CORE, message);
    }

    default void trace(Supplier<String> messageSupplier) {
        log(LogLevel.TRACE, LogChannel.CORE, messageSupplier);
    }

    default void trace(String message, Throwable throwable) {
        log(LogLevel.TRACE, LogChannel.CORE, message, throwable);
    }

    default void trace(LogChannel channel, String message) {
        log(LogLevel.TRACE, channel, message);
    }

    default void trace(LogChannel channel, Supplier<String> messageSupplier) {
        log(LogLevel.TRACE, channel, messageSupplier);
    }

    default void trace(LogChannel channel, String message, Throwable throwable) {
        log(LogLevel.TRACE, channel, message, throwable);
    }

    default void trace(LogChannel channel, Supplier<String> messageSupplier, Throwable throwable) {
        log(LogLevel.TRACE, channel, messageSupplier, throwable);
    }

    // ---- Convenience DEBUG methods ----

    default void debug(String message) {
        log(LogLevel.DEBUG, LogChannel.CORE, message);
    }

    default void debug(Supplier<String> messageSupplier) {
        log(LogLevel.DEBUG, LogChannel.CORE, messageSupplier);
    }

    default void debug(String message, Throwable throwable) {
        log(LogLevel.DEBUG, LogChannel.CORE, message, throwable);
    }

    default void debug(LogChannel channel, String message) {
        log(LogLevel.DEBUG, channel, message);
    }

    default void debug(LogChannel channel, Supplier<String> messageSupplier) {
        log(LogLevel.DEBUG, channel, messageSupplier);
    }

    default void debug(LogChannel channel, String message, Throwable throwable) {
        log(LogLevel.DEBUG, channel, message, throwable);
    }

    default void debug(LogChannel channel, Supplier<String> messageSupplier, Throwable throwable) {
        log(LogLevel.DEBUG, channel, messageSupplier, throwable);
    }

    // ---- Convenience INFO methods ----

    default void info(String message) {
        log(LogLevel.INFO, LogChannel.CORE, message);
    }

    default void info(Supplier<String> messageSupplier) {
        log(LogLevel.INFO, LogChannel.CORE, messageSupplier);
    }

    default void info(String message, Throwable throwable) {
        log(LogLevel.INFO, LogChannel.CORE, message, throwable);
    }

    default void info(LogChannel channel, String message) {
        log(LogLevel.INFO, channel, message);
    }

    default void info(LogChannel channel, Supplier<String> messageSupplier) {
        log(LogLevel.INFO, channel, messageSupplier);
    }

    default void info(LogChannel channel, String message, Throwable throwable) {
        log(LogLevel.INFO, channel, message, throwable);
    }

    // ---- Convenience WARN methods ----

    default void warn(String message) {
        log(LogLevel.WARN, LogChannel.CORE, message);
    }

    default void warn(Supplier<String> messageSupplier) {
        log(LogLevel.WARN, LogChannel.CORE, messageSupplier);
    }

    default void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, LogChannel.CORE, message, throwable);
    }

    default void warn(LogChannel channel, String message) {
        log(LogLevel.WARN, channel, message);
    }

    default void warn(LogChannel channel, Supplier<String> messageSupplier) {
        log(LogLevel.WARN, channel, messageSupplier);
    }

    default void warn(LogChannel channel, String message, Throwable throwable) {
        log(LogLevel.WARN, channel, message, throwable);
    }

    // ---- Convenience ERROR methods ----

    default void error(String message) {
        log(LogLevel.ERROR, LogChannel.CORE, message);
    }

    default void error(Supplier<String> messageSupplier) {
        log(LogLevel.ERROR, LogChannel.CORE, messageSupplier);
    }

    default void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, LogChannel.CORE, message, throwable);
    }

    default void error(LogChannel channel, String message) {
        log(LogLevel.ERROR, channel, message);
    }

    default void error(LogChannel channel, Supplier<String> messageSupplier) {
        log(LogLevel.ERROR, channel, messageSupplier);
    }

    default void error(LogChannel channel, String message, Throwable throwable) {
        log(LogLevel.ERROR, channel, message, throwable);
    }
}
