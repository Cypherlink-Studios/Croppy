package dev.darkblade.croppy.platform.paper.station.display;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StationViewerTrackerTest {

    static class TestRenderer extends PacketEventsStationRenderer {
        int destroyCallCount = 0;
        int[] lastDestroyedIds;

        public TestRenderer() {
            super(Logger.getLogger("test"));
        }

        @Override
        public void destroyEntities(Player player, int... entityIds) {
            destroyCallCount++;
            lastDestroyedIds = entityIds;
        }
    }

    private TestRenderer testRenderer;
    private ItemFactory itemFactory;
    private StationViewerTracker tracker;
    private DefaultStationInstance station;

    @BeforeEach
    void setUp() {
        VirtualEntityIdGenerator.reset();
        testRenderer = new TestRenderer();
        itemFactory = new ItemFactory(null, new DefaultItemRegistry());
        // tracker with null server (so it safely ignores player lookups in offline test)
        tracker = new StationViewerTracker(
                createMockOrNullServer(),
                testRenderer,
                itemFactory,
                Logger.getLogger("test")
        );

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

    private org.bukkit.Server createMockOrNullServer() {
        // Return a proxy or null-safe instance
        return (org.bukkit.Server) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{org.bukkit.Server.class},
                (proxy, method, args) -> null
        );
    }

    @Test
    void testGetOrCreateDisplay() {
        PacketStationDisplay display1 = tracker.getOrCreateDisplay(station);
        assertNotNull(display1);
        PacketStationDisplay display2 = tracker.getOrCreateDisplay(station);
        assertSame(display1, display2);
        assertEquals(1, tracker.getActiveDisplays().size());
    }

    @Test
    void testDestroyStationCleansUpTracking() {
        PacketStationDisplay display = tracker.getOrCreateDisplay(station);
        UUID playerId = UUID.randomUUID();
        display.addViewer(VisualTier.BASE_MODEL, playerId);

        tracker.destroyStation(station.getPosition());

        assertTrue(tracker.getActiveDisplays().isEmpty());
        assertTrue(display.getAllViewers().isEmpty());
    }

    @Test
    void testOnChunkUnloadDestroysStationsInChunk() {
        tracker.getOrCreateDisplay(station); // chunk: 10 >> 4 = 0, 20 >> 4 = 1
        assertEquals(1, tracker.getActiveDisplays().size());

        tracker.onChunkUnload("other_world", 0, 1);
        assertEquals(1, tracker.getActiveDisplays().size(), "Other world should not affect this station");

        tracker.onChunkUnload("world", 5, 5);
        assertEquals(1, tracker.getActiveDisplays().size(), "Other chunk coords should not affect this station");

        tracker.onChunkUnload("world", 0, 1);
        assertEquals(0, tracker.getActiveDisplays().size(), "Matching chunk should destroy the station display");
    }

    @Test
    void testGetDisplayByInteractionId() {
        PacketStationDisplay display = tracker.getOrCreateDisplay(station);
        int interactionId = display.getInteractionId();

        assertTrue(tracker.getDisplayByInteractionId(interactionId).isPresent());
        assertSame(display, tracker.getDisplayByInteractionId(interactionId).get());
        assertTrue(tracker.getDisplayByInteractionId(99999).isEmpty());
    }

    @Test
    void testOnPlayerQuitRemovesPlayerFromTracking() {
        PacketStationDisplay display = tracker.getOrCreateDisplay(station);
        UUID playerId = UUID.randomUUID();
        display.addViewer(VisualTier.BASE_MODEL, playerId);
        display.addViewer(VisualTier.TEXT_DISPLAY, playerId);

        Player dummyPlayer = (Player) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return playerId;
                    return null;
                }
        );

        tracker.onPlayerQuit(dummyPlayer);

        assertFalse(display.isViewing(VisualTier.BASE_MODEL, playerId));
        assertFalse(display.isViewing(VisualTier.TEXT_DISPLAY, playerId));
    }
}
