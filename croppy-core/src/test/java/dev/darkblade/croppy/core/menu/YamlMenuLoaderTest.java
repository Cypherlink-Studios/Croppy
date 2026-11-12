package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.menu.ClickTypeFilter;
import dev.darkblade.croppy.api.menu.MenuActionType;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlMenuLoaderTest {

    private final YamlMenuLoader loader = new YamlMenuLoader();

    @Test
    @DisplayName("YamlMenuLoader parses complete 4-row menu definition successfully")
    void testParseCompleteMenu() {
        String yaml = """
                id: "croppy:cooking_pot"
                title: "<gradient:#ffaa00:#ff5555>Cooking Pot</gradient>"
                rows: 4
                pattern:
                  - "X X X X X X X X X"
                  - "X I I I . O O . X"
                  - "X I I I . S . . X"
                  - "X X X X B X X X X"
                elements:
                  'X':
                    type: DECORATION
                    item:
                      material: "minecraft:gray_stained_glass_pane"
                      name: " "
                  'I':
                    type: INPUT
                  'O':
                    type: OUTPUT
                  'S':
                    type: STATUS
                    item:
                      material: "minecraft:compass"
                      name: "<gray>Status: Idle</gray>"
                    states:
                      - condition: "process.state == 'RUNNING'"
                        item:
                          material: "minecraft:soul_campfire"
                          name: "<gold>Cooking...</gold>"
                          lore:
                            - "<yellow>Progress:</yellow> <aqua>{process.progress_percent}%</aqua>"
                          glow: true
                  'B':
                    type: ACTION
                    item:
                      material: "minecraft:flint_and_steel"
                      name: "<yellow>Ignite</yellow>"
                    actions:
                      - click: LEFT
                        type: STATION_ACTION
                        action: "START_PROCESS"
                      - click: RIGHT
                        type: SOUND
                        sound: "minecraft:item.flintandsteel.use"
                        condition: "station.has_fuel == true"
                properties:
                  category: "cookware"
                """;

        MenuDefinition def = loader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "cooking_pot.yml");

        assertEquals(Key.of("croppy", "cooking_pot"), def.getKey());
        assertEquals("<gradient:#ffaa00:#ff5555>Cooking Pot</gradient>", def.title());
        assertEquals(4, def.rows());
        assertEquals(36, def.size());

        // Check sanitized pattern rows
        assertEquals("XXXXXXXXX", def.pattern().get(0));
        assertEquals("XIII.OO.X", def.pattern().get(1));
        assertEquals("XIII.S..X", def.pattern().get(2));
        assertEquals("XXXXBXXXX", def.pattern().get(3));

        // Check input and output slot mapping
        List<Integer> inputSlots = def.getSlotsForType(SlotType.INPUT);
        assertEquals(6, inputSlots.size());
        assertEquals(List.of(10, 11, 12, 19, 20, 21), inputSlots);

        List<Integer> outputSlots = def.getSlotsForType(SlotType.OUTPUT);
        assertEquals(2, outputSlots.size());
        assertEquals(List.of(14, 15), outputSlots);

        // Check status slot and conditional states
        List<Integer> statusSlots = def.getSlotsForType(SlotType.STATUS);
        assertEquals(List.of(23), statusSlots);
        var statusDef = def.getSlotAt(23).orElseThrow();
        assertEquals(SlotType.STATUS, statusDef.type());
        assertTrue(statusDef.getDefaultTemplate().isPresent());
        assertEquals("minecraft:compass", statusDef.getDefaultTemplate().get().material());
        assertEquals(1, statusDef.states().size());
        assertEquals("process.state == 'RUNNING'", statusDef.states().get(0).condition());
        assertEquals("minecraft:soul_campfire", statusDef.states().get(0).template().material());
        assertTrue(statusDef.states().get(0).template().glow());

        // Check action slot and multiple actions
        List<Integer> actionSlots = def.getSlotsForType(SlotType.ACTION);
        assertEquals(List.of(31), actionSlots);
        var actionDef = def.getSlotAt(31).orElseThrow();
        assertEquals(SlotType.ACTION, actionDef.type());
        assertEquals(2, actionDef.actions().size());

        assertEquals(ClickTypeFilter.LEFT, actionDef.actions().get(0).click());
        assertEquals(MenuActionType.STATION_ACTION, actionDef.actions().get(0).type());
        assertEquals("START_PROCESS", actionDef.actions().get(0).target());

        assertEquals(ClickTypeFilter.RIGHT, actionDef.actions().get(1).click());
        assertEquals(MenuActionType.SOUND, actionDef.actions().get(1).type());
        assertEquals("minecraft:item.flintandsteel.use", actionDef.actions().get(1).target());
        assertEquals("station.has_fuel == true", actionDef.actions().get(1).getCondition().orElse(null));

        // Check properties
        assertEquals("cookware", def.properties().get("category"));
    }

    @Test
    @DisplayName("YamlMenuLoader validates row and pattern dimensions")
    void testPatternValidation() {
        // Mismatched rows
        String badRowsYaml = """
                id: "croppy:test"
                rows: 2
                pattern:
                  - "XXXXXXXXX"
                """;
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(badRowsYaml.getBytes(StandardCharsets.UTF_8)), "badRows.yml"));

        // Row not 9 chars
        String badColsYaml = """
                id: "croppy:test"
                rows: 1
                pattern:
                  - "XXXX"
                """;
        assertThrows(IllegalArgumentException.class, () ->
                loader.load(new ByteArrayInputStream(badColsYaml.getBytes(StandardCharsets.UTF_8)), "badCols.yml"));
    }

    @Test
    @DisplayName("YamlMenuLoader loads all files from directory into registry")
    void testLoadAll(@TempDir Path tempDir) throws IOException {
        String yaml1 = """
                id: "croppy:menu1"
                rows: 1
                pattern:
                  - "XXXXXXXXX"
                """;
        String yaml2 = """
                id: "croppy:menu2"
                rows: 1
                pattern:
                  - "XXXXXXXXX"
                """;

        Files.writeString(tempDir.resolve("menu1.yml"), yaml1);
        Files.writeString(tempDir.resolve("menu2.yaml"), yaml2);

        DefaultMenuRegistry registry = new DefaultMenuRegistry();
        int loaded = loader.loadAll(tempDir, registry, NoOpCroppyLogger.INSTANCE);

        assertEquals(2, loaded);
        assertTrue(registry.contains(Key.of("croppy", "menu1")));
        assertTrue(registry.contains(Key.of("croppy", "menu2")));
    }
}
