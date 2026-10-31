package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.menu.ClickTypeFilter;
import dev.darkblade.croppy.api.menu.ConditionalTemplate;
import dev.darkblade.croppy.api.menu.MenuAction;
import dev.darkblade.croppy.api.menu.MenuActionType;
import dev.darkblade.croppy.api.menu.MenuItemTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuItemTemplateTest {

    @Test
    @DisplayName("MenuItemTemplate stores values and ensures immutability of lore")
    void testMenuItemTemplateValidationAndImmutability() {
        List<String> mutableLore = new ArrayList<>();
        mutableLore.add("Line 1");
        mutableLore.add("Line 2");

        MenuItemTemplate template = new MenuItemTemplate(
                "minecraft:diamond",
                5,
                "<aqua>Gem</aqua>",
                mutableLore,
                101,
                true
        );

        assertEquals("minecraft:diamond", template.material());
        assertEquals(5, template.amount());
        assertEquals("<aqua>Gem</aqua>", template.displayName());
        assertEquals(2, template.lore().size());
        assertEquals(101, template.customModelData());
        assertTrue(template.glow());

        // Test immutability: mutating source list should not mutate record
        mutableLore.add("Line 3");
        assertEquals(2, template.lore().size());

        // Test lore list cannot be directly modified
        assertThrows(UnsupportedOperationException.class, () -> template.lore().add("Line 4"));

        // Null material should throw NPE
        assertThrows(NullPointerException.class, () -> new MenuItemTemplate(
                null, 1, "Title", List.of(), 0, false
        ));

        // Amount <= 0 defaults to 1
        MenuItemTemplate zeroAmt = new MenuItemTemplate("minecraft:apple", 0, null, null, 0, false);
        assertEquals(1, zeroAmt.amount());
        assertEquals("", zeroAmt.displayName());
        assertTrue(zeroAmt.lore().isEmpty());
    }

    @Test
    @DisplayName("ConditionalTemplate validates condition and template")
    void testConditionalTemplateValidation() {
        MenuItemTemplate item = MenuItemTemplate.simple("minecraft:bell", "<gold>Ready</gold>");
        ConditionalTemplate ct = new ConditionalTemplate("process.state == 'RUNNING'", item);

        assertEquals("process.state == 'RUNNING'", ct.condition());
        assertEquals(item, ct.template());

        assertThrows(NullPointerException.class, () -> new ConditionalTemplate(null, item));
        assertThrows(IllegalArgumentException.class, () -> new ConditionalTemplate("   ", item));
        assertThrows(NullPointerException.class, () -> new ConditionalTemplate("valid", null));
    }

    @Test
    @DisplayName("MenuAction stores values and validates required fields")
    void testMenuActionValidation() {
        MenuAction action = new MenuAction(
                ClickTypeFilter.LEFT,
                MenuActionType.STATION_ACTION,
                "START_PROCESS",
                "station.has_fuel == true"
        );

        assertEquals(ClickTypeFilter.LEFT, action.click());
        assertEquals(MenuActionType.STATION_ACTION, action.type());
        assertEquals("START_PROCESS", action.target());
        assertTrue(action.getCondition().isPresent());
        assertEquals("station.has_fuel == true", action.getCondition().get());

        // Unconditional helper
        MenuAction simple = MenuAction.of(MenuActionType.CLOSE, "close");
        assertEquals(ClickTypeFilter.ALL, simple.click());
        assertEquals(MenuActionType.CLOSE, simple.type());
        assertTrue(simple.getCondition().isEmpty());

        // Null checks
        assertThrows(NullPointerException.class, () -> new MenuAction(ClickTypeFilter.ALL, null, "target", null));
        assertThrows(NullPointerException.class, () -> new MenuAction(ClickTypeFilter.ALL, MenuActionType.CLOSE, null, null));
    }
}
