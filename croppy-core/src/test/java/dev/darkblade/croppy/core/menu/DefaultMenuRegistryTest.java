package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMenuRegistryTest {

    private DefaultMenuRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultMenuRegistry();
    }

    @Test
    @DisplayName("Registry registers, looks up, and enforces uniqueness")
    void testRegisterAndLookup() {
        Key key = Key.of("croppy", "main_menu");
        MenuDefinition menu = new MenuDefinition(
                key,
                "Title",
                1,
                List.of("XXXXXXXXX"),
                Map.of(),
                Map.of()
        );

        assertFalse(registry.contains(key));
        assertEquals(0, registry.size());

        registry.register(menu);
        assertTrue(registry.contains(key));
        assertEquals(1, registry.size());
        assertTrue(registry.get(key).isPresent());
        assertSame(menu, registry.get(key).get());

        // Duplicate registration must fail
        assertThrows(IllegalStateException.class, () -> registry.register(menu));

        // Values view is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> registry.values().add(menu));

        // Clear
        registry.clear();
        assertEquals(0, registry.size());
        assertFalse(registry.contains(key));
    }
}
