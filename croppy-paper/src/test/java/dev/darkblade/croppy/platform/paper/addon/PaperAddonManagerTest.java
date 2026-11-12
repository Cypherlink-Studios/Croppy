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
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PaperAddonManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("PaperAddonManager automatically discovers and loads YAML resources from a JAR addon with config")
    void testLoadJarAddonResourcesAndConfig() throws IOException {
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
        assertEquals(1, context.getRegisteredMenus().size());
        assertEquals(1, context.getRegisteredInteractions().size());

        assertTrue(core.getSpeciesRegistry().get(Key.of("mock_addon", "star_berry")).isPresent());
        assertTrue(core.getItemRegistry().get(Key.of("mock_addon", "star_fruit")).isPresent());
        assertTrue(core.getRecipeRegistry().get(Key.of("mock_addon", "star_juice")).isPresent());
        assertTrue(core.getMenuRegistry().get(Key.of("mock_addon", "custom_menu")).isPresent());
        assertTrue(core.getInteractionRegistry().get(Key.of("mock_addon", "stir_pot")).isPresent());

        // Test configuration extraction and reading
        context.saveDefaultConfig();
        Path expectedConfigFile = addonsDir.resolve("mock-addon").resolve("config.yml");
        assertTrue(Files.exists(expectedConfigFile));
        assertEquals(85.0, context.getConfig().getDouble("test.heat"));

        manager.unloadAddon("mock-addon");
    }

    @Test
    @DisplayName("Reject JAR addon initialization when its data folder collides with an existing directory addon")
    void testCollisionRejectionInPaperAddonManager() throws IOException {
        Path addonsDir = tempDir.resolve("addons_collision");
        Files.createDirectories(addonsDir);

        // 1. Create a directory addon named 'frost' with id 'winter'
        Path frostDir = addonsDir.resolve("frost");
        Files.createDirectories(frostDir);
        String dirManifest = """
                id: "winter"
                name: "Winter Addon"
                version: "1.0.0"
                namespace: "winter"
                """;
        Files.writeString(frostDir.resolve("addon.yml"), dirManifest, StandardCharsets.UTF_8);

        // 2. Create a JAR addon named 'frost.jar' with id 'frost' (which wants data folder 'frost')
        Path frostJar = addonsDir.resolve("frost.jar");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(frostJar.toFile()))) {
            String jarManifest = """
                    id: "frost"
                    name: "Frost Addon"
                    version: "1.0.0"
                    namespace: "frost"
                    """;
            zos.putNextEntry(new ZipEntry("addon.yml"));
            zos.write(jarManifest.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        CroppyCore core = new CroppyCore();
        core.initialize();

        PaperAddonManager manager = new PaperAddonManager(null, core, addonsDir, null);
        manager.discoverAddons();

        assertEquals(2, manager.getDiscoveredAddons().size());

        // Directory addon 'winter' loads successfully with its root as dataFolder
        assertTrue(manager.loadAddon("winter"));
        assertEquals(frostDir, manager.getDataFolder("winter").orElseThrow());

        // JAR addon 'frost' must fail to load because 'frost' dataFolder is occupied by 'winter'
        assertFalse(manager.loadAddon("frost"));
    }

    @Test
    @DisplayName("PaperAddonManager loads menus and interactions from unpacked directory addon and purges them on disable")
    void testLoadDirectoryAddonMenus() throws IOException {
        Path addonsDir = tempDir.resolve("addons_dir");
        Files.createDirectories(addonsDir);

        Path dirAddon = addonsDir.resolve("dir-addon");
        Files.createDirectories(dirAddon.resolve("menus"));
        Files.createDirectories(dirAddon.resolve("interactions"));

        String manifest = """
                id: "dir-addon"
                name: "Directory Addon"
                version: "1.0.0"
                namespace: "dir_addon"
                """;
        Files.writeString(dirAddon.resolve("addon.yml"), manifest, StandardCharsets.UTF_8);

        String menuYaml = """
                id: "dir_addon:station_menu"
                title: "<green>Station GUI</green>"
                rows: 1
                pattern:
                  - "XXXXXXXXX"
                elements:
                  'X':
                    type: DECORATION
                    item:
                      material: "minecraft:stone"
                      name: "Border"
                """;
        Files.writeString(dirAddon.resolve("menus/station_menu.yml"), menuYaml, StandardCharsets.UTF_8);

        String interactionYaml = """
                id: "dir_addon:harvest_grape"
                crop: "croppy:grapes"
                trigger: "RIGHT_CLICK"
                priority: 15
                actions:
                  - type: "HARVEST_CROP"
                """;
        Files.writeString(dirAddon.resolve("interactions/harvest_grape.yml"), interactionYaml, StandardCharsets.UTF_8);

        CroppyCore core = new CroppyCore();
        core.initialize();

        PaperAddonManager manager = new PaperAddonManager(null, core, addonsDir, null);
        manager.discoverAddons();

        assertTrue(manager.enableAddon("dir-addon"));

        PaperAddonContext context = manager.getContext("dir-addon").orElseThrow();
        assertEquals(1, context.getRegisteredMenus().size());
        assertEquals(1, context.getRegisteredInteractions().size());
        assertTrue(core.getMenuRegistry().get(Key.of("dir_addon", "station_menu")).isPresent());
        assertTrue(core.getInteractionRegistry().get(Key.of("dir_addon", "harvest_grape")).isPresent());

        // When disabled, menus and interactions must be purged from registry
        assertTrue(manager.disableAddon("dir-addon"));
        assertFalse(core.getMenuRegistry().contains(Key.of("dir_addon", "station_menu")));
        assertFalse(core.getInteractionRegistry().contains(Key.of("dir_addon", "harvest_grape")));
    }

    @Test
    @DisplayName("Invalid interaction YAML file is skipped gracefully during addon load")
    void testInvalidInteractionSkippedGracefully() throws IOException {
        Path addonsDir = tempDir.resolve("addons_invalid_interaction");
        Files.createDirectories(addonsDir);

        Path dirAddon = addonsDir.resolve("invalid-addon");
        Files.createDirectories(dirAddon.resolve("interactions"));

        String manifest = """
                id: "invalid-addon"
                name: "Invalid Addon"
                version: "1.0.0"
                namespace: "invalid_addon"
                """;
        Files.writeString(dirAddon.resolve("addon.yml"), manifest, StandardCharsets.UTF_8);

        // Missing required 'id' and 'trigger'
        String brokenYaml = """
                priority: 10
                actions:
                  - type: "NONE"
                """;
        Files.writeString(dirAddon.resolve("interactions/broken.yml"), brokenYaml, StandardCharsets.UTF_8);

        CroppyCore core = new CroppyCore();
        core.initialize();

        PaperAddonManager manager = new PaperAddonManager(null, core, addonsDir, null);
        manager.discoverAddons();

        // Should still enable without throwing uncaught exceptions
        assertTrue(manager.enableAddon("invalid-addon"));
        PaperAddonContext context = manager.getContext("invalid-addon").orElseThrow();
        assertEquals(0, context.getRegisteredInteractions().size());
    }

    @Test
    @DisplayName("Verify croppy_coffee addon station menus parse and validate successfully")
    void testCroppyCoffeeStationMenus() {
        Optional<Path> coffeeResourcesOpt = resolveCoffeeAddonResources("menus");
        if (coffeeResourcesOpt.isPresent()) {
            Path coffeeResources = coffeeResourcesOpt.get();
            dev.darkblade.croppy.core.menu.YamlMenuLoader loader = new dev.darkblade.croppy.core.menu.YamlMenuLoader();
            dev.darkblade.croppy.core.menu.DefaultMenuRegistry registry = new dev.darkblade.croppy.core.menu.DefaultMenuRegistry();
            int loaded = loader.loadAll(coffeeResources, registry, dev.darkblade.croppy.api.logging.NoOpCroppyLogger.INSTANCE);
            assertEquals(3, loaded);
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_grinder")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_roaster")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "espresso_machine")));

            var grinder = registry.get(Key.of("croppy_coffee", "coffee_grinder")).orElseThrow();
            assertEquals(1, grinder.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.INPUT).size());
            assertEquals(1, grinder.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.OUTPUT).size());
            assertEquals(1, grinder.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.STATUS).size());
            assertEquals(1, grinder.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.ACTION).size());

            var roaster = registry.get(Key.of("croppy_coffee", "coffee_roaster")).orElseThrow();
            assertEquals(2, roaster.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.INPUT).size());
            assertEquals(2, roaster.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.OUTPUT).size());

            var espresso = registry.get(Key.of("croppy_coffee", "espresso_machine")).orElseThrow();
            assertEquals(3, espresso.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.INPUT).size());
            assertEquals(1, espresso.getSlotsForType(dev.darkblade.croppy.api.menu.SlotType.OUTPUT).size());
        }
    }

    @Test
    @DisplayName("Verify croppy_coffee addon interactions parse and validate successfully")
    void testCroppyCoffeeInteractions() {
        Optional<Path> coffeeInteractionsOpt = resolveCoffeeAddonResources("interactions");
        if (coffeeInteractionsOpt.isPresent()) {
            Path coffeeInteractions = coffeeInteractionsOpt.get();
            dev.darkblade.croppy.core.interaction.YamlInteractionLoader loader = new dev.darkblade.croppy.core.interaction.YamlInteractionLoader();
            dev.darkblade.croppy.core.interaction.DefaultInteractionRegistry registry = new dev.darkblade.croppy.core.interaction.DefaultInteractionRegistry();
            int loaded = loader.loadAll(coffeeInteractions, registry, dev.darkblade.croppy.api.logging.NoOpCroppyLogger.INSTANCE);
            assertTrue(loaded >= 15, "Expected at least 15 interactions, got: " + loaded);

            // Grinder interactions
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_grinder_crank")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_grinder_serve")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_grinder_extract")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_grinder_gui")));

            // Roaster interactions
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_roaster_inspect")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_roaster_serve")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_roaster_extract")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "coffee_roaster_gui")));

            // Espresso machine interactions
            assertTrue(registry.contains(Key.of("croppy_coffee", "espresso_machine_inspect")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "espresso_machine_serve")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "espresso_machine_extract")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "espresso_machine_gui")));

            // Crop interactions
            assertTrue(registry.contains(Key.of("croppy_coffee", "harvest_coffee_cherries")));
            assertTrue(registry.contains(Key.of("croppy_coffee", "dismantle_coffee_bush")));
        }
    }

    @Test
    @DisplayName("Verify croppy_coffee addon full progression recipes and processes cycle")
    void testCroppyCoffeeRecipesAndProcessesCycle() {
        Optional<Path> recipesOpt = resolveCoffeeAddonResources("recipes");
        Optional<Path> processesOpt = resolveCoffeeAddonResources("processes");

        if (recipesOpt.isPresent() && processesOpt.isPresent()) {
            Path coffeeRecipesDir = recipesOpt.get();
            Path coffeeProcessesDir = processesOpt.get();

            dev.darkblade.croppy.core.recipe.YamlRecipeLoader recipeLoader = new dev.darkblade.croppy.core.recipe.YamlRecipeLoader();
            dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry recipeRegistry = new dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry();
            int recipesLoaded = recipeLoader.loadAll(coffeeRecipesDir, recipeRegistry, java.util.logging.Logger.getLogger("Test"));
            assertEquals(13, recipesLoaded);

            dev.darkblade.croppy.core.process.YamlProcessLoader processLoader = new dev.darkblade.croppy.core.process.YamlProcessLoader();
            dev.darkblade.croppy.core.process.DefaultProcessRegistry processRegistry = new dev.darkblade.croppy.core.process.DefaultProcessRegistry();
            int processesLoaded = processLoader.loadAll(coffeeProcessesDir, processRegistry, java.util.logging.Logger.getLogger("Test"));
            assertEquals(11, processesLoaded);

            // Verify every recipe has a corresponding registered process definition
            for (var recipe : recipeRegistry.values()) {
                assertTrue(processRegistry.contains(recipe.processKey()),
                        "Recipe " + recipe.key() + " references missing process: " + recipe.processKey());
            }

            // Verify the coffee gameplay cycle components
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "coffee_seed_depulping")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "coffee_sapling_cultivation")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "coffee_grinding_light")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "coffee_grinding_dark")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "coffee_grinding")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "espresso_extraction")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "latte_blending")));
            assertTrue(recipeRegistry.contains(Key.of("croppy_coffee", "cappuccino_blending")));
        }
    }

    private Optional<Path> resolveCoffeeAddonResources(String subdir) {
        String customBase = System.getProperty("croppy.addons.path", System.getenv("CROPPY_ADDONS_PATH"));
        if (customBase != null && !customBase.isBlank()) {
            Path customPath = Path.of(customBase, "addons", "coffee", "src", "main", "resources", subdir);
            if (Files.exists(customPath)) {
                return Optional.of(customPath);
            }
        }

        Path fromRoot = Path.of("..", "Croppy-addons", "addons", "coffee", "src", "main", "resources", subdir);
        if (Files.exists(fromRoot)) {
            return Optional.of(fromRoot);
        }

        Path fromSubmodule = Path.of("..", "..", "Croppy-addons", "addons", "coffee", "src", "main", "resources", subdir);
        if (Files.exists(fromSubmodule)) {
            return Optional.of(fromSubmodule);
        }

        return Optional.empty();
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

            // config.yml
            String configYaml = """
                    test:
                      heat: 85.0
                    """;
            zos.putNextEntry(new ZipEntry("config.yml"));
            zos.write(configYaml.getBytes(StandardCharsets.UTF_8));
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

            // menus/custom_menu.yml
            String menuYaml = """
                    id: "mock_addon:custom_menu"
                    title: "<gold>Mock Menu</gold>"
                    rows: 1
                    pattern:
                      - "#########"
                    elements:
                      '#':
                        type: DECORATION
                        item:
                          material: "minecraft:gray_stained_glass_pane"
                          name: " "
                    """;
            zos.putNextEntry(new ZipEntry("menus/custom_menu.yml"));
            zos.write(menuYaml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // interactions/stir_pot.yml
            String interactionYaml = """
                    id: "mock_addon:stir_pot"
                    station: "croppy:cooking_pot"
                    trigger: "RIGHT_CLICK"
                    match_item: "mock_addon:star_fruit"
                    priority: 50
                    actions:
                      - type: "ADVANCE_PROGRESS"
                        ticks: 20
                    """;
            zos.putNextEntry(new ZipEntry("interactions/stir_pot.yml"));
            zos.write(interactionYaml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }
}
