package dev.darkblade.croppy.core.resourcepack;

import dev.darkblade.croppy.api.resourcepack.PackBuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultResourcePackCompilerTest {

    private DefaultResourcePackCompiler compiler;

    @BeforeEach
    void setUp() {
        compiler = new DefaultResourcePackCompiler();
    }

    @Test
    void testSingleSourceBasePackCompilation(@TempDir Path tempDir) throws IOException {
        Path basePack = tempDir.resolve("base_pack");
        Files.createDirectories(basePack.resolve("assets/croppy/models/item"));

        Files.writeString(basePack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":64}}", StandardCharsets.UTF_8);
        Files.writeString(basePack.resolve("assets/croppy/models/item/wheat.json"), "{}", StandardCharsets.UTF_8);

        Path outputZip = tempDir.resolve("exports/croppy-unified-pack.zip");

        PackBuildResult result = compiler.compile(basePack, List.of(), outputZip);

        assertNotNull(result);
        assertEquals(outputZip, result.zipPath());
        assertEquals(2, result.assetCount());
        assertTrue(result.sizeBytes() > 0);
        assertEquals(40, result.sha1Hex().length());
        assertEquals(20, result.sha1Digest().length);
        assertTrue(Files.exists(outputZip));

        try (ZipFile zipFile = new ZipFile(outputZip.toFile())) {
            assertNotNull(zipFile.getEntry("pack.mcmeta"));
            assertNotNull(zipFile.getEntry("assets/croppy/models/item/wheat.json"));
        }
    }

    @Test
    void testMultiAddonAssetMerging(@TempDir Path tempDir) throws IOException {
        Path basePack = tempDir.resolve("base");
        Files.createDirectories(basePack.resolve("assets/croppy"));
        Files.writeString(basePack.resolve("pack.mcmeta"), "{}", StandardCharsets.UTF_8);

        Path addonCoffee = tempDir.resolve("addon_coffee");
        Files.createDirectories(addonCoffee.resolve("assets/croppy_coffee/models/item"));
        Files.writeString(addonCoffee.resolve("assets/croppy_coffee/models/item/coffee_cup.json"), "{\"model\":\"cup\"}", StandardCharsets.UTF_8);

        Path addonWinery = tempDir.resolve("addon_winery");
        Files.createDirectories(addonWinery.resolve("assets/croppy_winery/textures/item"));
        Files.writeString(addonWinery.resolve("assets/croppy_winery/textures/item/wine.png"), "fake-png-data", StandardCharsets.UTF_8);

        Path outputZip = tempDir.resolve("exports/pack.zip");

        PackBuildResult result = compiler.compile(basePack, List.of(addonCoffee, addonWinery), outputZip);

        assertEquals(3, result.assetCount());
        assertTrue(result.sizeBytes() > 0);

        try (ZipFile zipFile = new ZipFile(outputZip.toFile())) {
            assertNotNull(zipFile.getEntry("pack.mcmeta"));
            assertNotNull(zipFile.getEntry("assets/croppy_coffee/models/item/coffee_cup.json"));
            assertNotNull(zipFile.getEntry("assets/croppy_winery/textures/item/wine.png"));
        }
    }

    @Test
    void testSha1DigestConsistency(@TempDir Path tempDir) throws IOException {
        Path basePack = tempDir.resolve("base");
        Files.createDirectories(basePack);
        Files.writeString(basePack.resolve("pack.mcmeta"), "{\"pack_format\":64}", StandardCharsets.UTF_8);

        Path out1 = tempDir.resolve("pack1.zip");
        Path out2 = tempDir.resolve("pack2.zip");

        PackBuildResult r1 = compiler.compile(basePack, List.of(), out1);
        PackBuildResult r2 = compiler.compile(basePack, List.of(), out2);

        assertEquals(r1.sha1Hex(), r2.sha1Hex());
        assertArrayEquals(r1.sha1Digest(), r2.sha1Digest());

        // Modify content and verify SHA-1 changes
        Files.writeString(basePack.resolve("pack.mcmeta"), "{\"pack_format\":64,\"changed\":true}", StandardCharsets.UTF_8);
        PackBuildResult r3 = compiler.compile(basePack, List.of(), out1);
        assertNotEquals(r1.sha1Hex(), r3.sha1Hex());
    }

    @Test
    void testInvalidPathFiltering(@TempDir Path tempDir) throws IOException {
        Path basePack = tempDir.resolve("base");
        Files.createDirectories(basePack.resolve("assets/croppy/models"));
        Files.writeString(basePack.resolve("pack.mcmeta"), "{}", StandardCharsets.UTF_8);
        Files.writeString(basePack.resolve("assets/croppy/models/Invalid_Model.json"), "{}", StandardCharsets.UTF_8); // Uppercase 'I' and 'M' -> invalid
        Files.writeString(basePack.resolve("assets/croppy/models/valid_model.json"), "{}", StandardCharsets.UTF_8); // Valid

        Path outputZip = tempDir.resolve("pack.zip");
        PackBuildResult result = compiler.compile(basePack, List.of(), outputZip);

        // Only pack.mcmeta and valid_model.json should be added (Invalid_Model.json skipped)
        assertEquals(2, result.assetCount());

        try (ZipFile zipFile = new ZipFile(outputZip.toFile())) {
            assertNotNull(zipFile.getEntry("pack.mcmeta"));
            assertNotNull(zipFile.getEntry("assets/croppy/models/valid_model.json"));
            assertNull(zipFile.getEntry("assets/croppy/models/Invalid_Model.json"));
        }
    }

    private void assertNull(ZipEntry entry) {
        org.junit.jupiter.api.Assertions.assertNull(entry);
    }
}
