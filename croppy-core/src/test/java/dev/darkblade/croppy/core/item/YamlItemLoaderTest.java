package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class YamlItemLoaderTest {

    private YamlItemLoader loader;
    private DefaultItemRegistry registry;

    @BeforeEach
    void setUp() {
        loader = new YamlItemLoader();
        registry = new DefaultItemRegistry();
    }

    @Test
    @DisplayName("Load flat item definition YAML")
    void testLoadFlatYaml() {
        String yaml = """
                id: "croppy:speed_gro"
                material: "minecraft:bone_meal"
                custom-model-data: 101
                name: "<green>Speed-Gro</green>"
                lore:
                  - "<gray>Increases growth rate by 20%</gray>"
                  - "<dark_gray>Single use per crop</dark_gray>"
                pdc:
                  speed_multiplier: "1.2"
                  tier: "1"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ItemDefinition def = loader.load(in, "fertilizers", "speed_gro.yml");

        assertEquals(Key.of("croppy", "speed_gro"), def.key());
        assertEquals("fertilizers", def.category());
        assertEquals("minecraft:bone_meal", def.materialKey());
        assertEquals(101, def.customModelData());
        assertEquals("<green>Speed-Gro</green>", def.displayName());
        assertEquals(2, def.lore().size());
        assertEquals("1.2", def.pdcProperties().get("speed_multiplier"));
        assertEquals("1", def.pdcProperties().get("tier"));
    }

    @Test
    @DisplayName("Load item definition with nested 'item' map")
    void testLoadNestedItemYaml() {
        String yaml = """
                id: "copper_watering_can"
                category: "watering_cans"
                item:
                  material: "IRON_HOE"
                  custom_model_data: 2002
                  name: "<yellow>Copper Watering Can</yellow>"
                  lore:
                    - "<gray>Holds 40 charges</gray>"
                pdc:
                  capacity: "40"
                  aoe: "1x3"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ItemDefinition def = loader.load(in, "tools", "copper_can.yml");

        assertEquals(Key.of("croppy", "copper_watering_can"), def.key());
        assertEquals("watering_cans", def.category());
        assertEquals("IRON_HOE", def.materialKey());
        assertEquals(2002, def.customModelData());
        assertEquals("<yellow>Copper Watering Can</yellow>", def.displayName());
        assertEquals(1, def.lore().size());
        assertEquals("40", def.pdcProperties().get("capacity"));
    }

    @Test
    @DisplayName("Reject YAML missing material or id")
    void testMissingRequiredFields() {
        String noId = """
                material: "WHEAT_SEEDS"
                """;
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(noId.getBytes(StandardCharsets.UTF_8)), "seeds", "test.yml"));

        String noMat = """
                id: "croppy:wheat_seeds"
                """;
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(noMat.getBytes(StandardCharsets.UTF_8)), "seeds", "test.yml"));
    }
}
