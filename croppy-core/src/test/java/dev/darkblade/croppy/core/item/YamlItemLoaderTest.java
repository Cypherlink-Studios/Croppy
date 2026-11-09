package dev.darkblade.croppy.core.item;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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
                id: "croppy:growth_fertilizer"
                material: "minecraft:bone_meal"
                custom-model-data: 101
                name: "<green>Growth Fertilizer</green>"
                lore:
                  - "<gray>Increases growth rate by 20%</gray>"
                  - "<dark_gray>Single use per crop</dark_gray>"
                pdc:
                  speed_multiplier: "1.2"
                  tier: "1"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ItemDefinition def = loader.load(in, "fertilizers", "growth_fertilizer.yml");

        assertEquals(Key.of("croppy", "growth_fertilizer"), def.key());
        assertEquals("fertilizers", def.category());
        assertEquals("minecraft:bone_meal", def.materialKey());
        assertEquals(101, def.customModelData());
        assertNull(def.itemModel());
        assertTrue(def.getItemModel().isEmpty());
        assertEquals("<green>Growth Fertilizer</green>", def.displayName());
        assertEquals(2, def.lore().size());
        assertEquals("1.2", def.pdcProperties().get("speed_multiplier"));
        assertEquals("1", def.pdcProperties().get("tier"));
    }

    @Test
    @DisplayName("Load flat item definition with item-model")
    void testLoadFlatYamlWithItemModel() {
        String yaml = """
                id: "croppy:growth_fertilizer"
                material: "minecraft:bone_meal"
                item-model: "croppy:growth_fertilizer"
                name: "<green>Growth Fertilizer</green>"
                lore:
                  - "<gray>Increases growth rate</gray>"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ItemDefinition def = loader.load(in, "fertilizers", "growth_fertilizer.yml");

        assertEquals(Key.of("croppy", "growth_fertilizer"), def.key());
        assertEquals(0, def.customModelData());
        assertEquals(Key.of("croppy", "growth_fertilizer"), def.itemModel());
        assertTrue(def.getItemModel().isPresent());
        assertEquals(Key.of("croppy", "growth_fertilizer"), def.getItemModel().get());
    }

    @Test
    @DisplayName("Load item definition with both custom-model-data and item_model (underscore variant)")
    void testLoadBothCustomModelDataAndItemModel() {
        String yaml = """
                id: "copper_watering_can"
                category: "watering_cans"
                material: "COPPER_INGOT"
                custom_model_data: 2002
                item_model: "croppy:copper_can"
                name: "<yellow>Copper Watering Can</yellow>"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ItemDefinition def = loader.load(in, "tools", "copper_can.yml");

        assertEquals(Key.of("croppy", "copper_watering_can"), def.key());
        assertEquals(2002, def.customModelData());
        assertEquals(Key.of("croppy", "copper_can"), def.itemModel());
        assertTrue(def.getItemModel().isPresent());
    }

    @Test
    @DisplayName("Load item definition with nested 'item' map including item-model")
    void testLoadNestedItemYaml() {
        String yaml = """
                id: "copper_watering_can"
                category: "watering_cans"
                item:
                  material: "IRON_HOE"
                  custom_model_data: 2002
                  item-model: "croppy:copper_watering_can"
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
        assertEquals(Key.of("croppy", "copper_watering_can"), def.itemModel());
        assertEquals("<yellow>Copper Watering Can</yellow>", def.displayName());
        assertEquals(1, def.lore().size());
        assertEquals("40", def.pdcProperties().get("capacity"));
    }

    @Test
    @DisplayName("Load item definition with unqualified item-model defaults to croppy namespace")
    void testUnqualifiedItemModelDefaultsToCroppy() {
        String yaml = """
                id: "copper_can"
                material: "COPPER_INGOT"
                item-model: "copper_can"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ItemDefinition def = loader.load(in, "tools", "copper_can.yml");

        assertEquals(Key.of("croppy", "copper_can"), def.itemModel());
    }

    @Test
    @DisplayName("Load item definition with flat texture and nested skull-texture")
    void testLoadTextureFields() {
        String base64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjIn19fQ==";
        String flatYaml = """
                id: "croppy:cooking_pot"
                material: "minecraft:player_head"
                texture: "%s"
                name: "Cooking Pot"
                """.formatted(base64);

        ItemDefinition def1 = loader.load(new ByteArrayInputStream(flatYaml.getBytes(StandardCharsets.UTF_8)), "stations", "cooking_pot.yml");
        assertEquals(base64, def1.skullTexture());
        assertTrue(def1.getSkullTexture().isPresent());

        String url = "http://textures.minecraft.net/texture/1234567890abcdef";
        String nestedYaml = """
                id: "skillet"
                item:
                  material: "player_head"
                  skull-texture: "%s"
                """.formatted(url);

        ItemDefinition def2 = loader.load(new ByteArrayInputStream(nestedYaml.getBytes(StandardCharsets.UTF_8)), "stations", "skillet.yml");
        assertEquals(url, def2.skullTexture());
        assertTrue(def2.getSkullTexture().isPresent());
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
