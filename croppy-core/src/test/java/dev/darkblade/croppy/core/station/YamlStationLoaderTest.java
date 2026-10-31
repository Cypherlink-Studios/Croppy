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
        assertTrue(def.hasDisplay());
        assertEquals(2, def.allowedRecipes().size());
        assertTrue(def.allowedRecipes().contains(Key.of("croppy", "wine")));
        assertEquals("fast", def.properties().get("animation_speed"));
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
}
