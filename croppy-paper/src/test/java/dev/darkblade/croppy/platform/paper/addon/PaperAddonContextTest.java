package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonConfig;
import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, null, null, null);

        Key speciesKey = Key.of("test_addon", "magic_crop");
        Key itemKey = Key.of("test_addon", "magic_seed");
        Key recipeKey = Key.of("test_addon", "magic_stew");
        Key menuKey = Key.of("test_addon", "magic_menu");

        context.trackSpecies(speciesKey);
        context.trackItem(itemKey);
        context.trackRecipe(recipeKey);
        context.trackMenu(menuKey);

        assertTrue(context.getRegisteredSpecies().contains(speciesKey));
        assertTrue(context.getRegisteredItems().contains(itemKey));
        assertTrue(context.getRegisteredRecipes().contains(recipeKey));
        assertTrue(context.getRegisteredMenus().contains(menuKey));

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

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, null, null, null);
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

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, null, null, null);
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

        PaperAddonContext context = new PaperAddonContext(desc, addonDataDir, jarFile, null, null, null, null);

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
}
