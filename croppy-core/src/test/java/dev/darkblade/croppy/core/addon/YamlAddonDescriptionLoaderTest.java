package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlAddonDescriptionLoaderTest {

    private YamlAddonDescriptionLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlAddonDescriptionLoader();
    }

    private ByteArrayInputStream yamlStream(String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Load valid declarative content pack manifest without main")
    void testLoadDeclarativePack() {
        String yaml = """
                id: exotic-fruits
                name: "Exotic Fruits Pack"
                version: "1.2.0"
                namespace: exotic_fruits
                description: "Adds dragonfruit and mangoes"
                author: "FarmerJoe"
                dependencies:
                  - croppy
                """;

        AddonDescription desc = loader.load(yamlStream(yaml), "test.yml");
        assertEquals("exotic-fruits", desc.id());
        assertEquals("Exotic Fruits Pack", desc.name());
        assertEquals("1.2.0", desc.version());
        assertEquals("exotic_fruits", desc.namespace());
        assertTrue(desc.main().isEmpty());
        assertFalse(desc.isCodeAddon());
        assertEquals(List.of("FarmerJoe"), desc.authors());
        assertEquals("Adds dragonfruit and mangoes", desc.description());
        assertEquals(List.of("croppy"), desc.dependencies());
        assertTrue(desc.softDependencies().isEmpty());
    }

    @Test
    @DisplayName("Load valid Java code addon manifest with main and softDependencies")
    void testLoadCodeAddon() {
        String yaml = """
                id: arcane-farming
                name: "Arcane Farming"
                version: "2.0.0"
                namespace: arcane
                main: "dev.darkblade.croppy.arcane.ArcaneAddon"
                authors:
                  - DarkBladeDev
                  - ContributorOne
                description: "Magical crops and altars"
                dependencies:
                  - croppy
                softDependencies:
                  - exotic-fruits
                """;

        AddonDescription desc = loader.load(yamlStream(yaml), "test.yml");
        assertEquals("arcane-farming", desc.id());
        assertTrue(desc.isCodeAddon());
        assertEquals("dev.darkblade.croppy.arcane.ArcaneAddon", desc.main().orElseThrow());
        assertEquals(List.of("DarkBladeDev", "ContributorOne"), desc.authors());
        assertEquals(List.of("croppy"), desc.dependencies());
        assertEquals(List.of("exotic-fruits"), desc.softDependencies());
    }

    @Test
    @DisplayName("Reject manifest when mandatory fields are missing")
    void testMissingMandatoryFields() {
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(yamlStream("name: Foo\nversion: 1.0\nnamespace: foo"), "test.yml"));

        assertThrows(IllegalArgumentException.class, () ->
                loader.load(yamlStream("id: foo\nversion: 1.0\nnamespace: foo"), "test.yml"));

        assertThrows(IllegalArgumentException.class, () ->
                loader.load(yamlStream("id: foo\nname: Foo\nnamespace: foo"), "test.yml"));

        assertThrows(IllegalArgumentException.class, () ->
                loader.load(yamlStream("id: foo\nname: Foo\nversion: 1.0"), "test.yml"));
    }

    @Test
    @DisplayName("Reject invalid id or namespace characters")
    void testInvalidIdentifierFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(yamlStream("id: Invalid Name\nname: Foo\nversion: 1.0\nnamespace: foo"), "test.yml"));

        assertThrows(IllegalArgumentException.class, () ->
                loader.load(yamlStream("id: valid-id\nname: Foo\nversion: 1.0\nnamespace: INVALID!"), "test.yml"));
    }
}
