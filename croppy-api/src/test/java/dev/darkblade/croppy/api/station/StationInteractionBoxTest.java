package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.station.visual.StationInteractionBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationInteractionBoxTest {

    @Test
    @DisplayName("Valid positive dimensions are retained")
    void testValidDimensions() {
        StationInteractionBox box = new StationInteractionBox(0.8f, 0.25f);
        assertEquals(0.8f, box.width(), 1e-6);
        assertEquals(0.25f, box.height(), 1e-6);
    }

    @Test
    @DisplayName("Zero or negative dimensions throw IllegalArgumentException")
    void testInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new StationInteractionBox(0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new StationInteractionBox(-0.5f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new StationInteractionBox(1.0f, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new StationInteractionBox(1.0f, -0.5f));
    }
}
