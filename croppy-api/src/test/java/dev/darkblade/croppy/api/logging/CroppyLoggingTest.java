package dev.darkblade.croppy.api.logging;

import dev.darkblade.croppy.api.addon.AddonContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CroppyLoggingTest {

    @Test
    void logLevelOrderingAndResolutionWork() {
        assertTrue(LogLevel.ERROR.isAtLeast(LogLevel.TRACE));
        assertTrue(LogLevel.INFO.isAtLeast(LogLevel.INFO));
        assertFalse(LogLevel.DEBUG.isAtLeast(LogLevel.INFO));

        assertTrue(LogLevel.INFO.allows(LogLevel.WARN));
        assertTrue(LogLevel.INFO.allows(LogLevel.INFO));
        assertFalse(LogLevel.INFO.allows(LogLevel.DEBUG));

        assertEquals(Optional.of(LogLevel.TRACE), LogLevel.fromString(" trace "));
        assertEquals(Optional.of(LogLevel.DEBUG), LogLevel.fromString("DEBUG"));
        assertEquals(Optional.of(LogLevel.INFO), LogLevel.fromString("Info"));
        assertEquals(Optional.of(LogLevel.WARN), LogLevel.fromString("warn"));
        assertEquals(Optional.of(LogLevel.WARN), LogLevel.fromString("WARNING"));
        assertEquals(Optional.of(LogLevel.ERROR), LogLevel.fromString("error"));
        assertEquals(Optional.empty(), LogLevel.fromString(null));
        assertEquals(Optional.empty(), LogLevel.fromString("   "));
        assertEquals(Optional.empty(), LogLevel.fromString("verbose"));
    }

    @Test
    void logChannelCaseInsensitiveResolutionCoversAllChannels() {
        assertEquals(13, LogChannel.values().length);
        for (LogChannel channel : LogChannel.values()) {
            assertEquals(Optional.of(channel), LogChannel.fromString(" " + channel.id().toUpperCase() + " "));
            assertEquals(Optional.of(channel), LogChannel.fromString(channel.id()));
        }
        assertEquals(Optional.empty(), LogChannel.fromString(null));
        assertEquals(Optional.empty(), LogChannel.fromString(""));
        assertEquals(Optional.empty(), LogChannel.fromString("unknown_channel"));
    }

    @Test
    void noOpLoggerNeverInvokesLazySuppliers() {
        CroppyLogger logger = NoOpCroppyLogger.instance();
        AtomicInteger calls = new AtomicInteger();

        for (LogLevel level : LogLevel.values()) {
            for (LogChannel channel : LogChannel.values()) {
                assertFalse(logger.isEnabled(level, channel));
                logger.log(level, channel, () -> {
                    calls.incrementAndGet();
                    return "expensive";
                });
            }
        }
        logger.trace(() -> {
            calls.incrementAndGet();
            return "t";
        });
        logger.debug(() -> {
            calls.incrementAndGet();
            return "d";
        });
        logger.debug(LogChannel.LOADER, () -> {
            calls.incrementAndGet();
            return "dl";
        }, new RuntimeException("ignored"));

        assertEquals(0, calls.get());
        assertSame(NoOpCroppyLogger.INSTANCE, logger.scoped("Addon:test"));
        assertThrows(IllegalArgumentException.class, () -> logger.scoped("   "));
    }

    @Test
    void scopedLoggerAndLazySuppliersDelegateProperly() {
        RecordingLogger root = new RecordingLogger(LogLevel.DEBUG);
        AtomicInteger traceCalls = new AtomicInteger();
        AtomicInteger debugCalls = new AtomicInteger();

        root.trace(LogChannel.PACKET, () -> {
            traceCalls.incrementAndGet();
            return "skipped";
        });
        root.debug(LogChannel.LOADER, () -> {
            debugCalls.incrementAndGet();
            return "loaded foo";
        });

        assertEquals(0, traceCalls.get());
        assertEquals(1, debugCalls.get());
        assertEquals(1, root.entries.size());
        assertEquals("[DEBUG][LOADER] loaded foo", root.entries.get(0));

        CroppyLogger scoped = root.scoped("Addon:coffee");
        CroppyLogger nested = scoped.scoped("Recipes");
        scoped.info(LogChannel.ADDON, "Ready");
        nested.warn(LogChannel.LOADER, "Duplicate key", new IllegalStateException("dup"));

        assertEquals("[INFO][ADDON] [Addon:coffee] Ready", root.entries.get(1));
        assertEquals("[WARN][LOADER] [Addon:coffee] [Recipes] Duplicate key (dup)", root.entries.get(2));
        assertThrows(IllegalArgumentException.class, () -> root.scoped(""));
    }

    @Test
    void julCroppyLoggerMapsLevelsAndThrowablesAccurately() {
        Logger jul = Logger.getLogger("croppy.test.jul." + System.nanoTime());
        jul.setUseParentHandlers(false);
        jul.setLevel(Level.ALL);
        List<LogRecord> records = new ArrayList<>();
        jul.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        JulCroppyLogger adapter = new JulCroppyLogger(jul);
        IllegalStateException boom = new IllegalStateException("boom");

        adapter.trace(LogChannel.STATION, "trace-msg");
        adapter.debug(LogChannel.LOADER, "debug-msg");
        adapter.info("info-msg");
        adapter.warn(LogChannel.PACK, "warn-msg");
        adapter.error(LogChannel.STORAGE, "error-msg", boom);

        assertEquals(5, records.size());
        assertEquals(Level.FINEST, records.get(0).getLevel());
        assertEquals("[station] trace-msg", records.get(0).getMessage());
        assertEquals(Level.FINE, records.get(1).getLevel());
        assertEquals("[loader] debug-msg", records.get(1).getMessage());
        assertEquals(Level.INFO, records.get(2).getLevel());
        assertEquals("info-msg", records.get(2).getMessage());
        assertEquals(Level.WARNING, records.get(3).getLevel());
        assertEquals("[pack] warn-msg", records.get(3).getMessage());
        assertEquals(Level.SEVERE, records.get(4).getLevel());
        assertEquals("[storage] error-msg", records.get(4).getMessage());
        assertSame(boom, records.get(4).getThrown());

        jul.setLevel(Level.WARNING);
        assertFalse(adapter.isEnabled(LogLevel.INFO, LogChannel.CORE));
        assertTrue(adapter.isEnabled(LogLevel.WARN, LogChannel.CORE));
    }

    @Test
    void addonContextDefaultCroppyLoggerWrapsJulLogger() {
        Logger jul = Logger.getLogger("croppy.test.addon." + System.nanoTime());
        jul.setUseParentHandlers(false);
        AddonContext context = (AddonContext) Proxy.newProxyInstance(
                AddonContext.class.getClassLoader(),
                new Class<?>[]{AddonContext.class},
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    if ("getLogger".equals(method.getName())) {
                        return jul;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        CroppyLogger croppyLogger = context.getCroppyLogger();
        assertInstanceOf(JulCroppyLogger.class, croppyLogger);
        assertSame(jul, ((JulCroppyLogger) croppyLogger).delegate());
    }

    private static final class RecordingLogger implements CroppyLogger {
        private final LogLevel threshold;
        private final List<String> entries = new ArrayList<>();

        private RecordingLogger(LogLevel threshold) {
            this.threshold = threshold;
        }

        @Override
        public boolean isEnabled(LogLevel level, LogChannel channel) {
            return threshold.allows(level);
        }

        @Override
        public void log(LogLevel level, LogChannel channel, String message) {
            entries.add("[" + level + "][" + channel + "] " + message);
        }

        @Override
        public void log(LogLevel level, LogChannel channel, String message, Throwable throwable) {
            entries.add("[" + level + "][" + channel + "] " + message + " (" + throwable.getMessage() + ")");
        }
    }
}
