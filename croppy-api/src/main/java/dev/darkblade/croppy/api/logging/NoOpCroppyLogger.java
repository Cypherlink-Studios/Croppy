package dev.darkblade.croppy.api.logging;

import java.util.Objects;

/**
 * Silent {@link CroppyLogger} implementation that discards all log messages without invoking lazy suppliers.
 */
public final class NoOpCroppyLogger implements CroppyLogger {

    /**
     * Shared singleton instance.
     */
    public static final NoOpCroppyLogger INSTANCE = new NoOpCroppyLogger();

    public NoOpCroppyLogger() {
    }

    /**
     * @return shared {@link NoOpCroppyLogger} singleton
     */
    public static NoOpCroppyLogger instance() {
        return INSTANCE;
    }

    @Override
    public boolean isEnabled(LogLevel level, LogChannel channel) {
        return false;
    }

    @Override
    public void log(LogLevel level, LogChannel channel, String message) {
        // Intentionally no-op
    }

    @Override
    public void log(LogLevel level, LogChannel channel, String message, Throwable throwable) {
        // Intentionally no-op
    }

    @Override
    public CroppyLogger scoped(String scope) {
        Objects.requireNonNull(scope, "scope cannot be null");
        if (scope.trim().isEmpty()) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
        return this;
    }
}
