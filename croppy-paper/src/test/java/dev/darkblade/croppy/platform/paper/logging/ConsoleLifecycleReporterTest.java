package dev.darkblade.croppy.platform.paper.logging;

import dev.darkblade.croppy.api.logging.LogLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleLifecycleReporterTest {

    @Test
    void richModeEmitsBannerPhaseRowsAndWarningCountersWithoutBoxDrift() {
        List<String> rawLines = new ArrayList<>();
        List<String> logLines = new ArrayList<>();
        AtomicReference<LogStyle> style = new AtomicReference<>(LogStyle.RICH);
        AtomicInteger warnings = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        PaperCroppyLogger logger = new PaperCroppyLogger((level, comp, t) ->
                logLines.add(PlainTextComponentSerializer.plainText().serialize(comp)));

        ConsoleLifecycleReporter reporter = new ConsoleLifecycleReporter(
                style::get,
                logger,
                comp -> rawLines.add(PlainTextComponentSerializer.plainText().serialize(comp)),
                warnings::get,
                errors::get
        );

        reporter.emitStartupBanner("0.7.0", "Paper 1.21.8-R0.1", "21");
        assertTrue(rawLines.stream().anyMatch(line -> line.contains("Modular Agriculture Engine")));
        assertTrue(rawLines.stream().anyMatch(line -> line.contains("v0.7.0 · Paper")));
        assertTrue(rawLines.stream().anyMatch(line -> line.contains("Environment: Paper 1.21.8-R0.1 · Java 21")));
        // Every bordered line in the banner must share exact visual box width (66 chars incl. corners/bars)
        List<String> bordered = rawLines.stream().filter(l -> l.startsWith("╭") || l.startsWith("│") || l.startsWith("├") || l.startsWith("╰")).toList();
        assertFalse(bordered.isEmpty());
        int expectedWidth = bordered.get(0).length();
        for (String line : bordered) {
            assertEquals(expectedWidth, line.length(), "Misaligned banner line: " + line);
        }
        assertTrue(logLines.isEmpty());

        reporter.emitPhase(1, 10, "Storage Backend", "SQLite (HikariCP)", 14, ConsoleLifecycleReporter.PhaseStatus.OK);
        reporter.emitPhase(2, 10, "Addons", "1 active (croppy_coffee)", 22, ConsoleLifecycleReporter.PhaseStatus.WARN);
        warnings.set(1);
        reporter.emitStartupComplete("0.7.0", 95);

        assertTrue(rawLines.stream().anyMatch(l -> l.contains("✓") && l.contains("Storage Backend") && l.contains("14 ms")));
        assertTrue(rawLines.stream().anyMatch(l -> l.contains("⚠") && l.contains("Addons") && l.contains("22 ms")));
        assertTrue(rawLines.stream().anyMatch(l -> l.contains("Croppy v0.7.0 enabled in 95 ms") && l.contains("1 warning(s), 0 error(s)")));

        rawLines.clear();
        reporter.emitReloadSummary(18, "12 species, 1 addons");
        reporter.emitShutdownSummary(11, "saved 4 crops");
        assertTrue(rawLines.stream().anyMatch(l -> l.contains("Croppy reloaded in 18 ms") && l.contains("12 species, 1 addons")));
        assertTrue(rawLines.stream().anyMatch(l -> l.contains("Croppy disabled cleanly in 11 ms") && l.contains("saved 4 crops")));
    }

    @Test
    void compactModeEmitsSingleLineLoggerSummariesWithNoRawBanner() {
        List<String> rawLines = new ArrayList<>();
        List<String> logLines = new ArrayList<>();
        PaperCroppyLogger logger = new PaperCroppyLogger((level, comp, t) ->
                logLines.add(PlainTextComponentSerializer.plainText().serialize(comp)));

        ConsoleLifecycleReporter reporter = new ConsoleLifecycleReporter(
                () -> LogStyle.COMPACT,
                logger,
                comp -> rawLines.add(PlainTextComponentSerializer.plainText().serialize(comp))
        );

        reporter.emitStartupBanner("0.7.0", "Paper 1.21.8", "21");
        reporter.emitPhase(1, 3, "Core Services", "ready", 5, ConsoleLifecycleReporter.PhaseStatus.OK);
        logger.warn(dev.darkblade.croppy.api.logging.LogChannel.LOADER, "bad file");
        reporter.emitStartupComplete("0.7.0", 40);
        reporter.emitReloadSummary(10, "all reloaded");
        reporter.emitShutdownSummary(7, "saved 0 crops");

        assertTrue(rawLines.isEmpty(), "COMPACT mode must not emit raw banner/box lines");
        assertTrue(logLines.stream().anyMatch(l -> l.contains("Enabling Croppy v0.7.0")));
        assertTrue(logLines.stream().anyMatch(l -> l.contains("[1/3] Core Services: ready (5 ms)")));
        assertTrue(logLines.stream().anyMatch(l -> l.contains("Enabled Croppy v0.7.0 in 40 ms (1 warning(s), 0 error(s))")));
        assertTrue(logLines.stream().anyMatch(l -> l.contains("Reloaded Croppy in 10 ms")));
        assertTrue(logLines.stream().anyMatch(l -> l.contains("Disabled Croppy in 7 ms")));
    }

    @Test
    void handlesMiniMessageCharactersInDynamicStringsSafely() {
        List<Component> emitted = new ArrayList<>();
        PaperCroppyLogger logger = new PaperCroppyLogger((l, c, t) -> {});
        ConsoleLifecycleReporter reporter = new ConsoleLifecycleReporter(() -> LogStyle.RICH, logger, emitted::add);

        assertDoesNotThrow(() -> reporter.emitStartupBanner("1.0+<dev>", "Paper <tag>", "21"));
        assertDoesNotThrow(() -> reporter.emitPhase(1, 1, "Loader", "<unclosed", 3, ConsoleLifecycleReporter.PhaseStatus.ERROR));
        String allPlain = emitted.stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(allPlain.contains("1.0+<dev>"));
        assertTrue(allPlain.contains("<unclosed"));
    }

    @Test
    void doesNotDuplicateJavaPrefixWhenCallerAlreadyIncludesJavaWord() {
        List<String> rawLines = new ArrayList<>();
        PaperCroppyLogger logger = new PaperCroppyLogger((l, c, t) -> {});
        ConsoleLifecycleReporter reporter = new ConsoleLifecycleReporter(
                () -> LogStyle.RICH,
                logger,
                comp -> rawLines.add(PlainTextComponentSerializer.plainText().serialize(comp))
        );

        reporter.emitStartupBanner("0.7.0", "Paper 1.21.8", "Java 25.0.5");

        assertTrue(rawLines.stream().anyMatch(line -> line.contains("Environment: Paper 1.21.8 · Java 25.0.5")),
                "Expected single Java prefix in banner, got: " + rawLines);
        assertFalse(rawLines.stream().anyMatch(line -> line.contains("Java Java")),
                "Duplicate Java prefix must never be emitted");
    }
}
