package dev.darkblade.croppy.platform.paper.display;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.display.VirtualEntityIdGenerator;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualBlockDisplayTrackerTest {

    static class TestBlockRenderer extends PacketEventsBlockRenderer {
        int spawnCount = 0;
        int destroyCount = 0;

        public TestBlockRenderer() {
            super(Logger.getLogger("test"));
        }

        @Override
        public void spawnBlockDisplay(Player player, VirtualBlockDisplay display, ItemStack item) {
            spawnCount++;
        }

        @Override
        public void destroyBlockDisplay(Player player, VirtualBlockDisplay display) {
            destroyCount++;
        }
    }

    private TestBlockRenderer renderer;
    private ItemFactory itemFactory;
    private VirtualBlockDisplayTracker tracker;
    private World mockWorld;
    private Player mockPlayer;
    private UUID playerId;
    private AtomicReference<Location> playerLocation;
    private AtomicBoolean playerOnline;

    @BeforeEach
    void setUp() {
        VirtualEntityIdGenerator.reset();
        renderer = new TestBlockRenderer();
        itemFactory = new ItemFactory(null, new DefaultItemRegistry());

        playerId = UUID.randomUUID();
        playerLocation = new AtomicReference<>(null);
        playerOnline = new AtomicBoolean(true);

        mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getName" -> "world";
                        case "isChunkLoaded" -> true;
                        case "getPlayers" -> playerOnline.get() ? List.of(mockPlayer) : List.of();
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
        );

        mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getUniqueId" -> playerId;
                        case "getWorld" -> mockWorld;
                        case "isOnline" -> playerOnline.get();
                        case "getLocation" -> playerLocation.get();
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
        );

        Server mockServer = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getWorld" -> {
                            if (args != null && "world".equals(args[0])) yield mockWorld;
                            yield null;
                        }
                        case "getPlayer" -> {
                            if (args != null && playerId.equals(args[0])) yield mockPlayer;
                            yield null;
                        }
                        default -> null;
                    };
                }
        );

        tracker = new VirtualBlockDisplayTracker(mockServer, renderer, itemFactory, Logger.getLogger("test"));
    }

    @Test
    @DisplayName("tickViewers spawns display when player approaches within 48 blocks and destroys when departing")
    void testProximityTrackingSpawnAndDestroy() {
        BlockPosition cropPos = BlockPosition.of("world", 100, 64, 100);
        Model3DDefinition model = Model3DDefinition.of(Key.croppy("grape_stage_2"), 1.0f, 2.0f);
        VirtualBlockDisplay display = tracker.getOrCreateDisplay(cropPos, 0.0f, model);

        // 1. Player is far away (distance ~ 100 blocks > 48)
        playerLocation.set(new Location(mockWorld, 200, 64, 100));
        tracker.tickViewers();

        assertFalse(display.isViewing(playerId), "Player should not be viewing when outside range");
        assertEquals(0, renderer.spawnCount, "No spawn packet should be sent when far");

        // 2. Player moves close (distance ~ 10 blocks <= 48)
        playerLocation.set(new Location(mockWorld, 110, 64, 100));
        tracker.tickViewers();

        assertTrue(display.isViewing(playerId), "Player should be added as viewer when in range");
        assertEquals(1, renderer.spawnCount, "Spawn packet must be sent on entering range");
        assertEquals(0, renderer.destroyCount);

        // 3. Player remains close: consecutive ticks should not re-spawn
        tracker.tickViewers();
        assertEquals(1, renderer.spawnCount, "No duplicate spawn packets for player already viewing");
        assertEquals(0, renderer.destroyCount);

        // 4. Player moves away (> 48 blocks)
        playerLocation.set(new Location(mockWorld, 160, 64, 100));
        tracker.tickViewers();

        assertFalse(display.isViewing(playerId), "Player must be removed as viewer when departing");
        assertEquals(1, renderer.destroyCount, "Destroy packet must be sent on departing range");
    }

    @Test
    @DisplayName("tickViewers removes viewer if player disconnects")
    void testPlayerDisconnectCleanup() {
        BlockPosition cropPos = BlockPosition.of("world", 50, 64, 50);
        VirtualBlockDisplay display = tracker.getOrCreateDisplay(cropPos, 0.0f, null);

        // Player is nearby
        playerLocation.set(new Location(mockWorld, 52, 64, 50));
        tracker.tickViewers();
        assertTrue(display.isViewing(playerId));
        assertEquals(1, renderer.spawnCount);

        // Player disconnects
        playerOnline.set(false);
        tracker.tickViewers();

        assertFalse(display.isViewing(playerId), "Disconnected player must be removed as viewer");
    }
}
