package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonConfig;
import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PaperAddonContextTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Track domain keys and modifiers in PaperAddonContext")
    void testTrackResources() {
        AddonDescription desc = new AddonDescription(
                "test-addon", "Test", "1.0", "test_addon", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, NoOpCroppyLogger.INSTANCE, null, null);

        Key speciesKey = Key.of("test_addon", "magic_crop");
        Key itemKey = Key.of("test_addon", "magic_seed");
        Key recipeKey = Key.of("test_addon", "magic_stew");
        Key menuKey = Key.of("test_addon", "magic_menu");
        Key interactionKey = Key.of("test_addon", "magic_interact");

        context.trackSpecies(speciesKey);
        context.trackItem(itemKey);
        context.trackRecipe(recipeKey);
        context.trackMenu(menuKey);
        context.trackInteraction(interactionKey);

        assertTrue(context.getRegisteredSpecies().contains(speciesKey));
        assertTrue(context.getRegisteredItems().contains(itemKey));
        assertTrue(context.getRegisteredRecipes().contains(recipeKey));
        assertTrue(context.getRegisteredMenus().contains(menuKey));
        assertTrue(context.getRegisteredInteractions().contains(interactionKey));

        GrowthModifier modifier = ctx -> 2.0;
        context.trackGrowthModifier(modifier);
        assertTrue(context.getRegisteredGrowthModifiers().contains(modifier));
    }

    @Test
    @DisplayName("Run managed async task and shutdown executor")
    void testManagedExecutor() throws InterruptedException {
        AddonDescription desc = new AddonDescription(
                "async-addon", "Async", "1.0", "async_addon", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, NoOpCroppyLogger.INSTANCE, null, null);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        context.runAsync(() -> {
            executed.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(executed.get());

        context.shutdownExecutor();
        assertTrue(context.getManagedExecutor().isShutdown());
    }

    @Test
    @DisplayName("Resolve platform adapter")
    void testPlatformAdapter() {
        AddonDescription desc = new AddonDescription(
                "adapter-test", "Adapter", "1.0", "adapter_addon", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, NoOpCroppyLogger.INSTANCE, null, null);
        assertTrue(context.getPlatformAdapter(PaperAddonContext.class).isPresent());
        assertEquals(context, context.getPlatformAdapter(PaperAddonContext.class).get());
        assertTrue(context.getPlatformAdapter(String.class).isEmpty());
    }

    @Test
    @DisplayName("Extract bundled config and query values")
    void testExtractBundledConfigAndQuery() throws IOException {
        Path addonDataDir = tempDir.resolve("my-addon-data");
        Path jarFile = tempDir.resolve("my-addon.jar");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jarFile.toFile()))) {
            String defaultYaml = """
                    # Default Addon Config
                    brewing:
                      temperature: 91.5
                      auto-start: true
                    """;
            zos.putNextEntry(new ZipEntry("config.yml"));
            zos.write(defaultYaml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        AddonDescription desc = new AddonDescription(
                "my-addon", "My Addon", "1.0", "my_addon", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        PaperAddonContext context = new PaperAddonContext(desc, addonDataDir, jarFile, null, NoOpCroppyLogger.INSTANCE, null, null);

        // Before extract, config does not exist on disk
        assertFalse(Files.exists(addonDataDir.resolve("config.yml")));

        context.saveDefaultConfig();

        // After extract, config exists and matches default
        assertTrue(Files.exists(addonDataDir.resolve("config.yml")));

        AddonConfig config = context.getConfig();
        assertEquals(91.5, config.getDouble("brewing.temperature"), 0.001);
        assertTrue(config.getBoolean("brewing.auto-start"));

        // Modifying in-memory and saving
        config.set("brewing.temperature", 98.0);
        context.saveConfig();

        context.reloadConfig();
        assertEquals(98.0, context.getConfig().getDouble("brewing.temperature"), 0.001);

        context.shutdownExecutor();
    }

    @Test
    @DisplayName("Isolated JUL bridge forwards addon logs to CroppyLogger on ADDON channel without parent leakage")
    void testJulToCroppyLoggerBridge() {
        record Entry(LogLevel level, LogChannel channel, String message, Throwable error) {}
        List<Entry> captured = new ArrayList<>();

        CroppyLogger recording = new CroppyLogger() {
            @Override
            public boolean isEnabled(LogLevel level, LogChannel channel) {
                return true;
            }

            @Override
            public void log(LogLevel level, LogChannel channel, String message) {
                captured.add(new Entry(level, channel, message, null));
            }

            @Override
            public void log(LogLevel level, LogChannel channel, String message, Throwable error) {
                captured.add(new Entry(level, channel, message, error));
            }

            @Override
            public void log(LogLevel level, LogChannel channel, Supplier<String> message, Throwable error) {
                captured.add(new Entry(level, channel, message.get(), error));
            }
        };

        AddonDescription desc = new AddonDescription(
                "bridge-test", "Bridge Test", "1.0", "bridge_test", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, recording, null, null);
        Logger jul = context.getLogger();

        assertEquals("Croppy-bridge-test", jul.getName());
        assertFalse(jul.getUseParentHandlers());
        assertEquals(Level.ALL, jul.getLevel());

        Logger root = Logger.getLogger("");
        List<LogRecord> parentRecords = new ArrayList<>();
        Handler spyParent = new Handler() {
            @Override public void publish(LogRecord record) { parentRecords.add(record); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        root.addHandler(spyParent);
        try {
            RuntimeException boom = new RuntimeException("boom");
            jul.log(Level.INFO, "Hello {0}", "Coffee");
            jul.warning("Watch out");
            jul.log(Level.SEVERE, "Broken", boom);
            jul.fine("Fine detail");
            jul.finest("Finest detail");

            assertTrue(parentRecords.isEmpty(), "Isolated addon JUL logger must not propagate to root handlers");
            assertEquals(5, captured.size());

            assertEquals(LogLevel.INFO, captured.get(0).level());
            assertEquals(LogChannel.ADDON, captured.get(0).channel());
            assertEquals("[Addon:bridge-test] Hello Coffee", captured.get(0).message());

            assertEquals(LogLevel.WARN, captured.get(1).level());
            assertEquals("[Addon:bridge-test] Watch out", captured.get(1).message());

            assertEquals(LogLevel.ERROR, captured.get(2).level());
            assertEquals("[Addon:bridge-test] Broken", captured.get(2).message());
            assertSame(boom, captured.get(2).error());

            assertEquals(LogLevel.DEBUG, captured.get(3).level());
            assertEquals("[Addon:bridge-test] Fine detail", captured.get(3).message());

            assertEquals(LogLevel.TRACE, captured.get(4).level());
            assertEquals("[Addon:bridge-test] Finest detail", captured.get(4).message());

            context.getCroppyLogger().info(LogChannel.ADDON, "Direct API call");
            assertEquals(6, captured.size());
            assertEquals("[Addon:bridge-test] Direct API call", captured.get(5).message());
        } finally {
            root.removeHandler(spyParent);
            context.shutdownExecutor();
        }
    }
}
