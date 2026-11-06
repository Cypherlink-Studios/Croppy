package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.AddonState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAddonManagerTest {

    @TempDir
    Path tempDir;

    private DefaultAddonManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultAddonManager(tempDir, null);
    }

    @Test
    @DisplayName("Discover directory-based addon")
    void testDiscoverDirectoryAddon() throws IOException {
        Path addonDir = tempDir.resolve("my-pack");
        Files.createDirectories(addonDir);
        String yaml = """
                id: my-pack
                name: "My Content Pack"
                version: "1.0.0"
                namespace: my_pack
                """;
        Files.writeString(addonDir.resolve("addon.yml"), yaml, StandardCharsets.UTF_8);

        manager.discoverAddons();

        Optional<AddonDescription> desc = manager.getAddon("my-pack");
        assertTrue(desc.isPresent());
        assertEquals("My Content Pack", desc.get().name());
        assertEquals(AddonState.ON_DISK, manager.getState("my-pack"));
        assertEquals(addonDir, manager.getAddonPath("my-pack").orElseThrow());
    }

    @Test
    @DisplayName("Discover JAR-based addon")
    void testDiscoverJarAddon() throws IOException {
        Path jarPath = tempDir.resolve("code-addon.jar");
        String yaml = """
                id: code-addon
                name: "Code Addon"
                version: "1.0.0"
                namespace: code_addon
                main: "com.example.TestAddon"
                """;

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            jos.putNextEntry(new JarEntry("addon.yml"));
            jos.write(yaml.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        manager.discoverAddons();

        Optional<AddonDescription> desc = manager.getAddon("code-addon");
        assertTrue(desc.isPresent());
        assertEquals("Code Addon", desc.get().name());
        assertTrue(desc.get().isCodeAddon());
        assertEquals(jarPath, manager.getAddonPath("code-addon").orElseThrow());
    }

    @Test
    @DisplayName("Lifecycle state transitions")
    void testLifecycleTransitions() {
        AddonDescription desc = new AddonDescription(
                "test", "Test", "1.0", "test", Optional.empty(), java.util.List.of(), "", java.util.List.of(), java.util.List.of()
        );
        manager.registerDiscovered(desc, tempDir.resolve("test"));

        assertEquals(AddonState.ON_DISK, manager.getState("test"));

        assertTrue(manager.loadAddon("test"));
        assertEquals(AddonState.LOADED, manager.getState("test"));

        assertTrue(manager.enableAddon("test"));
        assertEquals(AddonState.ENABLED, manager.getState("test"));

        assertTrue(manager.disableAddon("test"));
        assertEquals(AddonState.LOADED, manager.getState("test"));

        assertTrue(manager.unloadAddon("test"));
        assertEquals(AddonState.ON_DISK, manager.getState("test"));
    }
}
