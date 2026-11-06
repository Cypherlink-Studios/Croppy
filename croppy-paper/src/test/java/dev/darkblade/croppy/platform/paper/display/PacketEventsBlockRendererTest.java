package dev.darkblade.croppy.platform.paper.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.station.display.VirtualEntityIdGenerator;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PacketEventsBlockRendererTest {

    private PacketEventsBlockRenderer renderer;
    private List<PacketWrapper<?>> capturedPackets;
    private Player mockOnlinePlayer;
    private Player mockOfflinePlayer;
    private AtomicBoolean onlineState;

    @BeforeEach
    void setUp() {
        VirtualEntityIdGenerator.reset();
        capturedPackets = new ArrayList<>();
        onlineState = new AtomicBoolean(true);

        PlayerManager mockPlayerManager = (PlayerManager) Proxy.newProxyInstance(
                PlayerManager.class.getClassLoader(),
                new Class<?>[]{PlayerManager.class},
                (proxy, method, args) -> {
                    if ("sendPacket".equals(method.getName())) {
                        if (args != null && args.length >= 2 && args[1] instanceof PacketWrapper<?> wrapper) {
                            capturedPackets.add(wrapper);
                        }
                    }
                    return null;
                }
        );

        ServerManager mockServerManager = (ServerManager) Proxy.newProxyInstance(
                ServerManager.class.getClassLoader(),
                new Class<?>[]{ServerManager.class},
                (proxy, method, args) -> {
                    if ("getVersion".equals(method.getName())) {
                        return ServerVersion.V_1_21_8;
                    }
                    return null;
                }
        );

        PacketEvents.setAPI(new PacketEventsAPI<Object>() {
            @Override public boolean isLoaded() { return true; }
            @Override public void init() {}
            @Override public boolean isInitialized() { return true; }
            @Override public boolean isTerminated() { return false; }
            @Override public Object getPlugin() { return null; }
            @Override public ServerManager getServerManager() { return mockServerManager; }
            @Override public ProtocolManager getProtocolManager() { return null; }
            @Override public PlayerManager getPlayerManager() { return mockPlayerManager; }
            @Override public NettyManager getNettyManager() { return null; }
            @Override public ChannelInjector getInjector() { return null; }
        });

        renderer = new PacketEventsBlockRenderer(Logger.getLogger("test"));

        UUID onlineUuid = UUID.randomUUID();
        mockOnlinePlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getUniqueId" -> onlineUuid;
                        case "getName" -> "OnlineTester";
                        case "isOnline" -> onlineState.get();
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
        );

        UUID offlineUuid = UUID.randomUUID();
        mockOfflinePlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getUniqueId" -> offlineUuid;
                        case "getName" -> "OfflineTester";
                        case "isOnline" -> false;
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
        );
    }

    @Test
    @DisplayName("spawnBlockDisplay dispatches spawn and metadata PacketWrappers for ItemDisplay and Interaction")
    void testSpawnBlockDisplayDispatchesCorrectPackets() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 20);
        Model3DDefinition model = new Model3DDefinition(
                Key.of("croppy", "grape_stage_0"),
                1.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 0.0f,
                0.6f, 1.2f
        );
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 90.0f, model);

        renderer.spawnBlockDisplay(mockOnlinePlayer, display, null);

        assertEquals(4, capturedPackets.size(), "Should dispatch 2 spawn packets and 2 metadata packets");

        // 1. ItemDisplay spawn packet
        assertTrue(capturedPackets.get(0) instanceof WrapperPlayServerSpawnEntity);
        WrapperPlayServerSpawnEntity itemSpawn = (WrapperPlayServerSpawnEntity) capturedPackets.get(0);
        assertEquals(display.getItemDisplayId(), itemSpawn.getEntityId());
        assertEquals(EntityTypes.ITEM_DISPLAY, itemSpawn.getEntityType());
        assertEquals(10.5, itemSpawn.getPosition().getX(), 1e-4);
        assertEquals(64.5, itemSpawn.getPosition().getY(), 1e-4);
        assertEquals(20.5, itemSpawn.getPosition().getZ(), 1e-4);

        // 2. Interaction spawn packet
        assertTrue(capturedPackets.get(1) instanceof WrapperPlayServerSpawnEntity);
        WrapperPlayServerSpawnEntity interactionSpawn = (WrapperPlayServerSpawnEntity) capturedPackets.get(1);
        assertEquals(display.getInteractionId(), interactionSpawn.getEntityId());
        assertEquals(EntityTypes.INTERACTION, interactionSpawn.getEntityType());
        assertEquals(10.5, interactionSpawn.getPosition().getX(), 1e-4);
        assertEquals(64.0, interactionSpawn.getPosition().getY(), 1e-4);
        assertEquals(20.5, interactionSpawn.getPosition().getZ(), 1e-4);

        // 3. ItemDisplay metadata packet
        assertTrue(capturedPackets.get(2) instanceof WrapperPlayServerEntityMetadata);
        WrapperPlayServerEntityMetadata itemMeta = (WrapperPlayServerEntityMetadata) capturedPackets.get(2);
        assertEquals(display.getItemDisplayId(), itemMeta.getEntityId());

        // 4. Interaction metadata packet
        assertTrue(capturedPackets.get(3) instanceof WrapperPlayServerEntityMetadata);
        WrapperPlayServerEntityMetadata interactionMeta = (WrapperPlayServerEntityMetadata) capturedPackets.get(3);
        assertEquals(display.getInteractionId(), interactionMeta.getEntityId());
    }

    @Test
    @DisplayName("updateBlockDisplay dispatches metadata packets for both entities")
    void testUpdateBlockDisplayDispatchesMetadataPackets() {
        BlockPosition pos = BlockPosition.of("world", 5, 70, 5);
        Model3DDefinition model = new Model3DDefinition(
                Key.of("croppy", "tomato_stage_1"),
                1.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 0.0f,
                0.8f, 1.5f
        );
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 0.0f, model);

        renderer.updateBlockDisplay(mockOnlinePlayer, display, null, true);

        assertEquals(2, capturedPackets.size(), "Should dispatch 2 metadata packets");
        assertTrue(capturedPackets.get(0) instanceof WrapperPlayServerEntityMetadata);
        assertTrue(capturedPackets.get(1) instanceof WrapperPlayServerEntityMetadata);

        WrapperPlayServerEntityMetadata itemMeta = (WrapperPlayServerEntityMetadata) capturedPackets.get(0);
        assertEquals(display.getItemDisplayId(), itemMeta.getEntityId());

        WrapperPlayServerEntityMetadata interactionMeta = (WrapperPlayServerEntityMetadata) capturedPackets.get(1);
        assertEquals(display.getInteractionId(), interactionMeta.getEntityId());
    }

    @Test
    @DisplayName("destroyBlockDisplay dispatches WrapperPlayServerDestroyEntities containing all entity IDs")
    void testDestroyBlockDisplayDispatchesDestroyPacket() {
        BlockPosition pos = BlockPosition.of("world", 0, 60, 0);
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 0.0f, null);

        renderer.destroyBlockDisplay(mockOnlinePlayer, display);

        assertEquals(1, capturedPackets.size(), "Should dispatch 1 destroy packet");
        assertTrue(capturedPackets.get(0) instanceof WrapperPlayServerDestroyEntities);

        WrapperPlayServerDestroyEntities destroyPacket = (WrapperPlayServerDestroyEntities) capturedPackets.get(0);
        int[] entityIds = destroyPacket.getEntityIds();
        assertEquals(2, entityIds.length);
        assertEquals(display.getItemDisplayId(), entityIds[0]);
        assertEquals(display.getInteractionId(), entityIds[1]);
    }

    @Test
    @DisplayName("sendPacket ignores offline or null players")
    void testSendPacketIgnoresOfflinePlayers() {
        BlockPosition pos = BlockPosition.of("world", 0, 60, 0);
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 0.0f, null);

        renderer.destroyBlockDisplay(mockOfflinePlayer, display);
        assertEquals(0, capturedPackets.size(), "Should not dispatch packets to offline players");

        renderer.sendPacket(null, new WrapperPlayServerDestroyEntities(new int[]{123}));
        assertEquals(0, capturedPackets.size(), "Should not dispatch packets to null player");
    }
}
