package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class YamlSpeciesLoaderTest {

    private YamlSpeciesLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlSpeciesLoader();
    }

    private Species loadFromYaml(String yaml) {
        return loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "test.yml");
    }

    @Test
    @DisplayName("Parse valid wheat species definition")
    void testParseValidWheat() {
        String yaml = """
                id: "croppy:wheat"
                name: "Trigo"
                display:
                  type: "vanilla"
                  block: "minecraft:wheat"
                  seed_item: "minecraft:wheat_seeds"
                stages:
                  - id: "seed"
                    progress_required: 100
                    display:
                      block_age: 0
                  - id: "sprout"
                    progress_required: 100
                    display:
                      block_age: 2
                  - id: "growing"
                    progress_required: 150
                    display:
                      block_age: 5
                  - id: "mature"
                    progress_required: 0
                    display:
                      block_age: 7
                requirements:
                  min_light: 9
                  water_fluid_level: 1
                harvest:
                  yield:
                    - item: "minecraft:wheat"
                      min: 1
                      max: 1
                    - item: "minecraft:wheat_seeds"
                      min: 1
                      max: 3
                """;

        Species species = loadFromYaml(yaml);

        assertNotNull(species);
        assertEquals("croppy:wheat", species.getKey().asString());
        assertEquals("Trigo", species.getName());
        assertEquals("vanilla", species.getDisplay().type());
        assertEquals("minecraft:wheat", species.getDisplay().blockId());
        assertEquals("minecraft:wheat_seeds", species.getDisplay().seedItemId());

        assertEquals(4, species.getTotalStages());
        assertEquals("seed", species.getStage(0).id());
        assertEquals(100.0, species.getStage(0).progressRequired());
        assertEquals(0, species.getStage(0).blockAge());

        assertEquals("mature", species.getStage(3).id());
        assertEquals(0.0, species.getStage(3).progressRequired());
        assertEquals(7, species.getStage(3).blockAge());
        assertTrue(species.isMature(3));
        assertFalse(species.isMature(2));

        assertEquals(9, species.getRequirements().minLight());
        assertEquals(1, species.getRequirements().waterFluidLevel());
        assertTrue(species.getRequirements().requiresWater());

        assertEquals(2, species.getHarvest().yields().size());
        assertEquals("minecraft:wheat", species.getHarvest().yields().get(0).itemId());
        assertEquals(1, species.getHarvest().yields().get(0).min());
        assertEquals(1, species.getHarvest().yields().get(0).max());
        assertEquals("minecraft:wheat_seeds", species.getHarvest().yields().get(1).itemId());
        assertEquals(1, species.getHarvest().yields().get(1).min());
        assertEquals(3, species.getHarvest().yields().get(1).max());
    }

    @Test
    @DisplayName("Parse dry-farming crop without water requirement")
    void testDryFarmingCrop() {
        String yaml = """
                id: "croppy:cactus_crop"
                name: "Cactus"
                display:
                  type: "vanilla"
                  block: "minecraft:cactus"
                  seed_item: "minecraft:cactus"
                stages:
                  - id: "sprout"
                    progress_required: 50
                    display:
                      block_age: 0
                  - id: "mature"
                    progress_required: 0
                    display:
                      block_age: 1
                requirements:
                  min_light: 8
                  water_fluid_level: 0
                """;

        Species species = loadFromYaml(yaml);
        assertEquals(0, species.getRequirements().waterFluidLevel());
        assertFalse(species.getRequirements().requiresWater());
    }

    @Test
    @DisplayName("Reject species without id")
    void testRejectMissingId() {
        String yaml = """
                name: "No ID"
                display:
                  block: "minecraft:wheat"
                  seed_item: "minecraft:wheat_seeds"
                stages:
                  - id: "mature"
                    progress_required: 0
                """;
        assertThrows(IllegalArgumentException.class, () -> loadFromYaml(yaml));
    }

    @Test
    @DisplayName("Reject species with duplicate stage id")
    void testRejectDuplicateStageId() {
        String yaml = """
                id: "croppy:test"
                display:
                  block: "minecraft:wheat"
                  seed_item: "minecraft:wheat_seeds"
                stages:
                  - id: "stage1"
                    progress_required: 50
                  - id: "stage1"
                    progress_required: 0
                """;
        assertThrows(IllegalArgumentException.class, () -> loadFromYaml(yaml));
    }

    @Test
    @DisplayName("Reject invalid water fluid level greater than 8")
    void testRejectInvalidFluidLevel() {
        String yaml = """
                id: "croppy:test"
                display:
                  block: "minecraft:wheat"
                  seed_item: "minecraft:wheat_seeds"
                stages:
                  - id: "stage1"
                    progress_required: 0
                requirements:
                  water_fluid_level: 9
                """;
        assertThrows(IllegalArgumentException.class, () -> loadFromYaml(yaml));
    }
}
