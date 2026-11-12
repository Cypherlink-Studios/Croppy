package dev.darkblade.croppy.platform.paper.logging;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Formats and streams structured plugin lifecycle output ({@code onEnable}, {@code reload},
 * {@code onDisable}) in either {@link LogStyle#RICH} or {@link LogStyle#COMPACT} mode.
 */
public final class ConsoleLifecycleReporter {

    public enum PhaseStatus {
        OK,
        WARN,
        ERROR
    }

    private static final int BANNER_INNER_WIDTH = 66;
    private static final int LABEL_COLUMN_WIDTH = 22;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Supplier<LogStyle> styleSupplier;
    private final CroppyLogger logger;
    private final Consumer<Component> rawConsoleSender;
    private final IntSupplier warningCountSupplier;
    private final IntSupplier errorCountSupplier;

    /**
     * Creates a lifecycle reporter wired to a {@link PaperCroppyLogger} and raw console sink.
     *
     * @param styleSupplier    dynamic style supplier reflecting reloaded config
     * @param logger           structured plugin logger (for counts and {@link LogStyle#COMPACT} lines)
     * @param rawConsoleSender main-thread sender for prefix-free {@link LogStyle#RICH} components
     */
    public ConsoleLifecycleReporter(
            Supplier<LogStyle> styleSupplier,
            PaperCroppyLogger logger,
            Consumer<Component> rawConsoleSender
    ) {
        this(
                styleSupplier,
                Objects.requireNonNull(logger, "logger cannot be null"),
                rawConsoleSender,
                logger::getWarningCount,
                logger::getErrorCount
        );
    }

    /**
     * Full constructor allowing isolated unit-test stubs.
     */
    public ConsoleLifecycleReporter(
            Supplier<LogStyle> styleSupplier,
            CroppyLogger logger,
            Consumer<Component> rawConsoleSender,
            IntSupplier warningCountSupplier,
            IntSupplier errorCountSupplier
    ) {
        this.styleSupplier = Objects.requireNonNull(styleSupplier, "styleSupplier cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.rawConsoleSender = rawConsoleSender != null
                ? rawConsoleSender
                : component -> logger.info(LogChannel.CORE, PlainTextComponentSerializer.plainText().serialize(component));
        this.warningCountSupplier = Objects.requireNonNull(warningCountSupplier, "warningCountSupplier cannot be null");
        this.errorCountSupplier = Objects.requireNonNull(errorCountSupplier, "errorCountSupplier cannot be null");
    }

    /**
     * Emits the startup header before enable phases begin executing.
     */
    public void emitStartupBanner(String version, String platformInfo, String javaInfo) {
        String safeVersion = safeText(version, "unknown");
        String safePlatform = safeText(platformInfo, "Paper");
        String safeJava = normalizeJavaVersion(javaInfo);

        if (currentStyle() == LogStyle.COMPACT) {
            logger.info(LogChannel.CORE, "Enabling Croppy v" + safeVersion + " (" + safePlatform + ", Java " + safeJava + ")...");
            return;
        }

        String border = "─".repeat(BANNER_INNER_WIDTH);
        sendRaw("<dark_gray>╭" + border + "╮</dark_gray>");
        sendBannerLogoRow("   ____", "");
        sendBannerLogoRow("  / ___|_ __ ___  _ __  _ __  _   _", "Modular Agriculture Engine");
        sendBannerLogoRow(" | |   | '__/ _ \\| '_ \\| '_ \\| | | |", "& Culinary Simulation");
        sendBannerLogoRow(" | |___| | | (_) | |_) | |_) | |_| |", "");
        sendBannerLogoRow("  \\____|_|  \\___/| .__/| .__/ \\__, |", "v" + safeVersion + " · Paper");
        sendBannerLogoRow("                 |_|   |_|    |___/", "");
        sendRaw("<dark_gray>├" + border + "┤</dark_gray>");
        String envContent = truncate(" Environment: " + safePlatform + " · Java " + safeJava, BANNER_INNER_WIDTH);
        sendRaw("<dark_gray>│</dark_gray><gray><content></gray><dark_gray><pad>│</dark_gray>",
                Placeholder.unparsed("content", envContent),
                Placeholder.unparsed("pad", " ".repeat(Math.max(0, BANNER_INNER_WIDTH - envContent.length()))));
        sendRaw("<dark_gray>╰" + border + "╯</dark_gray>");
    }

    /**
     * Immediately emits one completed startup phase row so progress streams in real time.
     */
    public void emitPhase(int index, int total, String label, String summary, long durationMillis, PhaseStatus status) {
        String safeLabel = safeText(label, "Step");
        String safeSummary = safeText(summary, "done");
        PhaseStatus effectiveStatus = status != null ? status : PhaseStatus.OK;

        if (currentStyle() == LogStyle.COMPACT) {
            logger.info(LogChannel.CORE, "[" + index + "/" + total + "] " + safeLabel + ": " + safeSummary + " (" + durationMillis + " ms)");
            return;
        }

        String badge = statusBadge(effectiveStatus);
        int digits = Math.max(2, String.valueOf(total).length());
        String indexText = "[" + String.format("%" + digits + "d/%d", index, total) + "]";
        int dotCount = Math.max(2, LABEL_COLUMN_WIDTH - safeLabel.length());
        String dots = " " + ".".repeat(dotCount) + " ";

        sendRaw("  " + badge + " <dark_gray><index></dark_gray> <white><label></white><dark_gray><dots></dark_gray><gray><summary></gray> <dark_gray>(<ms> ms)</dark_gray>",
                Placeholder.unparsed("index", indexText),
                Placeholder.unparsed("label", safeLabel),
                Placeholder.unparsed("dots", dots),
                Placeholder.unparsed("summary", safeSummary),
                Placeholder.unparsed("ms", String.valueOf(durationMillis)));
    }

    /**
     * Emits the startup completion footer with total elapsed time and warning/error tallies.
     */
    public void emitStartupComplete(String version, long totalMillis) {
        String safeVersion = safeText(version, "unknown");
        int warnings = Math.max(0, warningCountSupplier.getAsInt());
        int errors = Math.max(0, errorCountSupplier.getAsInt());

        if (currentStyle() == LogStyle.COMPACT) {
            logger.info(LogChannel.CORE, "Enabled Croppy v" + safeVersion + " in " + totalMillis + " ms ("
                    + formatIssueCounts(warnings, errors) + ").");
            return;
        }

        PhaseStatus overall = errors > 0 ? PhaseStatus.ERROR : (warnings > 0 ? PhaseStatus.WARN : PhaseStatus.OK);
        sendRaw("  <dark_gray>" + "─".repeat(BANNER_INNER_WIDTH) + "</dark_gray>");
        sendRaw("  " + statusBadge(overall) + " <green>Croppy <bold>v<version></bold> enabled</green> <dark_gray>in</dark_gray> <white><ms> ms</white> <dark_gray>·</dark_gray> "
                        + coloredIssueSummary(warnings, errors),
                Placeholder.unparsed("version", safeVersion),
                Placeholder.unparsed("ms", String.valueOf(totalMillis)));
    }

    /**
     * Emits a concise summary after {@code /croppy reload} completes (without re-printing the ASCII banner).
     */
    public void emitReloadSummary(long totalMillis, String summary) {
        String safeSummary = safeText(summary, "configuration & definitions refreshed");
        int warnings = Math.max(0, warningCountSupplier.getAsInt());
        int errors = Math.max(0, errorCountSupplier.getAsInt());

        if (currentStyle() == LogStyle.COMPACT) {
            logger.info(LogChannel.CORE, "Reloaded Croppy in " + totalMillis + " ms (" + safeSummary + "; "
                    + formatIssueCounts(warnings, errors) + ").");
            return;
        }

        PhaseStatus status = errors > 0 ? PhaseStatus.ERROR : (warnings > 0 ? PhaseStatus.WARN : PhaseStatus.OK);
        sendRaw("  " + statusBadge(status) + " <green>Croppy reloaded</green> <dark_gray>in</dark_gray> <white><ms> ms</white> <dark_gray>·</dark_gray> <gray><summary></gray> <dark_gray>·</dark_gray> "
                        + coloredIssueSummary(warnings, errors),
                Placeholder.unparsed("ms", String.valueOf(totalMillis)),
                Placeholder.unparsed("summary", safeSummary));
    }

    /**
     * Emits a concise summary at the end of {@code onDisable}.
     */
    public void emitShutdownSummary(long totalMillis, String summary) {
        String safeSummary = safeText(summary, "clean shutdown");
        if (currentStyle() == LogStyle.COMPACT) {
            logger.info(LogChannel.CORE, "Disabled Croppy in " + totalMillis + " ms (" + safeSummary + ").");
            return;
        }

        sendRaw("  <green>✓</green> <gray>Croppy disabled cleanly</gray> <dark_gray>in</dark_gray> <white><ms> ms</white> <dark_gray>·</dark_gray> <gray><summary></gray>",
                Placeholder.unparsed("ms", String.valueOf(totalMillis)),
                Placeholder.unparsed("summary", safeSummary));
    }

    private LogStyle currentStyle() {
        LogStyle style = styleSupplier.get();
        return style != null ? style : LogStyle.RICH;
    }

    private void sendBannerLogoRow(String asciiPart, String caption) {
        String paddedAscii = String.format("%-38s", asciiPart);
        String truncatedCaption = truncate(caption, BANNER_INNER_WIDTH - 39);
        int rightPad = Math.max(0, BANNER_INNER_WIDTH - paddedAscii.length() - truncatedCaption.length());
        sendRaw("<dark_gray>│</dark_gray><gradient:#4ade80:#16a34a><bold><logo></bold></gradient><gray><caption_text></gray><dark_gray><pad>│</dark_gray>",
                Placeholder.unparsed("logo", paddedAscii),
                Placeholder.unparsed("caption_text", truncatedCaption),
                Placeholder.unparsed("pad", " ".repeat(rightPad)));
    }

    private void sendRaw(String template, TagResolver... resolvers) {
        rawConsoleSender.accept(MINI_MESSAGE.deserialize(template, resolvers));
    }

    private static String statusBadge(PhaseStatus status) {
        return switch (status) {
            case OK -> "<green>✓</green>";
            case WARN -> "<yellow>⚠</yellow>";
            case ERROR -> "<red>✖</red>";
        };
    }

    private static String coloredIssueSummary(int warnings, int errors) {
        String warnColor = warnings > 0 ? "<yellow>" : "<gray>";
        String warnEnd = warnings > 0 ? "</yellow>" : "</gray>";
        String errColor = errors > 0 ? "<red>" : "<gray>";
        String errEnd = errors > 0 ? "</red>" : "</gray>";
        return warnColor + warnings + " warning(s)" + warnEnd + "<dark_gray>, </dark_gray>"
                + errColor + errors + " error(s)" + errEnd;
    }

    private static String formatIssueCounts(int warnings, int errors) {
        return warnings + " warning(s), " + errors + " error(s)";
    }

    private static String normalizeJavaVersion(String javaInfo) {
        String fallback = System.getProperty("java.version", "21");
        String trimmed = safeText(javaInfo, fallback);
        if (trimmed.regionMatches(true, 0, "Java ", 0, 5)) {
            return safeText(trimmed.substring(5), fallback);
        }
        return trimmed;
    }

    private static String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return maxLength <= 3 ? value.substring(0, maxLength) : value.substring(0, maxLength - 3) + "...";
    }
}
