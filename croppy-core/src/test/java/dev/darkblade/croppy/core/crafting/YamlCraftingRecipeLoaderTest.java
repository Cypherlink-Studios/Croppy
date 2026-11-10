package dev.darkblade.croppy.core.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.CraftingType;
import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class YamlCraftingRecipeLoaderTest {

    private YamlCraftingRecipeLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlCraftingRecipeLoader();
    }

    @Test
    @DisplayName("Parse valid shaped crafting recipe YAML")
    void parseValidShapedRecipe() {
        String yaml = """
                id: "croppy:cooking_pot"
                type: "shaped"
                group: "croppy_stations"
                category: "misc"
                result:
                  item: "croppy:cooking_pot"
                  amount: 1
                shape:
                  - "I I"
                  - "ICI"
                  - " I "
                ingredients:
                  I: "minecraft:iron_ingot"
                  C: "minecraft:cauldron"
                """;

        CraftingRecipe recipe = loader.load(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                "cooking_pot.yml"
        );

        assertNotNull(recipe);
        assertEquals(Key.of("croppy", "cooking_pot"), recipe.getKey());
        assertEquals(CraftingType.SHAPED, recipe.type());
        assertEquals("croppy_stations", recipe.group());
        assertEquals("misc", recipe.category());
        assertEquals(Key.of("croppy", "cooking_pot"), recipe.resultItemKey());
        assertEquals(1, recipe.resultAmount());
        assertEquals(3, recipe.shape().size());
        assertEquals(2, recipe.shapedIngredients().size());
        assertEquals("minecraft:iron_ingot", recipe.shapedIngredients().get('I').identifier());
        assertEquals("minecraft:cauldron", recipe.shapedIngredients().get('C').identifier());
    }

    @Test
    @DisplayName("Parse valid shapeless crafting recipe YAML with string and map ingredients")
    void parseValidShapelessRecipe() {
        String yaml = """
                id: "croppy:growth_fertilizer"
                type: "shapeless"
                group: "croppy_fertilizers"
                category: "misc"
                result:
                  item: "croppy:growth_fertilizer"
                  amount: 2
                ingredients:
                  - item: "minecraft:bone_meal"
                    amount: 1
                  - "minecraft:wheat"
                """;

        CraftingRecipe recipe = loader.load(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                "growth_fertilizer.yml"
        );

        assertNotNull(recipe);
        assertEquals(Key.of("croppy", "growth_fertilizer"), recipe.getKey());
        assertEquals(CraftingType.SHAPELESS, recipe.type());
        assertEquals(2, recipe.resultAmount());
        assertEquals(2, recipe.shapelessIngredients().size());
        assertEquals("minecraft:bone_meal", recipe.shapelessIngredients().get(0).identifier());
        assertEquals(1, recipe.shapelessIngredients().get(0).amount());
        assertEquals("minecraft:wheat", recipe.shapelessIngredients().get(1).identifier());
        assertEquals(1, recipe.shapelessIngredients().get(1).amount());
    }

    @Test
    @DisplayName("Deduce shaped type when type attribute is omitted but shape is present")
    void deduceShapedType() {
        String yaml = """
                id: "croppy:skillet"
                result: "croppy:skillet"
                shape:
                  - " I "
                  - " I "
                  - " S "
                ingredients:
                  I: "minecraft:iron_ingot"
                  S: "minecraft:stick"
                """;

        CraftingRecipe recipe = loader.load(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                "skillet.yml"
        );

        assertEquals(CraftingType.SHAPED, recipe.type());
        assertEquals(Key.of("croppy", "skillet"), recipe.resultItemKey());
        assertEquals(1, recipe.resultAmount());
    }

    @Test
    @DisplayName("Reject recipe without mandatory id")
    void rejectMissingId() {
        String yaml = """
                type: "shapeless"
                ingredients:
                  - "minecraft:dirt"
                """;

        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "invalid.yml")
        );
    }

    @Test
    @DisplayName("Reject shaped ingredient with multi-character symbol")
    void rejectMultiCharSymbol() {
        String yaml = """
                id: "croppy:invalid"
                type: "shaped"
                shape:
                  - "II"
                ingredients:
                  II: "minecraft:iron_ingot"
                """;

        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "invalid.yml")
        );
    }

    @Test
    @DisplayName("loadAll discovers recipes recursively across nested directories")
    void loadAllDiscoversRecursively(@TempDir Path tempDir) throws IOException {
        Path stationsDir = tempDir.resolve("stations");
        Files.createDirectories(stationsDir);

        String potYaml = """
                id: "croppy:cooking_pot"
                type: "shaped"
                shape:
                  - "I I"
                  - "ICI"
                  - " I "
                ingredients:
                  I: "minecraft:iron_ingot"
                  C: "minecraft:cauldron"
                """;
        Files.writeString(stationsDir.resolve("cooking_pot.yml"), potYaml);

        Path fertDir = tempDir.resolve("fertilizers");
        Files.createDirectories(fertDir);

        String fertYaml = """
                id: "croppy:growth_fertilizer"
                type: "shapeless"
                ingredients:
                  - "minecraft:bone_meal"
                """;
        Files.writeString(fertDir.resolve("fert.yml"), fertYaml);

        // Corrupted file
        Files.writeString(tempDir.resolve("broken.yml"), "not a yaml map: [");

        DefaultCraftingRecipeRegistry registry = new DefaultCraftingRecipeRegistry();
        int loaded = loader.loadAll(tempDir, registry, Logger.getLogger("test"));

        assertEquals(2, loaded);
        assertEquals(2, registry.size());
        assertTrue(registry.contains(Key.of("croppy", "cooking_pot")));
        assertTrue(registry.contains(Key.of("croppy", "growth_fertilizer")));
    }
}
