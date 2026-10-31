package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuItemTemplate;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MenuDefinitionTest {

    @Test
    @DisplayName("MenuDefinition constructs and indexes pattern correctly")
    void testMenuDefinitionConstructionAndIndexing() {
        Key key = Key.of("croppy", "test_menu");
        List<String> pattern = List.of(
                "XXXXXXXXX",
                "XI.S.OOXX",
                "XXXXXXXXX"
        );
        Map<Character, SlotDefinition> elements = Map.of(
                'X', SlotDefinition.decoration(MenuItemTemplate.simple("minecraft:gray_stained_glass_pane", " ")),
                'I', SlotDefinition.input(),
                'O', SlotDefinition.output(),
                'S', new SlotDefinition(SlotType.STATUS, MenuItemTemplate.simple("minecraft:compass", "Status"), List.of(), List.of(), Map.of())
        );

        MenuDefinition menu = new MenuDefinition(key, "<yellow>Test</yellow>", 3, pattern, elements, Map.of());

        assertEquals(key, menu.getKey());
        assertEquals(3, menu.rows());
        assertEquals(27, menu.size());
        assertEquals('<', menu.title().charAt(0));

        // Test slot indexing
        assertEquals('X', menu.getSymbolAt(0));
        assertEquals('I', menu.getSymbolAt(10));
        assertEquals('.', menu.getSymbolAt(11));
        assertEquals('S', menu.getSymbolAt(12));
        assertEquals('O', menu.getSymbolAt(14));
        assertEquals('O', menu.getSymbolAt(15));

        // Test getSlotAt
        assertTrue(menu.getSlotAt(0).isPresent());
        assertEquals(SlotType.DECORATION, menu.getSlotAt(0).get().type());
        assertTrue(menu.getSlotAt(10).isPresent());
        assertEquals(SlotType.INPUT, menu.getSlotAt(10).get().type());
        assertTrue(menu.getSlotAt(11).isEmpty()); // '.' not defined in elements

        // Test getSlotsForType
        List<Integer> inputSlots = menu.getSlotsForType(SlotType.INPUT);
        assertEquals(List.of(10), inputSlots);

        List<Integer> outputSlots = menu.getSlotsForType(SlotType.OUTPUT);
        assertEquals(List.of(14, 15), outputSlots);

        List<Integer> statusSlots = menu.getSlotsForType(SlotType.STATUS);
        assertEquals(List.of(12), statusSlots);
    }

    @Test
    @DisplayName("MenuDefinition validates rows and pattern length")
    void testMenuDefinitionValidation() {
        Key key = Key.of("croppy", "invalid");
        // Rows count out of 1..6
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition(
                key, "Title", 0, List.of(), Map.of(), Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition(
                key, "Title", 7, List.of(), Map.of(), Map.of()
        ));

        // Pattern rows count doesn't match rows
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition(
                key, "Title", 2, List.of("XXXXXXXXX"), Map.of(), Map.of()
        ));

        // Pattern line width not 9
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition(
                key, "Title", 1, List.of("XXXX"), Map.of(), Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition(
                key, "Title", 1, List.of("XXXXXXXXXX"), Map.of(), Map.of()
        ));
    }
}
