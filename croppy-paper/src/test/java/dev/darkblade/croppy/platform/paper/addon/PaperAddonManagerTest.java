package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.core.CroppyCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperAddonManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("PaperAddonManager automatically discovers and loads YAML resources from a JAR addon")
    void testLoadJarAddonResources() throws IOException {
        Path addonsDir = tempDir.resolve("addons");
        Files.createDirectories(addonsDir);

        Path jarFile = addonsDir.resolve("test-addon.jar");
        createTestAddonJar(jarFile);

        CroppyCore core = new CroppyCore();
        core.initialize();

        PaperAddonManager manager = new PaperAddonManager(null, core, addonsDir, null);
        manager.discoverAddons();

        assertEquals(1, manager.getDiscoveredAddons().size());
        assertTrue(manager.enableAddon("mock-addon"));

        PaperAddonContext context = manager.getContext("mock-addon").orElseThrow();
        assertEquals(1, context.getRegisteredSpecies().size());
        assertEquals(1, context.getRegisteredItems().size());
        assertEquals(1, context.getRegisteredRecipes().size());

        assertTrue(core.getSpeciesRegistry().get(Key.of("mock_addon", "star_berry")).isPresent());
        assertTrue(core.getItemRegistry().get(Key.of("mock_addon", "star_fruit")).isPresent());
        assertTrue(core.getRecipeRegistry().get(Key.of("mock_addon", "star_juice")).isPresent());
    }

    private void createTestAddonJar(Path jarFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jarFile.toFile()))) {
            // addon.yml
            String manifest = """
                    id: "mock-addon"
                    name: "Mock Addon"
                    version: "1.0.0"
                    namespace: "mock_addon"
                    """;
            zos.putNextEntry(new ZipEntry("addon.yml"));
            zos.write(manifest.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // crops/star_berry.yml
            String cropYaml = """
                    id: "mock_addon:star_berry"
                    name: "Star Berry"
                    display:
                      type: "vanilla"
                      block: "minecraft:sweet_berry_bush"
                      seed_item: "mock_addon:star_fruit"
                    stages:
                      - id: "seed"
                        progress_required: 100
                    requirements:
                      light:
                        min: 9
                    """;
            zos.putNextEntry(new ZipEntry("crops/star_berry.yml"));
            zos.write(cropYaml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // items/ingredients/star_fruit.yml
            String itemYaml = """
                    id: "mock_addon:star_fruit"
                    material: "APPLE"
                    name: "Star Fruit"
                    """;
            zos.putNextEntry(new ZipEntry("items/ingredients/star_fruit.yml"));
            zos.write(itemYaml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // recipes/star_juice.yml
            String recipeYaml = """
                    id: "mock_addon:star_juice"
                    name: "Star Juice"
                    category: "beverages"
                    station: "croppy:skillet"
                    result:
                      item: "mock_addon:star_fruit"
                      amount: 1
                    inputs:
                      - item: "mock_addon:star_fruit"
                        amount: 2
                    """;
            zos.putNextEntry(new ZipEntry("recipes/star_juice.yml"));
            zos.write(recipeYaml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }
}
