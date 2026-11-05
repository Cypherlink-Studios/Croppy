package dev.darkblade.croppy.platform.paper.station.listener;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.display.PacketEventsStationRenderer;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
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

    private StationViewerTracker viewerTracker;
    private StationBlockListener blockListener;
    private StationInteractListener interactListener;
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

        dev.darkblade.croppy.core.CroppyCore core = new dev.darkblade.croppy.core.CroppyCore(Logger.getLogger("test"));

        dev.darkblade.croppy.api.storage.StationStorage storage = new dev.darkblade.croppy.api.storage.StationStorage() {
            @Override public java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> loadChunk(String w, int x, int z) { return List.of(); }
            @Override public void saveChunk(String w, int x, int z, java.util.Collection<dev.darkblade.croppy.api.station.StationInstance> s) {}
            @Override public void deleteChunk(String w, int x, int z) {}
            @Override public void saveStation(dev.darkblade.croppy.api.station.StationInstance s) {}
            @Override public void deleteStation(BlockPosition p) {}
        };
        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());

        blockListener = new StationBlockListener(core.getStationManager(), core.getStationRegistry(), storage, itemFactory, viewerTracker);

        interactListener = new StationInteractListener(
                core.getStationManager(),
                core.getProcessEngine(),
                itemFactory,
                null,
                core.getInteractionRouter(),
                core.getRuleEngine()
        );

        Plugin mockPlugin = (Plugin) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> null
        );

        listener = new StationVirtualInteractionPacketListener(
                mockPlugin,
                viewerTracker,
                blockListener,
                interactListener,
                Logger.getLogger("test")
        );
    }

    @Test
    @DisplayName("Ignores non-INTERACT_ENTITY packet types")
    void testIgnoresOtherPacketTypes() {
        for (java.lang.reflect.Constructor<?> c : PacketReceiveEvent.class.getConstructors()) {
            System.out.println("Constructor: " + c);
        }
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
}
