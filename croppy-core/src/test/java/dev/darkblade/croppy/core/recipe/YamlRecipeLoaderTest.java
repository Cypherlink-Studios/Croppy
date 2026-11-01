package dev.darkblade.croppy.core.recipe;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class YamlRecipeLoaderTest {

    private YamlRecipeLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlRecipeLoader();
    }

    @Test
    @DisplayName("Parses complete recipe definition with hint and discovery rules")
    void testLoadCompleteRecipe() {
        String yaml = """
                id: "croppy:tomato_soup"
                name: "<yellow>Tomato Soup</yellow>"
                category: "croppy:soups"
                station: "croppy:cooking_pot"
                process: "croppy:tomato_soup_cook"
                
                inputs:
                  - item: "croppy:tomato"
                    amount: 2
                  - item: "croppy:onion"
                    amount: 1
                
                result:
                  item: "croppy:tomato_soup"
                  amount: 1
                
                discovery:
                  allow_excess_inputs: false
                  methods:
                    - "croppy:experimental"
                    - "croppy:automatic"
                  hint:
                    description: "<gray>A warm simmered red soup...</gray>"
                    revealed_ingredients:
                      - "croppy:tomato"
                    hide_result: true
                
                properties:
                  difficulty: "easy"
                """;

        InputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        RecipeDefinition recipe = loader.load(in, "tomato_soup.yml");

        assertNotNull(recipe);
        assertEquals(Key.of("croppy", "tomato_soup"), recipe.key());
        assertEquals("<yellow>Tomato Soup</yellow>", recipe.displayName());
        assertEquals(Key.of("croppy", "soups"), recipe.categoryKey());
        assertEquals(Key.of("croppy", "cooking_pot"), recipe.stationKey());
        assertEquals(Key.of("croppy", "tomato_soup_cook"), recipe.processKey());
        assertEquals(2, recipe.inputRequirements().size());
        assertEquals(Key.of("croppy", "tomato_soup"), recipe.resultItemKey());
        assertEquals(1, recipe.resultAmount());
        assertFalse(recipe.allowExcessInputs());

        assertEquals(2, recipe.enabledDiscoveryMethods().size());
        assertTrue(recipe.enabledDiscoveryMethods().contains(Key.of("croppy", "experimental")));

        assertEquals("<gray>A warm simmered red soup...</gray>", recipe.hint().description());
        assertEquals(1, recipe.hint().revealedIngredientKeys().size());
        assertTrue(recipe.hint().hideResultItem());
        assertEquals("easy", recipe.properties().get("difficulty"));
    }

    @Test
    @DisplayName("Parses minimal recipe with fallback defaults")
    void testLoadMinimalRecipe() {
        String yaml = """
                id: "croppy:simple_bread"
                name: "Simple Bread"
                station: "croppy:oven"
                inputs:
                  - item: "croppy:flour"
                    amount: 3
                """;

        InputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        RecipeDefinition recipe = loader.load(in, "bread.yml");

        assertNotNull(recipe);
        assertEquals(Key.of("croppy", "simple_bread"), recipe.key());
        assertEquals(Key.of("croppy", "all"), recipe.categoryKey());
        assertEquals(Key.of("croppy", "simple_bread"), recipe.processKey());
        assertEquals(Key.of("croppy", "simple_bread"), recipe.resultItemKey());
        assertEquals(1, recipe.resultAmount());
    }

    @Test
    @DisplayName("Throws exception on missing required station or id")
    void testMissingRequiredFields() {
        String noStationYaml = """
                id: "croppy:broken"
                name: "Broken"
                """;

        InputStream in = new ByteArrayInputStream(noStationYaml.getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> loader.load(in, "broken.yml"));
    }

    @Test
    @DisplayName("Walks directory and loads multiple YAML files")
    void testLoadAll(@TempDir Path tempDir) throws Exception {
        Path recipe1 = tempDir.resolve("soup.yml");
        Path recipe2 = tempDir.resolve("bread.yml");

        Files.writeString(recipe1, """
                id: "croppy:soup"
                name: "Soup"
                station: "croppy:pot"
                """);

        Files.writeString(recipe2, """
                id: "croppy:bread"
                name: "Bread"
                station: "croppy:oven"
                """);

        DefaultRecipeRegistry registry = new DefaultRecipeRegistry();
        Logger logger = Logger.getLogger(getClass().getName());

        int count = loader.loadAll(tempDir, registry, logger);
        assertEquals(2, count);
        assertEquals(2, registry.size());
        assertTrue(registry.contains(Key.of("croppy", "soup")));
        assertTrue(registry.contains(Key.of("croppy", "bread")));
    }
}
