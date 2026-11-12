package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayLayerConfig;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.display.PacketEventsStationRenderer;
import dev.darkblade.croppy.platform.paper.station.display.PacketStationDisplay;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
import dev.darkblade.croppy.platform.paper.station.display.VisualTier;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StationPlayerConnectionListenerTest {

    private DefaultStationManager stationManager;
    private StationViewerTracker viewerTracker;
    private StationPlayerConnectionListener listener;

    private List<Location> blockChangeLocations;
    private List<BlockData> blockChangeData;
    private Player mockPlayer;
    private Chunk mockChunk;
    private World mockWorld;
    private BlockData mockBarrierData;

    @BeforeEach
    void setUp() {
        stationManager = new DefaultStationManager();
        blockChangeLocations = new ArrayList<>();
        blockChangeData = new ArrayList<>();

        mockBarrierData = (BlockData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{BlockData.class},
                (proxy, method, args) -> {
                    if ("getMaterial".equals(method.getName())) return Material.BARRIER;
                    return null;
                }
        );

        mockWorld = (World) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "world";
                    if ("getPlayers".equals(method.getName())) return List.of(mockPlayer);
                    if ("isChunkLoaded".equals(method.getName())) return true;
                    return null;
                }
        );

        Server mockServer = (Server) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if ("createBlockData".equals(method.getName())) return mockBarrierData;
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    return null;
                }
        );

        mockPlayer = (Player) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("isOnline".equals(method.getName())) return true;
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("getLocation".equals(method.getName())) return new Location(mockWorld, 10.0, 64.0, 10.0);
                    if ("getUniqueId".equals(method.getName())) return UUID.fromString("00000000-0000-0000-0000-000000000001");
                    if ("getName".equals(method.getName())) return "TestPlayer";
                    if ("sendBlockChange".equals(method.getName())) {
                        blockChangeLocations.add((Location) args[0]);
                        blockChangeData.add((BlockData) args[1]);
                        return null;
                    }
                    return null;
                }
        );

        mockChunk = (Chunk) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Chunk.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("getX".equals(method.getName())) return 0; // chunkX = 0 -> block x 0..15
                    if ("getZ".equals(method.getName())) return 0; // chunkZ = 0 -> block z 0..15
                    return null;
                }
        );

        PacketEventsStationRenderer testRenderer = new PacketEventsStationRenderer(Logger.getLogger("test")) {
            @Override
            public void spawnBaseModel(Player player, PacketStationDisplay display, DisplayLayerConfig config, org.bukkit.inventory.ItemStack item, BlockData blockData, DisplayTransform transformOverride) {}
            @Override
            public void updateBaseModelMetadata(Player player, PacketStationDisplay display, DisplayLayerConfig config, org.bukkit.inventory.ItemStack item, DisplayTransform transformOverride) {}
            @Override
            public void destroyEntities(Player player, int... entityIds) {}
        };

        viewerTracker = new StationViewerTracker(
                mockServer,
                testRenderer,
                new ItemFactory(null, new DefaultItemRegistry()),
                Logger.getLogger("test")
        );

        listener = new StationPlayerConnectionListener(viewerTracker, null, stationManager);
    }

    @Test
    @DisplayName("PlayerChunkLoadEvent dispatches BARRIER sendBlockChange for stations with hide_base_block")
    void testPlayerChunkLoadMasksHiddenStation() {
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
        BlockPosition pos = new BlockPosition("world", 5, 64, 5); // In chunk (0, 0)
        stationManager.addStation(new DefaultStationInstance(def.key(), pos, def));

        PlayerChunkLoadEvent event = new PlayerChunkLoadEvent(mockChunk, mockPlayer);
        listener.onPlayerChunkLoad(event);

        assertEquals(1, blockChangeLocations.size(), "Should dispatch 1 sendBlockChange");
        assertEquals(5, blockChangeLocations.get(0).getBlockX());
        assertEquals(64, blockChangeLocations.get(0).getBlockY());
        assertEquals(5, blockChangeLocations.get(0).getBlockZ());
        assertSame(mockBarrierData, blockChangeData.get(0));
    }

    @Test
    @DisplayName("PlayerChunkLoadEvent ignores stations with hide_base_block false")
    void testPlayerChunkLoadIgnoresUnhiddenStation() {
        StationDefinition def = new StationDefinition(
                Key.of("croppy", "furnace_station"),
                "Furnace Station",
                "minecraft:furnace",
                StationInteractionType.PHYSICAL,
                4, 2, Key.of("croppy", "furnace"), false,
                List.of(), Map.of(), null, null, List.of()
        );
        BlockPosition pos = new BlockPosition("world", 5, 64, 5);
        stationManager.addStation(new DefaultStationInstance(def.key(), pos, def));

        PlayerChunkLoadEvent event = new PlayerChunkLoadEvent(mockChunk, mockPlayer);
        listener.onPlayerChunkLoad(event);

        assertTrue(blockChangeLocations.isEmpty(), "Unhidden stations should not trigger block changes");
    }

    @Test
    @DisplayName("onPlayerJoin immediately evaluates nearby stations and adds viewer")
    void testOnPlayerJoinEvaluatesNearbyStations() {
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
        BlockPosition pos = new BlockPosition("world", 12, 64, 12); // ~2.8 blocks from player at (10, 64, 10)
        DefaultStationInstance inst = new DefaultStationInstance(def.key(), pos, def);
        inst.setComponent(DisplayComponent.class, new DefaultDisplayComponent());
        stationManager.addStation(inst);

        PlayerJoinEvent joinEvent = new PlayerJoinEvent(mockPlayer, (net.kyori.adventure.text.Component) null);
        listener.onPlayerJoin(joinEvent);

        PacketStationDisplay display = viewerTracker.getDisplay(pos).orElse(null);
        assertNotNull(display, "Display should be created immediately on join");
        assertTrue(display.isViewing(VisualTier.BASE_MODEL, mockPlayer.getUniqueId()), "Player should be viewing base model");
        assertFalse(blockChangeLocations.isEmpty(), "Block change should be sent for hidden station");
    }
}
