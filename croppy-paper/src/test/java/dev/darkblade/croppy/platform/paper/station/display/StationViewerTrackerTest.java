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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

class StationViewerTrackerTest {

    static class TestRenderer extends PacketEventsStationRenderer {
        int destroyCallCount = 0;
        int[] lastDestroyedIds;
        int spawnTextCallCount = 0;
        int updateTextCallCount = 0;
        Component lastSpawnedText;
        Component lastUpdatedText;

        public TestRenderer() {
            super(Logger.getLogger("test"));
        }

        @Override
        public void destroyEntities(Player player, int... entityIds) {
            destroyCallCount++;
            lastDestroyedIds = entityIds;
        }

        @Override
        public void spawnTextDisplay(Player player, PacketStationDisplay display, dev.darkblade.croppy.api.station.visual.TextDisplayTemplate template, Component text) {
            spawnTextCallCount++;
            lastSpawnedText = text;
        }

        @Override
        public void updateTextDisplayMetadata(Player player, PacketStationDisplay display, dev.darkblade.croppy.api.station.visual.TextDisplayTemplate template, Component text) {
            updateTextCallCount++;
            lastUpdatedText = text;
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

    @Test
    void testStateAwareHologramRenderingAndPacketSuppression() {
        dev.darkblade.croppy.core.station.component.DefaultDisplayComponent display =
                new dev.darkblade.croppy.core.station.component.DefaultDisplayComponent();
        station.setComponent(dev.darkblade.croppy.api.station.component.DisplayComponent.class, display);

        UUID playerId = UUID.randomUUID();
        Player dummyPlayer = (Player) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return playerId;
                    if ("getLocation".equals(method.getName())) return new org.bukkit.Location(null, 10.5, 64.0, 20.5);
                    return null;
                }
        );

        org.bukkit.World dummyWorld = (org.bukkit.World) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{org.bukkit.World.class},
                (proxy, method, args) -> {
                    if ("getPlayers".equals(method.getName())) return List.of(dummyPlayer);
                    if ("getName".equals(method.getName())) return "world";
                    return null;
                }
        );

        // 1. First sync while IDLE -> spawns text display with station name
        tracker.syncStation(station, dummyWorld, null);
        assertEquals(1, testRenderer.spawnTextCallCount);
        String text1 = PlainTextComponentSerializer.plainText().serialize(testRenderer.lastSpawnedText);
        assertTrue(text1.contains("Cooking Pot"));

        // 2. Second sync unchanged -> text suppression prevents update packet
        display.setDirty(false);
        tracker.syncStation(station, dummyWorld, null);
        assertEquals(0, testRenderer.updateTextCallCount, "Redundant packet should be suppressed when text unchanged");

        // 3. Process starts RUNNING with phase status text -> delegates to phase template
        display.setStatusText("<gray>Simmering: 50%</gray>");
        display.setDirty(true);
        dev.darkblade.croppy.core.process.DefaultProcessInstance process =
                new dev.darkblade.croppy.core.process.DefaultProcessInstance(Key.of("croppy", "soup_process"));
        process.setState(dev.darkblade.croppy.api.process.ProcessState.RUNNING);

        tracker.syncStation(station, dummyWorld, process);
        assertEquals(1, testRenderer.updateTextCallCount);
        String text2 = PlainTextComponentSerializer.plainText().serialize(testRenderer.lastUpdatedText);
        assertTrue(text2.contains("Simmering: 50%"), "Active process must delegate to phase status text");

        // 4. Same text ticked again with dirty=false -> packet suppressed
        display.setDirty(false);
        tracker.syncStation(station, dummyWorld, process);
        assertEquals(1, testRenderer.updateTextCallCount, "Should not resend packet when text component matches cached");
    }

    @Test
    void testHideWhenIdleDynamicCulling() {
        dev.darkblade.croppy.core.station.component.DefaultDisplayComponent display =
                new dev.darkblade.croppy.core.station.component.DefaultDisplayComponent();
        station.setComponent(dev.darkblade.croppy.api.station.component.DisplayComponent.class, display);

        dev.darkblade.croppy.core.station.component.DefaultThermalComponent thermal =
                new dev.darkblade.croppy.core.station.component.DefaultThermalComponent(20.0, 20.0, false);
        station.setComponent(dev.darkblade.croppy.api.station.component.ThermalComponent.class, thermal);

        // Create station with hide_when_idle = true
        StationDefinition defWithHide = new StationDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                "cauldron",
                StationInteractionType.HYBRID,
                6, 2, null, true,
                List.of(), Map.of(),
                new dev.darkblade.croppy.api.station.visual.StationVisualDefinition(
                        null, null,
                        new dev.darkblade.croppy.api.station.visual.TextDisplayTemplate(
                                true, "<yellow>{recipe_name}</yellow>",
                                dev.darkblade.croppy.api.station.visual.DisplayTransform.DEFAULT,
                                dev.darkblade.croppy.api.station.visual.BillboardMode.CENTER,
                                8, 200, 0, false, true, true
                        )
                )
        );
        DefaultStationInstance idleStation = new DefaultStationInstance(defWithHide.key(), new BlockPosition("world", 10, 64, 20), defWithHide);
        idleStation.setComponent(dev.darkblade.croppy.api.station.component.DisplayComponent.class, display);
        idleStation.setComponent(dev.darkblade.croppy.api.station.component.ThermalComponent.class, thermal);

        UUID playerId = UUID.randomUUID();
        Player dummyPlayer = (Player) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return playerId;
                    if ("getLocation".equals(method.getName())) return new org.bukkit.Location(null, 10.5, 64.0, 20.5);
                    return null;
                }
        );

        org.bukkit.World dummyWorld = (org.bukkit.World) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{org.bukkit.World.class},
                (proxy, method, args) -> {
                    if ("getPlayers".equals(method.getName())) return List.of(dummyPlayer);
                    if ("getName".equals(method.getName())) return "world";
                    return null;
                }
        );

        // 1. Cold (20°C), empty, idle station -> text display NOT spawned
        tracker.syncStation(idleStation, dummyWorld, null);
        assertEquals(0, testRenderer.spawnTextCallCount, "Text display must not spawn when cold & idle with hide_when_idle=true");

        // 2. Station heats up to 80°C -> text display spawns
        thermal.setTemperature(80.0);
        display.setDirty(true);
        tracker.syncStation(idleStation, dummyWorld, null);
        assertEquals(1, testRenderer.spawnTextCallCount, "Text display must spawn when heated above ambient");

        // 3. Station cools back down to 20°C -> text display destroyed
        thermal.setTemperature(20.0);
        display.setDirty(true);
        tracker.syncStation(idleStation, dummyWorld, null);
        assertTrue(testRenderer.destroyCallCount > 0, "Text display must be destroyed when station cools to idle");
    }
}
