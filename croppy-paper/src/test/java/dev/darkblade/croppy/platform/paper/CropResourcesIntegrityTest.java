package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.core.interaction.DefaultInteractionRegistry;
import dev.darkblade.croppy.core.interaction.YamlInteractionLoader;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.item.YamlItemLoader;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CropResourcesIntegrityTest {

    @Test
    @DisplayName("Verify all bundled crop definitions parse cleanly for neutral starter pack")
    void testBundledCropsIntegrity() {
        Path cropsDir = Paths.get("src", "main", "resources", "crops");
        assertTrue(Files.isDirectory(cropsDir), "crops resource directory must exist");

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        List<Species> loaded = loader.loadDirectory(cropsDir, Logger.getAnonymousLogger());

        assertEquals(5, loaded.size(), "Starter pack crops must contain exactly 5 neutral crops (wheat, carrot, potato, tomato, rice)");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:wheat")), "Must contain croppy:wheat");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:carrot")), "Must contain croppy:carrot");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:potato")), "Must contain croppy:potato");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:tomato")), "Must contain croppy:tomato");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:rice")), "Must contain croppy:rice");
        assertTrue(loaded.stream().noneMatch(s -> s.getKey().asString().equals("croppy:grapes")), "Must not contain croppy:grapes in neutral starter pack");

        // Verify rice properties
        Species rice = loaded.stream().filter(s -> s.getKey().asString().equals("croppy:rice")).findFirst().orElseThrow();
        assertEquals(2, rice.getHeight());
        assertEquals("minecraft:water", rice.getDisplay().blockId());
    }

    @Test
    @DisplayName("Verify all bundled interaction definitions load cleanly")
    void testBundledInteractionsIntegrity() {
        Path interactionsDir = Paths.get("src", "main", "resources", "interactions");
        assertTrue(Files.isDirectory(interactionsDir), "interactions resource directory must exist");

        DefaultInteractionRegistry registry = new DefaultInteractionRegistry();
        YamlInteractionLoader loader = new YamlInteractionLoader();
        int count = loader.loadAll(interactionsDir, registry, Logger.getAnonymousLogger());

        assertTrue(count > 0, "Must load interactions");
        assertFalse(registry.getInteractionsForCrop(dev.darkblade.croppy.api.identifier.Key.of("croppy", "rice")).isEmpty(), "Must have rice interactions");
    }

    @Test
    @DisplayName("Verify all bundled items parse cleanly and have companion resource pack assets")
    void testBundledItemsIntegrity() {
        Path itemsDir = Paths.get("src", "main", "resources", "items");
        assertTrue(Files.isDirectory(itemsDir), "items resource directory must exist");

        DefaultItemRegistry registry = new DefaultItemRegistry();
        YamlItemLoader loader = new YamlItemLoader();
        int count = loader.loadAllCategories(itemsDir, Logger.getAnonymousLogger(), registry);

        assertEquals(26, count, "Starter pack must contain exactly 26 neutral items");
        assertTrue(registry.get(Key.of("croppy", "green_grapes")).isEmpty(), "Must not contain croppy:green_grapes in base");
        assertTrue(registry.get(Key.of("croppy", "purple_grapes")).isEmpty(), "Must not contain croppy:purple_grapes in base");
        assertTrue(registry.get(Key.of("croppy", "grapes")).isEmpty(), "Must not contain croppy:grapes in base");
        assertTrue(registry.get(Key.of("croppy", "grape_seeds")).isEmpty(), "Must not contain croppy:grape_seeds in base");

        // Verify that EVERY bundled item has its companion resource pack JSON definition
        for (ItemDefinition def : registry.values()) {
            assertNotNull(def.itemModel(), "Item " + def.key() + " must define itemModel");
            Path itemJson = Paths.get("..", "resourcepack", "assets", "croppy", "items", def.itemModel().value() + ".json");
            if (!Files.exists(itemJson)) {
                itemJson = Paths.get("resourcepack", "assets", "croppy", "items", def.itemModel().value() + ".json");
            }
            assertTrue(Files.exists(itemJson), "Item JSON definition must exist for " + def.key() + " at " + itemJson);
        }
    }

    @Test
    @DisplayName("Verify companion resource pack strict JSON compliance and model linkage")
    void testResourcePackIntegrity() throws Exception {
        Path rpRoot = Paths.get("..", "resourcepack", "assets", "croppy");
        if (!Files.exists(rpRoot)) {
            rpRoot = Paths.get("resourcepack", "assets", "croppy");
        }
        assertTrue(Files.isDirectory(rpRoot), "Resource pack assets directory must exist at " + rpRoot.toAbsolutePath());

        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

        try (java.util.stream.Stream<Path> stream = Files.walk(rpRoot)) {
            List<Path> jsonFiles = stream.filter(p -> p.toString().endsWith(".json")).toList();
            assertFalse(jsonFiles.isEmpty(), "Resource pack must contain JSON files");

            for (Path jsonFile : jsonFiles) {
                // Verify lowercase resource location compliance
                String relPath = rpRoot.relativize(jsonFile).toString().replace('\\', '/');
                assertTrue(relPath.matches("^[a-z0-9_./-]+$"), "Asset path must be lowercase: " + relPath);

                // Verify strict JSON parsing
                String content = Files.readString(jsonFile);
                assertFalse(content.matches("(?s).*,\\s*[}\\]].*"), "No trailing commas allowed in " + relPath);
                try {
                    assertNotNull(yaml.load(content.replace("\t", "  ")), "Must parse as valid JSON/YAML: " + relPath);
                } catch (Exception e) {
                    fail("Failed to parse JSON file " + relPath + ": " + e.getMessage());
                }
            }
        }
    }

    @Test
    @DisplayName("Verify CroppyPlugin extracts all bundled resource YAMLs on startup")
    void testPluginRuntimeResourceExtractionSynchronized() throws Exception {
        Path resourcesRoot = Paths.get("src", "main", "resources");
        if (!Files.exists(resourcesRoot)) {
            resourcesRoot = Paths.get("croppy-paper", "src", "main", "resources");
        }
        assertTrue(Files.isDirectory(resourcesRoot), "Resources directory must exist");

        Path pluginSource = Paths.get("src", "main", "java", "dev", "darkblade", "croppy", "platform", "paper", "CroppyPlugin.java");
        if (!Files.exists(pluginSource)) {
            pluginSource = Paths.get("croppy-paper", "src", "main", "java", "dev", "darkblade", "croppy", "platform", "paper", "CroppyPlugin.java");
        }
        assertTrue(Files.exists(pluginSource), "CroppyPlugin.java source must exist");

        String pluginCode = Files.readString(pluginSource);

        try (java.util.stream.Stream<Path> stream = Files.walk(resourcesRoot)) {
            List<Path> ymlFiles = stream
                    .filter(p -> p.toString().endsWith(".yml"))
                    .toList();

            for (Path yml : ymlFiles) {
                String relativePath = resourcesRoot.relativize(yml).toString().replace('\\', '/');
                if (relativePath.equals("paper-plugin.yml") || relativePath.equals("config.yml")) {
                    continue; // standard metadata and Bukkit config
                }

                String expectedCall = "saveResourceIfNotExists(\"" + relativePath + "\")";
                assertTrue(
                        pluginCode.contains(expectedCall),
                        "CroppyPlugin must extract default resource: " + expectedCall
                );
            }
        }
    }
}
