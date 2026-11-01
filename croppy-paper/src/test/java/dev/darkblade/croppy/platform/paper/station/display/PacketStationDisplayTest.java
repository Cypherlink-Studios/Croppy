package dev.darkblade.croppy.platform.paper.station.display;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketStationDisplayTest {

    private DefaultStationInstance station;

    @BeforeEach
    void setUp() {
        VirtualEntityIdGenerator.reset();
        StationDefinition def = new StationDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                "cauldron",
                StationInteractionType.HYBRID,
                6,
                2,
                true,
                List.of(),
                Map.of()
        );
        station = new DefaultStationInstance(def.key(), new BlockPosition("world", 10, 64, 20), def);
    }

    @Test
    void testEntityIdsAllocated() {
        PacketStationDisplay display = new PacketStationDisplay(station);

        int baseId = display.getEntityId(VisualTier.BASE_MODEL);
        int procId = display.getEntityId(VisualTier.PROCESS_ITEM);
        int textId = display.getEntityId(VisualTier.TEXT_DISPLAY);

        assertTrue(baseId < 0);
        assertTrue(procId < 0);
        assertTrue(textId < 0);
        assertNotEquals(baseId, procId);
        assertNotEquals(procId, textId);

        int[] allIds = display.getAllEntityIds();
        assertEquals(3, allIds.length);
        assertEquals(baseId, allIds[0]);
        assertEquals(procId, allIds[1]);
        assertEquals(textId, allIds[2]);
    }

    @Test
    void testViewerTracking() {
        PacketStationDisplay display = new PacketStationDisplay(station);
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        assertTrue(display.addViewer(VisualTier.BASE_MODEL, player1));
        assertFalse(display.addViewer(VisualTier.BASE_MODEL, player1)); // duplicate returns false
        assertTrue(display.isViewing(VisualTier.BASE_MODEL, player1));
        assertFalse(display.isViewing(VisualTier.PROCESS_ITEM, player1));

        assertTrue(display.addViewer(VisualTier.PROCESS_ITEM, player1));
        assertTrue(display.addViewer(VisualTier.TEXT_DISPLAY, player2));

        assertEquals(2, display.getAllViewers().size());

        display.removeViewer(VisualTier.BASE_MODEL, player1);
        assertFalse(display.isViewing(VisualTier.BASE_MODEL, player1));
        assertTrue(display.isViewing(VisualTier.PROCESS_ITEM, player1));

        display.removePlayerFromAll(player1);
        assertFalse(display.isViewing(VisualTier.PROCESS_ITEM, player1));

        display.clearAllViewers();
        assertTrue(display.getAllViewers().isEmpty());
    }
}
