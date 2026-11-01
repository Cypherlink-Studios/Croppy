package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.identifier.Key;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultDisplayComponentTest {

    @Test
    void testInitialState() {
        DefaultDisplayComponent component = new DefaultDisplayComponent();
        assertTrue(component.isDirty());
        assertTrue(component.getDisplayedItem().isEmpty());
        assertEquals("", component.getStatusText());
        assertEquals(0.0f, component.getProgress(), 0.0001f);
        assertEquals("NONE", component.getThermalZone());
    }

    @Test
    void testSetDisplayedItemSetsDirty() {
        DefaultDisplayComponent component = new DefaultDisplayComponent();
        component.setDirty(false);
        assertFalse(component.isDirty());

        Key item = Key.of("croppy", "wheat");
        component.setDisplayedItem(item);
        assertTrue(component.isDirty());
        assertEquals(item, component.getDisplayedItem().orElse(null));

        component.setDirty(false);
        component.setDisplayedItem(item); // same item
        assertFalse(component.isDirty());
    }

    @Test
    void testSetStatusTextSetsDirty() {
        DefaultDisplayComponent component = new DefaultDisplayComponent();
        component.setDirty(false);

        component.setStatusText("Cooking");
        assertTrue(component.isDirty());
        assertEquals("Cooking", component.getStatusText());

        component.setDirty(false);
        component.setStatusText("Cooking");
        assertFalse(component.isDirty());
    }

    @Test
    void testProgressAndClamping() {
        DefaultDisplayComponent component = new DefaultDisplayComponent();
        component.setDirty(false);

        component.setProgress(0.5f);
        assertTrue(component.isDirty());
        assertEquals(0.5f, component.getProgress(), 0.0001f);

        component.setDirty(false);
        component.setProgress(1.5f); // Clamps to 1.0f
        assertTrue(component.isDirty());
        assertEquals(1.0f, component.getProgress(), 0.0001f);

        component.setDirty(false);
        component.setProgress(-0.5f); // Clamps to 0.0f
        assertTrue(component.isDirty());
        assertEquals(0.0f, component.getProgress(), 0.0001f);
    }

    @Test
    void testThermalZoneTracking() {
        DefaultDisplayComponent component = new DefaultDisplayComponent();
        component.setDirty(false);

        component.setThermalZone("BOIL");
        assertTrue(component.isDirty());
        assertEquals("BOIL", component.getThermalZone());

        component.setDirty(false);
        component.setThermalZone("boil"); // case-insensitive check
        assertFalse(component.isDirty());
    }
}
