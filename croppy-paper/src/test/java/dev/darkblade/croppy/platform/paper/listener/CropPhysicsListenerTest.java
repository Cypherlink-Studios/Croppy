package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.platform.paper.display.PacketEventsBlockRenderer;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropPhysicsListenerTest {

    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CroppyCore core;
    private List<BlockPosition> deletedStoragePositions;
    private List<BlockPosition> removedDisplayPositions;
    private CropPhysicsListener listener;
    private Map<BlockPosition, MockBlockState> worldBlocks;

    static class MockBlockState {
        Material material;
        MockBlockState(Material material) {
            this.material = material;
        }
    }

    static class TestDisplayTracker extends VirtualBlockDisplayTracker {
        final List<BlockPosition> removed;

        TestDisplayTracker(List<BlockPosition> removed) {
            super(
                    (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class}, (p, m, a) -> null),
                    createUnsafeInstance(PacketEventsBlockRenderer.class),
                    createUnsafeInstance(ItemFactory.class),
                    Logger.getAnonymousLogger()
            );
            this.removed = removed;
        }

        @Override
        public void removeDisplay(BlockPosition pos) {
            removed.add(pos);
        }

        @SuppressWarnings("unchecked")
        private static <T> T createUnsafeInstance(Class<T> clazz) {
            try {
                return (T) UNSAFE.allocateInstance(clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeEach
    void setUp() {
        core = new CroppyCore();
        core.initialize();

        deletedStoragePositions = new ArrayList<>();
        removedDisplayPositions = new ArrayList<>();
        worldBlocks = new HashMap<>();

        CropStorage mockStorage = new CropStorage() {
            @Override
            public Collection<CropInstance> loadChunk(String worldName, int chunkX, int chunkZ) {
                return List.of();
            }

            @Override
            public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<CropInstance> crops) {}

            @Override
            public void saveCrop(CropInstance crop) {}

            @Override
            public void deleteCrop(BlockPosition position) {
                deletedStoragePositions.add(position);
            }
        };
        core.setStorage(mockStorage);

        TestDisplayTracker tracker = new TestDisplayTracker(removedDisplayPositions);
        listener = new CropPhysicsListener(core, tracker);
    }

    private World createWorldProxy(String worldName) {
        AtomicReference<World> ref = new AtomicReference<>();
        World proxy = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (p, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return worldName;
                    }
                    if ("getBlockAt".equals(method.getName()) && args != null && args.length == 3) {
                        int bx = ((Number) args[0]).intValue();
                        int by = ((Number) args[1]).intValue();
                        int bz = ((Number) args[2]).intValue();
                        BlockPosition targetPos = BlockPosition.of(worldName, bx, by, bz);
                        MockBlockState state = worldBlocks.computeIfAbsent(targetPos, k -> new MockBlockState(Material.AIR));
                        return createMockBlock(worldName, bx, by, bz, state, ref.get());
                    }
                    return null;
                }
        );
        ref.set(proxy);
        return proxy;
    }

    private Block createMockBlock(String worldName, int x, int y, int z, Material material) {
        BlockPosition pos = BlockPosition.of(worldName, x, y, z);
        worldBlocks.put(pos, new MockBlockState(material));
        return createMockBlock(worldName, x, y, z, worldBlocks.get(pos), createWorldProxy(worldName));
    }

    private Block createMockBlock(String worldName, int x, int y, int z, MockBlockState state, World worldProxy) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getWorld" -> worldProxy;
                        case "getX" -> x;
                        case "getY" -> y;
                        case "getZ" -> z;
                        case "getType" -> state.material;
                        case "setType" -> {
                            if (args != null && args.length > 0 && args[0] instanceof Material m) {
                                state.material = m;
                            }
                            yield null;
                        }
                        case "getRelative" -> {
                            if (args != null && args.length > 0 && args[0] instanceof BlockFace face) {
                                int rx = x + face.getModX();
                                int ry = y + face.getModY();
                                int rz = z + face.getModZ();
                                yield worldProxy != null ? worldProxy.getBlockAt(rx, ry, rz) : null;
                            }
                            yield null;
                        }
                        default -> null;
                    };
                }
        );
    }

    @Test
    @DisplayName("Water flow into secondary block removes multi-block crop, deletes canonical root storage and clears displays")
    void testWaterFlowOnSecondaryBlockDestroysCrop() {
        BlockPosition rootPos = BlockPosition.of("world", 10, 64, 10);
        BlockPosition secondaryPos = BlockPosition.of("world", 10, 65, 10);

        CropInstance crop = new DefaultCropInstance(Key.of("croppy", "grape"), rootPos);
        core.getCropManager().registerCrop(crop, 2);

        createMockBlock("world", 10, 64, 10, Material.OAK_SAPLING);
        Block secondaryBlock = createMockBlock("world", 10, 65, 10, Material.AIR);
        Block fromBlock = createMockBlock("world", 10, 65, 11, Material.WATER);

        BlockFromToEvent event = new BlockFromToEvent(fromBlock, secondaryBlock);
        listener.onWaterFlow(event);

        assertFalse(core.getCropManager().isOccupied(rootPos), "Root pos should no longer be occupied");
        assertFalse(core.getCropManager().isOccupied(secondaryPos), "Secondary pos should no longer be occupied");

        assertEquals(1, deletedStoragePositions.size());
        assertEquals(rootPos, deletedStoragePositions.get(0), "Storage delete should resolve to canonical root");

        assertEquals(1, removedDisplayPositions.size());
        assertEquals(rootPos, removedDisplayPositions.get(0), "Display removal should resolve to canonical root");

        assertEquals(Material.AIR, worldBlocks.get(rootPos).material, "Physical base block should be set to AIR");
    }

    @Test
    @DisplayName("Piston extending into secondary block destroys multi-block crop canonically")
    void testPistonExtendOnSecondaryBlock() {
        BlockPosition rootPos = BlockPosition.of("world", 20, 64, 20);
        BlockPosition secondaryPos = BlockPosition.of("world", 20, 65, 20);

        CropInstance crop = new DefaultCropInstance(Key.of("croppy", "corn"), rootPos);
        core.getCropManager().registerCrop(crop, 2);

        createMockBlock("world", 20, 64, 20, Material.OAK_SAPLING);
        Block secondaryBlock = createMockBlock("world", 20, 65, 20, Material.AIR);
        Block pistonBlock = createMockBlock("world", 20, 65, 21, Material.PISTON);

        BlockPistonExtendEvent event = new BlockPistonExtendEvent(pistonBlock, List.of(secondaryBlock), BlockFace.NORTH);
        listener.onPistonExtend(event);

        assertFalse(core.getCropManager().isOccupied(rootPos));
        assertFalse(core.getCropManager().isOccupied(secondaryPos));
        assertEquals(List.of(rootPos), deletedStoragePositions);
        assertEquals(List.of(rootPos), removedDisplayPositions);
        assertEquals(Material.AIR, worldBlocks.get(rootPos).material);
    }

    @Test
    @DisplayName("BlockPhysicsEvent destroys crop when soil beneath root is removed")
    void testBlockPhysicsDestroysCropOnBrokenSoil() {
        BlockPosition rootPos = BlockPosition.of("world", 30, 64, 30);
        CropInstance crop = new DefaultCropInstance(Key.of("croppy", "wheat"), rootPos);
        core.getCropManager().registerCrop(crop, 1);

        Block cropBlock = createMockBlock("world", 30, 64, 30, Material.WHEAT);
        createMockBlock("world", 30, 63, 30, Material.AIR); // Broken soil

        BlockPhysicsEvent event = new BlockPhysicsEvent(cropBlock, null);
        listener.onBlockPhysics(event);

        assertFalse(core.getCropManager().isOccupied(rootPos));
        assertEquals(List.of(rootPos), deletedStoragePositions);
        assertEquals(List.of(rootPos), removedDisplayPositions);
        assertEquals(Material.AIR, worldBlocks.get(rootPos).material);
    }

    @Test
    @DisplayName("Player trample on farmland destroys crop above it")
    void testPlayerTrampleDestroysCrop() {
        BlockPosition rootPos = BlockPosition.of("world", 40, 64, 40);
        CropInstance crop = new DefaultCropInstance(Key.of("croppy", "tomato"), rootPos);
        core.getCropManager().registerCrop(crop, 2);

        createMockBlock("world", 40, 64, 40, Material.OAK_SAPLING);
        Block farmland = createMockBlock("world", 40, 63, 40, Material.FARMLAND);

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (p, m, a) -> null
        );

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.PHYSICAL, null, farmland, BlockFace.UP);
        listener.onPlayerTrample(event);

        assertFalse(core.getCropManager().isOccupied(rootPos));
        assertFalse(core.getCropManager().isOccupied(BlockPosition.of("world", 40, 65, 40)));
        assertEquals(List.of(rootPos), deletedStoragePositions);
        assertEquals(List.of(rootPos), removedDisplayPositions);
        assertEquals(Material.AIR, worldBlocks.get(rootPos).material);
    }
}
