package dev.darkblade.croppy.platform.paper.logging;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PaperCroppyLoggerTest {

    private record CapturedEntry(LogLevel level, String plainText, Throwable throwable) {}

    private static PaperCroppyLogger createRecordingLogger(List<CapturedEntry> entries) {
        return new PaperCroppyLogger((level, component, throwable) ->
                entries.add(new CapturedEntry(level, PlainTextComponentSerializer.plainText().serialize(component), throwable)));
    }

    @Test
    void respectsThresholdAndSkipsLazySuppliersWhenFiltered() {
        List<CapturedEntry> entries = new ArrayList<>();
        PaperCroppyLogger logger = createRecordingLogger(entries);
        AtomicInteger expensiveCalls = new AtomicInteger();

        logger.trace(LogChannel.PACKET, () -> {
            expensiveCalls.incrementAndGet();
            return "trace payload";
        });
        logger.debug(LogChannel.LOADER, () -> {
            expensiveCalls.incrementAndGet();
            return "debug payload";
        });
        assertEquals(0, expensiveCalls.get());
        assertTrue(entries.isEmpty());

        logger.info("startup message");
        logger.warn(LogChannel.PACK, "warning payload");
        logger.error(LogChannel.STORAGE, "error payload", new IllegalStateException("db"));

        assertEquals(3, entries.size());
        assertEquals("startup message", entries.get(0).plainText());
        assertEquals("[pack] warning payload", entries.get(1).plainText());
        assertEquals("[storage] error payload", entries.get(2).plainText());
        assertNotNull(entries.get(2).throwable());
        assertEquals(1, logger.getWarningCount());
        assertEquals(1, logger.getErrorCount());

        logger.resetCounters();
        assertEquals(0, logger.getWarningCount());
        assertEquals(0, logger.getErrorCount());
    }

    @Test
    void debugChannelPromotionAndApplyConfigResetWorkCleanly() {
        List<CapturedEntry> entries = new ArrayList<>();
        PaperCroppyLogger logger = createRecordingLogger(entries);

        assertTrue(logger.enableDebugChannel(LogChannel.STATION));
        assertFalse(logger.enableDebugChannel(LogChannel.STATION));
        assertTrue(logger.isEnabled(LogLevel.DEBUG, LogChannel.STATION));
        assertFalse(logger.isEnabled(LogLevel.TRACE, LogChannel.STATION));
        assertFalse(logger.isEnabled(LogLevel.DEBUG, LogChannel.LOADER));

        logger.debug(LogChannel.STATION, "ticking station");
        logger.debug(LogChannel.LOADER, "ignored loader");
        assertEquals(1, entries.size());
        assertEquals("[DEBUG | station] ticking station", entries.get(0).plainText());

        // Toggle off and toggle back on
        assertFalse(logger.toggleDebugChannel(LogChannel.STATION));
        assertFalse(logger.isEnabled(LogLevel.DEBUG, LogChannel.STATION));
        assertTrue(logger.toggleDebugChannel(LogChannel.GROWTH));
        assertTrue(logger.isEnabled(LogLevel.DEBUG, LogChannel.GROWTH));

        // enableAll / disableAll
        logger.enableAllDebugChannels();
        assertEquals(LogChannel.values().length, logger.getDebugChannels().size());
        logger.disableAllDebugChannels();
        assertTrue(logger.getDebugChannels().isEmpty());

        // applyConfig resets runtime overrides to configured set
        logger.enableDebugChannel(LogChannel.PACKET);
        logger.applyConfig(LogLevel.WARN, Set.of(LogChannel.ADDON));
        assertEquals(LogLevel.WARN, logger.getMinimumLevel());
        assertEquals(Set.of(LogChannel.ADDON), logger.getDebugChannels());
        assertFalse(logger.isEnabled(LogLevel.INFO, LogChannel.CORE));
        assertTrue(logger.isEnabled(LogLevel.DEBUG, LogChannel.ADDON));
    }

    @Test
    void treatsMiniMessageTagsAndScopePrefixesAsPlainText() {
        List<CapturedEntry> entries = new ArrayList<>();
        PaperCroppyLogger logger = createRecordingLogger(entries);

        CroppyLogger addonLogger = logger.scoped("Addon:coffee");
        addonLogger.info(LogChannel.ADDON, "Bad config <red><click:run_command:/op>msg</click></red>");

        assertEquals(1, entries.size());
        assertEquals("[addon] [Addon:coffee] Bad config <red><click:run_command:/op>msg</click></red>",
                entries.get(0).plainText());
    }
}
