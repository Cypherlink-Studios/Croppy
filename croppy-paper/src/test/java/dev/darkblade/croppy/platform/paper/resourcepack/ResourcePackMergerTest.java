package dev.darkblade.croppy.platform.paper.resourcepack;

import dev.darkblade.croppy.api.addon.AddonDescription;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackMergerTest {

    @TempDir
    Path tempDir;

    private Path packDir;
    private ResourcePackMerger merger;

    @BeforeEach
    void setUp() {
        packDir = tempDir.resolve("resourcepack");
        merger = new ResourcePackMerger(packDir, dev.darkblade.croppy.api.logging.NoOpCroppyLogger.INSTANCE);
    }

    private AddonDescription createDesc(String id, String namespace) {
        return new AddonDescription(
                id, id, "1.0", namespace, Optional.empty(), List.of(), "", List.of(), List.of()
        );
    }

    @Test
    @DisplayName("Merge assets from directory into companion pack")
    void testMergeFromDirectory() throws IOException {
        Path addonDir = tempDir.resolve("my-addon");
        Path modelFile = addonDir.resolve("assets").resolve("winery").resolve("models").resolve("item").resolve("barrel.json");
        Files.createDirectories(modelFile.getParent());
        Files.writeString(modelFile, "{\"parent\":\"minecraft:item/generated\"}", StandardCharsets.UTF_8);

        AddonDescription desc = createDesc("winery-addon", "winery");
        int merged = merger.mergeAddonAssets(desc, addonDir);
        assertEquals(1, merged);

        Path targetFile = packDir.resolve("assets").resolve("winery").resolve("models").resolve("item").resolve("barrel.json");
        assertTrue(Files.exists(targetFile));
        assertEquals("{\"parent\":\"minecraft:item/generated\"}", Files.readString(targetFile));
    }

    @Test
    @DisplayName("Merge assets from JAR file into companion pack")
    void testMergeFromJar() throws IOException {
        Path jarPath = tempDir.resolve("addon.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            jos.putNextEntry(new JarEntry("assets/bakery/textures/item/pie.png"));
            jos.write("fake-png-bytes".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        AddonDescription desc = createDesc("bakery-addon", "bakery");
        int merged = merger.mergeAddonAssets(desc, jarPath);
        assertEquals(1, merged);

        Path targetFile = packDir.resolve("assets").resolve("bakery").resolve("textures").resolve("item").resolve("pie.png");
        assertTrue(Files.exists(targetFile));
        assertEquals("fake-png-bytes", Files.readString(targetFile));
    }

    @Test
    @DisplayName("Reject unauthorized overwrite of core croppy namespace")
    void testRejectCoreNamespaceOverwrite() throws IOException {
        Path addonDir = tempDir.resolve("malicious-addon");
        Path modelFile = addonDir.resolve("assets").resolve("croppy").resolve("items").resolve("wheat.json");
        Files.createDirectories(modelFile.getParent());
        Files.writeString(modelFile, "{\"malicious\":true}", StandardCharsets.UTF_8);

        // Addon namespace is "custom", not "croppy"
        AddonDescription desc = createDesc("custom-addon", "custom");
        int merged = merger.mergeAddonAssets(desc, addonDir);
        assertEquals(0, merged);

        Path targetFile = packDir.resolve("assets").resolve("croppy").resolve("items").resolve("wheat.json");
        assertFalse(Files.exists(targetFile));
    }

    @Test
    @DisplayName("Reject invalid asset path segment naming")
    void testRejectInvalidPathSegment() throws IOException {
        Path addonDir = tempDir.resolve("invalid-addon");
        Path invalidFile = addonDir.resolve("assets").resolve("custom").resolve("Invalid Name.json");
        Files.createDirectories(invalidFile.getParent());
        Files.writeString(invalidFile, "{}", StandardCharsets.UTF_8);

        AddonDescription desc = createDesc("custom-addon", "custom");
        int merged = merger.mergeAddonAssets(desc, addonDir);
        assertEquals(0, merged);
    }
}
