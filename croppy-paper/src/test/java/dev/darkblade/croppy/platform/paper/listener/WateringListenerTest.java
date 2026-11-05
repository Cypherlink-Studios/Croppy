package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WateringListenerTest {

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

    public static class TestItemStack extends ItemStack {
        private ItemMeta meta;

        @Override
        public boolean hasItemMeta() {
            return meta != null;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }

        @Override
        public boolean setItemMeta(ItemMeta itemMeta) {
            this.meta = itemMeta;
            return true;
        }
    }

    private Plugin mockPlugin;
    private WateringCanService wateringCanService;
    private CropManager cropManager;
    private WateringListener listener;
    private World mockWorld;

    @BeforeEach
    void setUp() {
        mockPlugin = (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "croppy";
                    return null;
                }
        );

        mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "world";
                    if ("getFullTime".equals(method.getName())) return 24000L;
                    if ("playSound".equals(method.getName())) return null;
                    if ("spawnParticle".equals(method.getName())) return null;
                    if ("getBlockAt".equals(method.getName())) return createMockBlock(Material.AIR, null, null);
                    return null;
                }
        );

        wateringCanService = new WateringCanService(mockPlugin);
        cropManager = new CropManager();
        listener = new WateringListener(wateringCanService, cropManager, null);
    }

    private Block createMockBlock(Material material, BlockData data, Location location) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if ("getType".equals(method.getName())) return material;
                    if ("getBlockData".equals(method.getName())) return data;
                    if ("getLocation".equals(method.getName())) return location != null ? location : new Location(mockWorld, 10, 64, 10);
                    if ("getX".equals(method.getName())) return location != null ? location.getBlockX() : 10;
                    if ("getY".equals(method.getName())) return location != null ? location.getBlockY() : 64;
                    if ("getZ".equals(method.getName())) return location != null ? location.getBlockZ() : 10;
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("equals".equals(method.getName())) return proxy == args[0];
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    return null;
                }
        );
    }

    private Block createMockBlockWithRelative(Material material, BlockData data, Location location, Map<BlockFace, Block> relatives) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if ("getType".equals(method.getName())) return material;
                    if ("getBlockData".equals(method.getName())) return data;
                    if ("getLocation".equals(method.getName())) return location != null ? location : new Location(mockWorld, 10, 64, 10);
                    if ("getX".equals(method.getName())) return location != null ? location.getBlockX() : 10;
                    if ("getY".equals(method.getName())) return location != null ? location.getBlockY() : 64;
                    if ("getZ".equals(method.getName())) return location != null ? location.getBlockZ() : 10;
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("getRelative".equals(method.getName())) {
                        BlockFace face = (BlockFace) args[0];
                        return relatives != null ? relatives.get(face) : null;
                    }
                    if ("equals".equals(method.getName())) return proxy == args[0];
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    return null;
                }
        );
    }

    private Levelled createMockLevelled(int level) {
        return (Levelled) Proxy.newProxyInstance(
                Levelled.class.getClassLoader(),
                new Class<?>[]{Levelled.class},
                (proxy, method, args) -> {
                    if ("getLevel".equals(method.getName())) return level;
                    return null;
                }
        );
    }

    private Waterlogged createMockWaterlogged(boolean isWaterlogged) {
        return (Waterlogged) Proxy.newProxyInstance(
                Waterlogged.class.getClassLoader(),
                new Class<?>[]{Waterlogged.class},
                (proxy, method, args) -> {
                    if ("isWaterlogged".equals(method.getName())) return isWaterlogged;
                    return null;
                }
        );
    }

    private Player createMockPlayer(RayTraceResult rayTrace) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("rayTraceBlocks".equals(method.getName())) return rayTrace;
                    if ("getLocation".equals(method.getName())) return new Location(mockWorld, 10, 65, 10);
                    if ("getFacing".equals(method.getName())) return BlockFace.NORTH;
                    if ("sendActionBar".equals(method.getName())) return null;
                    if ("playSound".equals(method.getName())) return null;
                    if ("equals".equals(method.getName())) return proxy == args[0];
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    return null;
                }
        );
    }

    private ItemStack createMockWateringCan(String tier, int waterUses) {
        Map<NamespacedKey, Object> pdcMap = new HashMap<>();
        pdcMap.put(new NamespacedKey("croppy", "watering_can_tier"), tier);
        pdcMap.put(new NamespacedKey("croppy", "water_uses"), waterUses);
        pdcMap.put(new NamespacedKey("croppy", "water_level"), waterUses);
        pdcMap.put(new NamespacedKey("croppy", "category"), "watering_cans");

        PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        return pdcMap.get(key);
                    } else if ("has".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        return pdcMap.containsKey(key);
                    } else if ("set".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        Object val = args[2];
                        pdcMap.put(key, val);
                        return null;
                    }
                    return null;
                }
        );

        ItemMeta meta = (ItemMeta) Proxy.newProxyInstance(
                ItemMeta.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> {
                    if ("getPersistentDataContainer".equals(method.getName())) return pdc;
                    if ("setLore".equals(method.getName())) return null;
                    if ("lore".equals(method.getName())) return null;
                    return null;
                }
        );

        try {
            TestItemStack stack = (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
            stack.meta = meta;
            return stack;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Verify isWaterSource identifies source water vs flowing water")
    void testIsWaterSourceLevelledWater() {
        Block sourceWater = createMockBlock(Material.WATER, createMockLevelled(0), null);
        assertTrue(listener.isWaterSource(sourceWater), "Water with level 0 must be recognized as water source");

        Block flowingWater = createMockBlock(Material.WATER, createMockLevelled(3), null);
        assertFalse(listener.isWaterSource(flowingWater), "Flowing water with level > 0 must not be a refill source");
    }

    @Test
    @DisplayName("Verify isWaterSource identifies water cauldron and waterlogged blocks")
    void testIsWaterSourceCauldronAndWaterlogged() {
        Block cauldron = createMockBlock(Material.WATER_CAULDRON, null, null);
        assertTrue(listener.isWaterSource(cauldron), "WATER_CAULDRON must be recognized as water source");

        Block waterloggedStair = createMockBlock(Material.OAK_STAIRS, createMockWaterlogged(true), null);
        assertTrue(listener.isWaterSource(waterloggedStair), "Waterlogged block must be recognized as water source");

        Block dryStair = createMockBlock(Material.OAK_STAIRS, createMockWaterlogged(false), null);
        assertFalse(listener.isWaterSource(dryStair), "Non-waterlogged block must not be recognized as water source");
    }

    @Test
    @DisplayName("Verify isWaterSource rejects non-water solid blocks and null")
    void testIsWaterSourceNonWater() {
        assertFalse(listener.isWaterSource(null), "Null block must return false");

        Block dirt = createMockBlock(Material.DIRT, null, null);
        assertFalse(listener.isWaterSource(dirt), "DIRT must not be a water source");

        Block farmland = createMockBlock(Material.FARMLAND, null, null);
        assertFalse(listener.isWaterSource(farmland), "FARMLAND must not be a water source");
    }

    @Test
    @DisplayName("Verify findTargetWaterSource prioritizes direct server fluid raytrace")
    void testFindTargetWaterSourceRayTraceDirectHit() {
        Block waterBlock = createMockBlock(Material.WATER, createMockLevelled(0), new Location(mockWorld, 10, 64, 10));
        RayTraceResult rayTrace = new RayTraceResult(new Vector(10.5, 64.8, 10.5), waterBlock, BlockFace.UP);
        Player player = createMockPlayer(rayTrace);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, createMockWateringCan("copper", 0), null, null, EquipmentSlot.HAND);

        Block target = listener.findTargetWaterSource(player, event);
        assertNotNull(target, "Raytraced water block must be detected");
        assertSame(waterBlock, target);
    }

    @Test
    @DisplayName("Verify findTargetWaterSource falls back to adjacent block for 1-block puddle (bug repro)")
    void testFindTargetWaterSourceFallbackAdjacentBlock() {
        // Player crosshair passed through water and clicked the dirt block at the bottom with face UP
        Player player = createMockPlayer(null); // Raytrace didn't hit or wasn't available
        Block waterAbove = createMockBlock(Material.WATER, createMockLevelled(0), new Location(mockWorld, 10, 64, 10));
        Block dirtBelow = createMockBlockWithRelative(
                Material.DIRT,
                null,
                new Location(mockWorld, 10, 63, 10),
                Map.of(BlockFace.UP, waterAbove)
        );

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                createMockWateringCan("copper", 0),
                dirtBelow,
                BlockFace.UP,
                EquipmentSlot.HAND
        );

        Block target = listener.findTargetWaterSource(player, event);
        assertNotNull(target, "Adjacent water block on BlockFace.UP must be resolved as fallback");
        assertSame(waterAbove, target);
    }

    @Test
    @DisplayName("Verify findTargetWaterSource returns null when no water is targeted")
    void testFindTargetWaterSourceReturnsNullWhenNoWater() {
        Player player = createMockPlayer(null);
        Block farmland = createMockBlockWithRelative(
                Material.FARMLAND,
                null,
                new Location(mockWorld, 10, 64, 10),
                Map.of(BlockFace.UP, createMockBlock(Material.AIR, null, null))
        );

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                createMockWateringCan("copper", 10),
                farmland,
                BlockFace.UP,
                EquipmentSlot.HAND
        );

        Block target = listener.findTargetWaterSource(player, event);
        assertNull(target, "No water source should be found on dry farmland");
    }

    @Test
    @DisplayName("Verify onPlayerInteract refills empty watering can when clicking water")
    void testRefillEmptyWateringCanOnWaterClick() {
        Block waterBlock = createMockBlock(Material.WATER, createMockLevelled(0), new Location(mockWorld, 10, 64, 10));
        RayTraceResult rayTrace = new RayTraceResult(new Vector(10.5, 64.8, 10.5), waterBlock, BlockFace.UP);
        Player player = createMockPlayer(rayTrace);

        ItemStack emptyCan = createMockWateringCan("copper", 0);
        assertEquals(0, wateringCanService.getWaterLevel(emptyCan));

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, emptyCan, waterBlock, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Event must be cancelled after refill");
        assertEquals(40, wateringCanService.getWaterLevel(emptyCan), "Watering can must be refilled to tier capacity (40)");
    }

    @Test
    @DisplayName("Verify onPlayerInteract refills empty watering can via RIGHT_CLICK_AIR on deep water")
    void testRefillEmptyWateringCanOnRightClickAir() {
        Block deepWater = createMockBlock(Material.WATER, createMockLevelled(0), new Location(mockWorld, 10, 64, 10));
        RayTraceResult rayTrace = new RayTraceResult(new Vector(10.5, 64.8, 10.5), deepWater, BlockFace.UP);
        Player player = createMockPlayer(rayTrace);

        ItemStack emptyCan = createMockWateringCan("rusty", 0);
        assertEquals(0, wateringCanService.getWaterLevel(emptyCan));

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, emptyCan, null, null, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertEquals(Event.Result.DENY, event.useItemInHand(), "Item use must be denied after refill on RIGHT_CLICK_AIR");
        assertEquals(20, wateringCanService.getWaterLevel(emptyCan), "Rusty watering can must be refilled to 20");
    }

    @Test
    @DisplayName("Verify onPlayerInteract ignores RIGHT_CLICK_AIR when no water is in sight")
    void testRightClickAirWithNoWaterReturnsCleanly() {
        Player player = createMockPlayer(null); // Look into sky
        ItemStack can = createMockWateringCan("copper", 0);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, can, null, null, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertNotEquals(Event.Result.DENY, event.useItemInHand(), "Item in hand must not be denied for air click without water");
        assertEquals(0, wateringCanService.getWaterLevel(can), "Water level should not change");
    }

    @Test
    @DisplayName("Verify onPlayerInteract waters multi-block crop when clicking secondary Y+1 block")
    void testWaterMultiBlockCropAtSecondaryCoordinate() {
        BlockPosition rootPos = BlockPosition.of("world", 10, 64, 10);
        BlockPosition secondaryPos = BlockPosition.of("world", 10, 65, 10);
        dev.darkblade.croppy.api.crop.CropInstance crop = new dev.darkblade.croppy.core.crop.DefaultCropInstance(
                dev.darkblade.croppy.api.identifier.Key.of("croppy", "grape"), rootPos
        );
        crop.setLastWateredDay(-1L);
        cropManager.registerCrop(crop, 2);

        Block clickedBlock = createMockBlock(Material.AIR, null, new Location(mockWorld, 10, 65, 10));
        Player player = createMockPlayer(null);
        ItemStack can = createMockWateringCan("copper", 10);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, can, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Event should be cancelled after watering");
        assertEquals(1L, crop.getLastWateredDay(), "Crop lastWateredDay should be updated from secondary block interaction");
        assertEquals(9, wateringCanService.getWaterLevel(can), "Water level should decrease by 1");
    }
}
