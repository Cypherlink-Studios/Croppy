package dev.darkblade.croppy.platform.paper.logging;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Paper implementation of {@link CroppyLogger} backed by Kyori {@link ComponentLogger}.
 * <p>
 * Dynamic message payloads are always wrapped via {@link Component#text(String)} to prevent
 * MiniMessage injection from exception messages, file paths, or external addon output.
 */
public final class PaperCroppyLogger implements CroppyLogger {

    /**
     * Sink abstraction decoupling formatted component emission for headless unit testing.
     */
    @FunctionalInterface
    public interface ComponentSink {
        void emit(LogLevel level, Component message, Throwable throwable);
    }

    private final ComponentSink sink;
    private final Object stateLock = new Object();
    private volatile LogLevel minimumLevel = LogLevel.INFO;
    private volatile Set<LogChannel> debugChannels = Set.of();

    private final AtomicInteger warningCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();

    /**
     * Creates a logger writing formatted components to the given Paper {@link ComponentLogger}.
     *
     * @param componentLogger Paper plugin component logger
     */
    public PaperCroppyLogger(ComponentLogger componentLogger) {
        this(new ComponentLoggerSink(Objects.requireNonNull(componentLogger, "componentLogger cannot be null")));
    }

    /**
     * Creates a logger backed by a custom {@link ComponentSink} (primarily for unit tests).
     *
     * @param sink formatted log sink
     */
    public PaperCroppyLogger(ComponentSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink cannot be null");
    }

    /**
     * Applies configuration values from {@code config.yml}, resetting any runtime debug channel overrides.
     *
     * @param level              configured base severity threshold
     * @param configuredChannels configured channels promoted to {@link LogLevel#DEBUG}
     */
    public void applyConfig(LogLevel level, Set<LogChannel> configuredChannels) {
        Objects.requireNonNull(level, "level cannot be null");
        synchronized (stateLock) {
            this.minimumLevel = level;
            if (configuredChannels == null || configuredChannels.isEmpty()) {
                this.debugChannels = Set.of();
            } else {
                EnumSet<LogChannel> copy = EnumSet.copyOf(configuredChannels);
                this.debugChannels = Collections.unmodifiableSet(copy);
            }
        }
    }

    public LogLevel getMinimumLevel() {
        return minimumLevel;
    }

    public Set<LogChannel> getDebugChannels() {
        return debugChannels;
    }

    /**
     * Promotes {@code channel} to {@link LogLevel#DEBUG} at runtime.
     *
     * @return {@code true} if the channel was newly added
     */
    public boolean enableDebugChannel(LogChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");
        synchronized (stateLock) {
            EnumSet<LogChannel> updated = copyCurrentChannels();
            boolean changed = updated.add(channel);
            if (changed) {
                this.debugChannels = Collections.unmodifiableSet(updated);
            }
            return changed;
        }
    }

    /**
     * Removes runtime {@link LogLevel#DEBUG} promotion for {@code channel}.
     *
     * @return {@code true} if the channel was active and is now removed
     */
    public boolean disableDebugChannel(LogChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");
        synchronized (stateLock) {
            EnumSet<LogChannel> updated = copyCurrentChannels();
            boolean changed = updated.remove(channel);
            if (changed) {
                this.debugChannels = updated.isEmpty() ? Set.of() : Collections.unmodifiableSet(updated);
            }
            return changed;
        }
    }

    /**
     * Toggles runtime {@link LogLevel#DEBUG} promotion for {@code channel}.
     *
     * @return {@code true} if the channel is enabled after toggling, {@code false} otherwise
     */
    public boolean toggleDebugChannel(LogChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");
        synchronized (stateLock) {
            EnumSet<LogChannel> updated = copyCurrentChannels();
            boolean nowEnabled;
            if (updated.contains(channel)) {
                updated.remove(channel);
                nowEnabled = false;
            } else {
                updated.add(channel);
                nowEnabled = true;
            }
            this.debugChannels = updated.isEmpty() ? Set.of() : Collections.unmodifiableSet(updated);
            return nowEnabled;
        }
    }

    /**
     * Enables {@link LogLevel#DEBUG} output across every {@link LogChannel}.
     */
    public void enableAllDebugChannels() {
        synchronized (stateLock) {
            this.debugChannels = Collections.unmodifiableSet(EnumSet.allOf(LogChannel.class));
        }
    }

    /**
     * Clears all active debug channel promotions.
     */
    public void disableAllDebugChannels() {
        synchronized (stateLock) {
            this.debugChannels = Set.of();
        }
    }

    public void resetCounters() {
        warningCount.set(0);
        errorCount.set(0);
    }

    public int getWarningCount() {
        return warningCount.get();
    }

    public int getErrorCount() {
        return errorCount.get();
    }

    @Override
    public boolean isEnabled(LogLevel level, LogChannel channel) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        if (this.minimumLevel.allows(level)) {
            return true;
        }
        return level.isAtLeast(LogLevel.DEBUG) && this.debugChannels.contains(channel);
    }

    @Override
    public void log(LogLevel level, LogChannel channel, String message) {
        emit(level, channel, message, null);
    }

    @Override
    public void log(LogLevel level, LogChannel channel, String message, Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        emit(level, channel, message, throwable);
    }

    private void emit(LogLevel level, LogChannel channel, String message, Throwable throwable) {
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        if (!isEnabled(level, channel)) {
            return;
        }
        if (level == LogLevel.WARN) {
            warningCount.incrementAndGet();
        } else if (level == LogLevel.ERROR) {
            errorCount.incrementAndGet();
        }
        sink.emit(level, formatComponent(level, channel, message), throwable);
    }

    private EnumSet<LogChannel> copyCurrentChannels() {
        Set<LogChannel> current = this.debugChannels;
        return current.isEmpty() ? EnumSet.noneOf(LogChannel.class) : EnumSet.copyOf(current);
    }

    static Component formatComponent(LogLevel level, LogChannel channel, String message) {
        TextComponent.Builder builder = Component.text();
        switch (level) {
            case TRACE -> builder
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("TRACE", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(channel.id(), NamedTextColor.GRAY))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(message, NamedTextColor.GRAY));
            case DEBUG -> builder
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("DEBUG", NamedTextColor.AQUA))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(channel.id(), NamedTextColor.GRAY))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(message, NamedTextColor.GRAY));
            case INFO -> {
                if (channel != LogChannel.CORE) {
                    builder
                            .append(Component.text("[", NamedTextColor.DARK_GRAY))
                            .append(Component.text(channel.id(), NamedTextColor.GRAY))
                            .append(Component.text("] ", NamedTextColor.DARK_GRAY));
                }
                builder.append(Component.text(message));
            }
            case WARN -> {
                if (channel != LogChannel.CORE) {
                    builder
                            .append(Component.text("[", NamedTextColor.DARK_GRAY))
                            .append(Component.text(channel.id(), NamedTextColor.YELLOW))
                            .append(Component.text("] ", NamedTextColor.DARK_GRAY));
                }
                builder.append(Component.text(message, NamedTextColor.YELLOW));
            }
            case ERROR -> {
                if (channel != LogChannel.CORE) {
                    builder
                            .append(Component.text("[", NamedTextColor.DARK_GRAY))
                            .append(Component.text(channel.id(), NamedTextColor.RED))
                            .append(Component.text("] ", NamedTextColor.DARK_GRAY));
                }
                builder.append(Component.text(message, NamedTextColor.RED));
            }
        }
        return builder.build();
    }

    private record ComponentLoggerSink(ComponentLogger logger) implements ComponentSink {
        @Override
        public void emit(LogLevel level, Component message, Throwable throwable) {
            // Note: DEBUG and TRACE route via ComponentLogger.info once approved by Croppy's own gate,
            // because Paper's default log4j2 configuration suppresses SLF4J debug/trace events.
            switch (level) {
                case ERROR -> {
                    if (throwable != null) {
                        logger.error(message, throwable);
                    } else {
                        logger.error(message);
                    }
                }
                case WARN -> {
                    if (throwable != null) {
                        logger.warn(message, throwable);
                    } else {
                        logger.warn(message);
                    }
                }
                case INFO, DEBUG, TRACE -> {
                    if (throwable != null) {
                        logger.info(message, throwable);
                    } else {
                        logger.info(message);
                    }
                }
            }
        }
    }
}
