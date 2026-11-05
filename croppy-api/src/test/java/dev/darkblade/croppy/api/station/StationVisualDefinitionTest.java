package dev.darkblade.croppy.api.station;

import dev.darkblade.croppy.api.station.visual.StationInteractionBox;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationVisualDefinitionTest {

    @Test
    @DisplayName("Canonical constructor retains hideBaseBlock and interactionBox")
    void testCanonicalConstructor() {
        StationInteractionBox box = new StationInteractionBox(0.8f, 0.25f);
        StationVisualDefinition visual = new StationVisualDefinition(null, null, null, true, box);

        assertTrue(visual.hideBaseBlock());
        assertTrue(visual.getInteractionBox().isPresent());
        assertEquals(box, visual.getInteractionBox().get());
    }

    @Test
    @DisplayName("Backward-compatible constructor defaults hideBaseBlock to false and interactionBox to empty")
    void testBackwardCompatibleConstructor() {
        StationVisualDefinition visual = new StationVisualDefinition(null, null, null);

        assertFalse(visual.hideBaseBlock());
        assertTrue(visual.getInteractionBox().isEmpty());
    }
}
