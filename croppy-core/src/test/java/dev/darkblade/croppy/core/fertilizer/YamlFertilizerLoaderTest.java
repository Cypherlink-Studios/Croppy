package dev.darkblade.croppy.core.fertilizer;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlFertilizerLoaderTest {

    private final YamlFertilizerLoader loader = new YamlFertilizerLoader();

    @Test
    @DisplayName("Should load valid speed fertilizer YAML")
    void testLoadSpeedFertilizer() {
        String yaml = """
            id: "croppy:growth_fertilizer"
            name: "<green>Growth Fertilizer</green>"
            description:
              - "<gray>Accelerates growth by 25%</gray>"
            type: SPEED
            speed_multiplier: 1.25
            item:
              material: BONE_MEAL
              custom_model_data: 1001
        """;

        Fertilizer f = loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "growth_fertilizer.yml");

        assertEquals(Key.of("croppy", "growth_fertilizer"), f.getKey());
        assertEquals("<green>Growth Fertilizer</green>", f.name());
        assertEquals(List.of("<gray>Accelerates growth by 25%</gray>"), f.description());
        assertEquals(FertilizerType.SPEED, f.type());
        assertEquals(1.25, f.speedMultiplier(), 0.001);
        assertEquals(0.0, f.retentionChance());
        assertEquals("BONE_MEAL", f.material());
        assertEquals(1001, f.customModelData());
    }

    @Test
    @DisplayName("Should load valid retaining soil YAML")
    void testLoadRetainingSoil() {
        String yaml = """
            id: "croppy:hydrating_fertilizer"
            name: "<aqua>Hydrating Fertilizer</aqua>"
            type: RETAINING
            retention_chance: 0.50
            item:
              material: CLAY_BALL
        """;

        Fertilizer f = loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "hydrating_fertilizer.yml");

        assertEquals(Key.of("croppy", "hydrating_fertilizer"), f.getKey());
        assertEquals(FertilizerType.RETAINING, f.type());
        assertEquals(0.50, f.retentionChance(), 0.001);
        assertEquals("CLAY_BALL", f.material());
        assertEquals(0, f.customModelData());
    }

    @Test
    @DisplayName("Should reject invalid YAML without id or invalid type")
    void testInvalidYaml() {
        String noId = """
            name: "Test"
            type: SPEED
        """;
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(noId.getBytes(StandardCharsets.UTF_8)), "bad.yml"));

        String badType = """
            id: "croppy:bad"
            name: "Bad"
            type: NON_EXISTENT
        """;
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(badType.getBytes(StandardCharsets.UTF_8)), "bad2.yml"));
    }
}
