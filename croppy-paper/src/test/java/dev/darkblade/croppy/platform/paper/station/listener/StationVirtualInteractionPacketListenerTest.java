package dev.darkblade.croppy.platform.paper.station.listener;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayLayerConfig;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.display.PacketEventsStationRenderer;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
import dev.darkblade.croppy.platform.paper.station.display.VisualTier;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StationVirtualInteractionPacketListenerTest {

    private DefaultStationManager stationManager;
    private StationViewerTracker viewerTracker;
    private StationBlockListener blockListener;
    private StationInteractListener interactListener;
    private Plugin mockPlugin;
    private StationVirtualInteractionPacketListener listener;

    @BeforeEach
    void setUp() {
        Server mockServer = (Server) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> null
        );

        viewerTracker = new StationViewerTracker(
                mockServer,
                new PacketEventsStationRenderer(Logger.getLogger("test")),
                new ItemFactory(null, new DefaultItemRegistry()),
                Logger.getLogger("test")
        );

        CroppyCore core = new CroppyCore(Logger.getLogger("test"));
        stationManager = core.getStationManager();

        dev.darkblade.croppy.api.storage.StationStorage storage = new dev.darkblade.croppy.api.storage.StationStorage() {
            @Override public java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> loadChunk(String w, int x, int z) { return List.of(); }
            @Override public void saveChunk(String w, int x, int z, java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> s) {}
            @Override public void deleteChunk(String w, int x, int z) {}
            @Override public void saveStation(dev.darkblade.croppy.api.station.StationInstance s) {}
            @Override public void deleteStation(BlockPosition p) {}
        };
        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());

        blockListener = new StationBlockListener(stationManager, core.getStationRegistry(), storage, itemFactory, viewerTracker);

        interactListener = new StationInteractListener(
                stationManager,
                core.getProcessEngine(),
                itemFactory,
                null,
                core.getInteractionRouter(),
                core.getRuleEngine()
        );

        mockPlugin = (Plugin) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> null
        );

        listener = new StationVirtualInteractionPacketListener(
                mockPlugin,
                viewerTracker,
                blockListener,
                interactListener,
                Logger.getLogger("test"),
                null,
                stationManager
        );
    }

    @Test
    @DisplayName("Lookup via interaction ID returns empty for non-station entities")
    void testUnknownEntityLookup() {
        assertTrue(viewerTracker.getDisplayByInteractionId(12345).isEmpty());
    }

    @Test
    @DisplayName("Lookup finds registered station interaction ID")
    void testKnownEntityLookup() {
        StationDefinition def = new StationDefinition(
                Key.of("croppy", "skillet"),
                "Skillet",
                "minecraft:player_head",
                StationInteractionType.PHYSICAL,
                2,
                1,
                true,
                List.of(),
                Map.of()
        );
        DefaultStationInstance station = new DefaultStationInstance(def.key(), BlockPosition.of("world", 0, 64, 0), def);
        var display = viewerTracker.getOrCreateDisplay(station);

        assertTrue(viewerTracker.getDisplayByInteractionId(display.getInteractionId()).isPresent());
        assertEquals(display, viewerTracker.getDisplayByInteractionId(display.getInteractionId()).get());
    }

    @Test
    @DisplayName("Queries DefaultStationManager before displays are active and detects hideBaseBlock = true")
    void testResolveStationFromStationManagerBeforeDisplaysActive() {
        DisplayLayerConfig baseConfig = new DisplayLayerConfig(
                true, "ITEM", Key.of("croppy", "skillet"), null,
                DisplayTransform.DEFAULT, BillboardMode.FIXED, 48, 0
        );
        StationVisualDefinition visual = new StationVisualDefinition(baseConfig, null, null, true, null);

        StationDefinition def = new StationDefinition(
                Key.of("croppy", "skillet"),
                "Skillet",
                "minecraft:furnace",
                StationInteractionType.PHYSICAL,
                4, 2, Key.of("croppy", "skillet"), true,
                List.of(), Map.of(), visual, null, List.of()
        );

        BlockPosition pos = BlockPosition.of("world", 5, 64, 5);
        DefaultStationInstance station = new DefaultStationInstance(def.key(), pos, def);
        stationManager.addStation(station);

        // Verify viewer tracker has no active display yet
        assertTrue(viewerTracker.getDisplay(pos).isEmpty(), "Display should not exist yet in viewer tracker");

        // Listener must resolve station from stationManager and identify that base block needs masking
        assertTrue(listener.resolveStation(pos).isPresent(), "Should resolve station from DefaultStationManager");
        assertEquals(station, listener.resolveStation(pos).get());
        assertTrue(listener.shouldMaskBaseBlock(pos), "Should determine base block must be masked");
    }

    @Test
    @DisplayName("Does not mask base block when hide_base_block is false")
    void testShouldNotMaskWhenHideBaseBlockIsFalse() {
        DisplayLayerConfig baseConfig = new DisplayLayerConfig(
                true, "ITEM", Key.of("croppy", "chopping_board"), null,
                DisplayTransform.DEFAULT, BillboardMode.FIXED, 48, 0
        );
        StationVisualDefinition visual = new StationVisualDefinition(baseConfig, null, null, false, null);

        StationDefinition def = new StationDefinition(
                Key.of("croppy", "chopping_board"),
                "Chopping Board",
                "minecraft:oak_slab",
                StationInteractionType.PHYSICAL,
                4, 2, Key.of("croppy", "chopping_board"), true,
                List.of(), Map.of(), visual, null, List.of()
        );

        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultStationInstance station = new DefaultStationInstance(def.key(), pos, def);
        stationManager.addStation(station);

        assertTrue(listener.resolveStation(pos).isPresent());
        assertFalse(listener.shouldMaskBaseBlock(pos), "Should not mask base block when hideBaseBlock is false");
    }

    @Test
    @DisplayName("Does not mask base block when no station is registered at position")
    void testShouldNotMaskWhenStationNotRegistered() {
        BlockPosition pos = BlockPosition.of("world", 99, 64, 99);
        assertTrue(listener.resolveStation(pos).isEmpty());
        assertFalse(listener.shouldMaskBaseBlock(pos));
    }

    @Test
    @DisplayName("Falls back to viewerTracker when stationManager is null")
    void testFallbackToViewerTrackerWhenStationManagerNull() {
        StationVirtualInteractionPacketListener legacyListener = new StationVirtualInteractionPacketListener(
                mockPlugin,
                viewerTracker,
                blockListener,
                interactListener,
                Logger.getLogger("test")
        );

        DisplayLayerConfig baseConfig = new DisplayLayerConfig(
                true, "ITEM", Key.of("croppy", "skillet"), null,
                DisplayTransform.DEFAULT, BillboardMode.FIXED, 48, 0
        );
        StationVisualDefinition visual = new StationVisualDefinition(baseConfig, null, null, true, null);

        StationDefinition def = new StationDefinition(
                Key.of("croppy", "skillet"),
                "Skillet",
                "minecraft:furnace",
                StationInteractionType.PHYSICAL,
                4, 2, Key.of("croppy", "skillet"), true,
                List.of(), Map.of(), visual, null, List.of()
        );

        BlockPosition pos = BlockPosition.of("world", 20, 64, 20);
        DefaultStationInstance station = new DefaultStationInstance(def.key(), pos, def);

        // Pre-create display in viewerTracker
        viewerTracker.getOrCreateDisplay(station);

        assertTrue(legacyListener.resolveStation(pos).isPresent(), "Fallback to viewerTracker must find display");
        assertTrue(legacyListener.shouldMaskBaseBlock(pos), "Fallback must determine base block must be masked");
    }
}
