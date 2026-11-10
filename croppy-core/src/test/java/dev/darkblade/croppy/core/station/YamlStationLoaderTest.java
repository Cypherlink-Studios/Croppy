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
    void testLoadRotationMode() {
        String yamlDefault = """
                id: "croppy:keg"
                block: "barrel"
                """;
        StationDefinition def1 = loader.load(new ByteArrayInputStream(yamlDefault.getBytes(StandardCharsets.UTF_8)), "keg.yml");
        assertEquals(dev.darkblade.croppy.api.species.RotationMode.CARDINAL_4, def1.rotationMode());

        String yamlFreeYaw = """
                id: "croppy:skillet"
                block: "iron_block"
                rotation_mode: "FREE_YAW"
                """;
        StationDefinition def2 = loader.load(new ByteArrayInputStream(yamlFreeYaw.getBytes(StandardCharsets.UTF_8)), "skillet.yml");
        assertEquals(dev.darkblade.croppy.api.species.RotationMode.FREE_YAW, def2.rotationMode());

        String yamlHyphen = """
                id: "croppy:pot"
                block: "cauldron"
                rotation-mode: "CARDINAL_8"
                """;
        StationDefinition def3 = loader.load(new ByteArrayInputStream(yamlHyphen.getBytes(StandardCharsets.UTF_8)), "pot.yml");
        assertEquals(dev.darkblade.croppy.api.species.RotationMode.CARDINAL_8, def3.rotationMode());
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
        assertFalse(text.hideWhenIdle());
    }

    @Test
    void testLoadVisualTextDisplayHideWhenIdle() {
        String yaml = """
                id: "croppy:cooking_pot"
                name: "<gold>Pot</gold>"
                block: "minecraft:player_head"
                has_display: true
                visual:
                  text_display:
                    enabled: true
                    hide_when_idle: true
                    format: "<gold>{recipe_name}</gold>"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "cooking_pot.yml");

        assertTrue(def.getVisualDefinition().isPresent());
        var visual = def.getVisualDefinition().get();
        assertTrue(visual.getTextDisplay().isPresent());
        assertTrue(visual.getTextDisplay().get().hideWhenIdle());
        assertEquals("<gold>{recipe_name}</gold>", visual.getTextDisplay().get().format());
    }

    @Test
    void testLoadVisualHideBaseBlockAndInteraction() {
        String yaml = """
                id: "croppy:skillet"
                name: "<gradient:#ffaa00:#ff3300>Skillet</gradient>"
                block: "minecraft:player_head"
                has_display: true
                visual:
                  hide_base_block: true
                  interaction:
                    width: 0.8
                    height: 0.25
                  base_model:
                    enabled: true
                    type: "ITEM"
                    item: "minecraft:heavy_weighted_pressure_plate"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "skillet.yml");

        assertTrue(def.getVisualDefinition().isPresent());
        var visual = def.getVisualDefinition().get();
        assertTrue(visual.hideBaseBlock());
        assertTrue(visual.getInteractionBox().isPresent());
        assertEquals(0.8f, visual.getInteractionBox().get().width(), 0.001f);
        assertEquals(0.25f, visual.getInteractionBox().get().height(), 0.001f);
    }

    @Test
    void testLoadStationStates() {
        String yaml = """
                id: "croppy:stove"
                block: "barrier"
                states:
                  - id: "lit"
                    priority: 20
                    when: "fuel_ticks > 0"
                    model:
                      item_model: "croppy:station/stove/lit"
                      custom_model_data: 1005
                      scale: [1.0, 1.2, 1.0]
                    hitbox:
                      width: 0.9
                      height: 0.8
                    effects:
                      particle:
                        type: "CAMPFIRE_COSY_SMOKE"
                        count: 3
                        offset: [0.1, 0.4, 0.1]
                        speed: 0.01
                      sound:
                        sound: "block.fire.ambient"
                        interval_ticks: 30
                        volume: 0.8
                        pitch: 1.0
                  - id: "unlit"
                    priority: 0
                    model:
                      item_model: "croppy:station/stove/unlit"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "stove-states.yml");

        assertEquals(2, def.states().size());

        var litState = def.states().get(0);
        assertEquals("lit", litState.id());
        assertEquals(20, litState.priority());
        assertTrue(litState.getCondition().isPresent());
        assertEquals("fuel_ticks > 0", litState.getCondition().get());

        assertTrue(litState.getModel().isPresent());
        var model = litState.getModel().get();
        assertTrue(model.getItemModel().isPresent());
        assertEquals(Key.of("croppy", "station/stove/lit"), model.getItemModel().get());
        assertEquals(1005, model.customModelData());
        assertEquals(1.2f, model.transform().scaleY(), 0.001f);

        assertTrue(litState.getHitbox().isPresent());
        assertEquals(0.9f, litState.getHitbox().get().width(), 0.001f);
        assertEquals(0.8f, litState.getHitbox().get().height(), 0.001f);

        assertTrue(litState.getEffects().isPresent());
        var effects = litState.getEffects().get();
        assertTrue(effects.hasParticle());
        assertEquals("CAMPFIRE_COSY_SMOKE", effects.particle().type());
        assertEquals(3, effects.particle().count());
        assertTrue(effects.hasSound());
        assertEquals("block.fire.ambient", effects.sound().sound());
        assertEquals(30, effects.sound().intervalTicks());

        var unlitState = def.states().get(1);
        assertEquals("unlit", unlitState.id());
        assertEquals(0, unlitState.priority());
        assertTrue(unlitState.getCondition().isEmpty());
        assertTrue(unlitState.getModel().isPresent());
        assertEquals(Key.of("croppy", "station/stove/unlit"), unlitState.getModel().get().getItemModel().get());
    }

    @Test
    void testStationStateModelTransformOverridesAndInheritance() {
        String yaml = """
                id: "croppy:pot"
                block: "cauldron"
                visual:
                  base_model:
                    item: "croppy:pot"
                    translation: [0.0, 0.5, 0.0]
                    scale: [2.0, 2.0, 2.0]
                    rotation: [0.0, 45.0, 0.0]
                states:
                  - id: "lifted"
                    when: "temp > 100"
                    model:
                      translation: [0.0, 0.75, 0.0]
                  - id: "scaled_and_rotated"
                    when: "temp > 200"
                    model:
                      item_model: "croppy:station/cooking_pot/boiling"
                      scale: [2.5, 2.5, 2.5]
                      rotation: [15.0, 90.0, 0.0]
                  - id: "no_transform"
                    when: "temp == 0"
                    model:
                      item_model: "croppy:station/cooking_pot/cold"
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "pot-transforms.yml");

        assertEquals(3, def.states().size());

        // State 0: overrides translation, inherits base scale [2.0, 2.0, 2.0] and base yaw 45 deg
        var state0 = def.states().get(0);
        assertTrue(state0.getModel().isPresent());
        var model0 = state0.getModel().get();
        assertTrue(model0.getItemModel().isEmpty()); // no item model override
        assertTrue(model0.hasTransform());
        var t0 = model0.transform();
        assertEquals(0.75, t0.translationY(), 0.001);
        assertEquals(2.0f, t0.scaleX(), 0.001f);
        assertEquals(2.0f, t0.scaleY(), 0.001f);
        assertEquals(2.0f, t0.scaleZ(), 0.001f);
        assertEquals(Math.toRadians(45.0), t0.yaw(), 0.001);

        // State 1: overrides item_model, scale, and rotation, inherits base translation Y 0.5
        var state1 = def.states().get(1);
        assertTrue(state1.getModel().isPresent());
        var model1 = state1.getModel().get();
        assertEquals(Key.of("croppy", "station/cooking_pot/boiling"), model1.getItemModel().get());
        assertTrue(model1.hasTransform());
        var t1 = model1.transform();
        assertEquals(0.5, t1.translationY(), 0.001);
        assertEquals(2.5f, t1.scaleX(), 0.001f);
        assertEquals(Math.toRadians(15.0), t1.pitch(), 0.001);
        assertEquals(Math.toRadians(90.0), t1.yaw(), 0.001);

        // State 2: only item_model, no transform override -> transform is null (preserves base)
        var state2 = def.states().get(2);
        assertTrue(state2.getModel().isPresent());
        var model2 = state2.getModel().get();
        assertEquals(Key.of("croppy", "station/cooking_pot/cold"), model2.getItemModel().get());
        assertFalse(model2.hasTransform());
        assertNull(model2.transform());
    }

    @Test
    void testStationStateHitboxOverridesAndFormats() {
        String yaml = """
                id: "croppy:skillet"
                block: "iron_block"
                visual:
                  interaction:
                    width: 0.8
                    height: 0.3
                states:
                  - id: "map_hitbox"
                    hitbox:
                      width: 1.2
                      height: 0.5
                  - id: "partial_hitbox"
                    hitbox:
                      width: 1.5
                  - id: "list_hitbox"
                    hitbox: [1.1, 0.4]
                  - id: "scalar_hitbox"
                    hitbox: 0.9
                  - id: "no_hitbox"
                    priority: 0
                """;

        ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        StationDefinition def = loader.load(in, "skillet-hitbox.yml");

        assertEquals(5, def.states().size());

        // State 0: map hitbox
        var state0 = def.states().get(0);
        assertTrue(state0.getHitbox().isPresent());
        assertEquals(1.2f, state0.getHitbox().get().width(), 0.001f);
        assertEquals(0.5f, state0.getHitbox().get().height(), 0.001f);

        // State 1: partial hitbox overrides width, inherits base height 0.3
        var state1 = def.states().get(1);
        assertTrue(state1.getHitbox().isPresent());
        assertEquals(1.5f, state1.getHitbox().get().width(), 0.001f);
        assertEquals(0.3f, state1.getHitbox().get().height(), 0.001f);

        // State 2: list format [1.1, 0.4]
        var state2 = def.states().get(2);
        assertTrue(state2.getHitbox().isPresent());
        assertEquals(1.1f, state2.getHitbox().get().width(), 0.001f);
        assertEquals(0.4f, state2.getHitbox().get().height(), 0.001f);

        // State 3: scalar number 0.9 -> width=0.9, height=0.9
        var state3 = def.states().get(3);
        assertTrue(state3.getHitbox().isPresent());
        assertEquals(0.9f, state3.getHitbox().get().width(), 0.001f);
        assertEquals(0.9f, state3.getHitbox().get().height(), 0.001f);

        // State 4: no hitbox override -> empty Optional
        var state4 = def.states().get(4);
        assertTrue(state4.getHitbox().isEmpty());
    }
}
