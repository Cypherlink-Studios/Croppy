package dev.darkblade.croppy.core.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class YamlStationLoaderTest {

    private final YamlStationLoader loader = new YamlStationLoader();

    @Test
    void testLoadValidStation() {
        String yaml = """
                id: "croppy:keg"
                name: "<gold>Artisan Keg</gold>"
                block: "minecraft:barrel"
                interaction: "PHYSICAL"
                input_capacity: 2
                output_capacity: 1
                menu: "croppy:keg_menu"
                display: true
                allowed_recipes:
                  - "croppy:wine"
                  - "croppy:beer"
                properties:
                  animation_speed: "fast"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "test-keg.yml");

        assertEquals(Key.of("croppy", "keg"), def.key());
        assertEquals("<gold>Artisan Keg</gold>", def.displayName());
        assertEquals("minecraft:barrel", def.baseBlockMaterial());
        assertEquals(StationInteractionType.PHYSICAL, def.interactionType());
        assertEquals(2, def.inputCapacity());
        assertEquals(1, def.outputCapacity());
        assertEquals(Key.of("croppy", "keg_menu"), def.menuKey());
        assertTrue(def.hasDisplay());
        assertEquals(2, def.allowedRecipes().size());
        assertTrue(def.allowedRecipes().contains(Key.of("croppy", "wine")));
        assertEquals("fast", def.properties().get("animation_speed"));
    }

    @Test
    void testLoadHeatSourceStation() {
        String yaml = """
                id: "croppy:stove"
                name: "<gradient:#ff8800:#ff3300>Rustic Stove</gradient>"
                block: "minecraft:smoker"
                interaction: "HYBRID"
                input_capacity: 1
                output_capacity: 0
                menu: "croppy:stove_menu"
                has_display: false
                properties:
                  heat_source: "true"
                  min_temperature: "20.0"
                  max_temperature: "350.0"
                  heating_rate: "5.0"
                  cooling_rate: "2.0"
                  fuel_burn_multiplier: "1.0"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "test-stove.yml");

        assertEquals(Key.of("croppy", "stove"), def.key());
        assertEquals("true", def.properties().get("heat_source"));
        assertEquals("20.0", def.properties().get("min_temperature"));
        assertEquals("350.0", def.properties().get("max_temperature"));
        assertEquals("5.0", def.properties().get("heating_rate"));
        assertEquals("2.0", def.properties().get("cooling_rate"));
        assertEquals("1.0", def.properties().get("fuel_burn_multiplier"));
    }

    @Test
    void testDefaultMenuKeyWhenOmitted() {
        String yaml = """
                id: "croppy:pot"
                name: "Cooking Pot"
                block: "cauldron"
                """;
        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "test-pot.yml");
        assertEquals(StationDefinition.DEFAULT_MENU_KEY, def.menuKey());
    }

    @Test
    void testLoadMissingIdThrowsException() {
        String yaml = """
                name: "Unknown Station"
                block: "barrel"
                """;
        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> loader.load(in, "invalid.yml"));
    }

    @Test
    void testLoadAllFromDirectory(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("pot.yml");
        Path file2 = tempDir.resolve("jar.yaml");

        Files.writeString(file1, """
                id: "croppy:cooking_pot"
                name: "Cooking Pot"
                block: "cauldron"
                interaction: "GUI"
                """);

        Files.writeString(file2, """
                id: "croppy:preserves_jar"
                name: "Preserves Jar"
                block: "glass"
                """);

        DefaultStationRegistry registry = new DefaultStationRegistry();
        int loaded = loader.loadAll(tempDir, registry, Logger.getLogger("test"));

        assertEquals(2, loaded);
        assertTrue(registry.get(Key.of("croppy", "cooking_pot")).isPresent());
        assertTrue(registry.get(Key.of("croppy", "preserves_jar")).isPresent());
    }

    @Test
    void testLoadStationWithVisualDefinition() {
        String yaml = """
                id: "croppy:cooking_pot"
                name: "<gold>Cooking Pot</gold>"
                block: "cauldron"
                visual:
                  base_model:
                    enabled: true
                    type: "ITEM"
                    item: "croppy:pot_model"
                    translation: [0.0, 0.5, 0.0]
                    scale: [1.0, 1.0, 1.0]
                    view_distance: 48
                  process_display:
                    enabled: true
                    type: "ITEM"
                    translation: [0.0, 0.35, 0.0]
                    scale: [0.55, 0.15, 0.55]
                    view_distance: 24
                    interpolation_duration: 5
                  text_display:
                    enabled: true
                    billboard: "CENTER"
                    translation: [0.0, 1.35, 0.0]
                    view_distance: 8
                    format: "<yellow>{recipe_name}</yellow>"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "test-visual-pot.yml");

        assertTrue(def.getVisualDefinition().isPresent());
        var visual = def.getVisualDefinition().get();

        assertTrue(visual.getBaseModel().isPresent());
        var base = visual.getBaseModel().get();
        assertTrue(base.enabled());
        assertEquals("ITEM", base.type());
        assertEquals(Key.of("croppy", "pot_model"), base.getItemKey().orElse(null));
        assertEquals(48, base.viewDistance());
        assertEquals(0.5, base.transform().translationY(), 0.0001);

        assertTrue(visual.getProcessDisplay().isPresent());
        var process = visual.getProcessDisplay().get();
        assertEquals(24, process.viewDistance());
        assertEquals(5, process.interpolationDuration());
        assertEquals(0.55f, process.transform().scaleX(), 0.0001f);

        assertTrue(visual.getTextDisplay().isPresent());
        var text = visual.getTextDisplay().get();
        assertEquals(8, text.viewDistance());
        assertEquals("<yellow>{recipe_name}</yellow>", text.format());
        assertEquals(dev.darkblade.croppy.api.station.visual.BillboardMode.CENTER, text.billboard());
    }
}
