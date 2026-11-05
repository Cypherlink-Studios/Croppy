package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.RotationMode;
import dev.darkblade.croppy.api.species.SpatialFootprint;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.species.SpeciesDefinition;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropPlantListenerTest {

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
        private Material type = Material.AIR;

        @Override
        public boolean hasItemMeta() {
            return meta != null;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }

        @Override
        public Material getType() {
            return type;
        }
    }

    private TestItemStack createTestItemStack(Material material) {
        try {
            TestItemStack stack = (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
            stack.type = material;
            return stack;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CroppyCore core;
    private CropPlantListener listener;

    @BeforeEach
    void setUp() {
        core = new CroppyCore();
        core.initialize();
        listener = new CropPlantListener(core);
    }

    private Block createMockBlock(String worldName, int x, int y, int z) {
        return createMockBlock(worldName, x, y, z, org.bukkit.Material.AIR, true, null);
    }

    private Block createMockBlock(
            String worldName,
            int x,
            int y,
            int z,
            org.bukkit.Material material,
            boolean passable,
            java.util.Map<BlockPosition, Block> worldBlocks
    ) {
        World worldProxy = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return worldName;
                    }
                    if ("getBlockAt".equals(method.getName()) && args != null && args.length == 3) {
                        int bx = ((Number) args[0]).intValue();
                        int by = ((Number) args[1]).intValue();
                        int bz = ((Number) args[2]).intValue();
                        if (worldBlocks != null) {
                            Block b = worldBlocks.get(BlockPosition.of(worldName, bx, by, bz));
                            if (b != null) return b;
                        }
                        return createMockBlock(worldName, bx, by, bz, org.bukkit.Material.AIR, true, null);
                    }
                    return null;
                }
        );

        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getWorld" -> worldProxy;
                        case "getX" -> x;
                        case "getY" -> y;
                        case "getZ" -> z;
                        case "getType" -> material;
                        case "isPassable" -> passable;
                        default -> null;
                    };
                }
        );
    }

    private BlockPlaceEvent createMockPlaceEvent(Block placedBlock) throws Exception {
        return createMockPlaceEvent(placedBlock, createTestItemStack(Material.AIR));
    }

    private BlockPlaceEvent createMockPlaceEvent(Block placedBlock, org.bukkit.inventory.ItemStack itemInHand) throws Exception {
        BlockPlaceEvent event = (BlockPlaceEvent) UNSAFE.allocateInstance(BlockPlaceEvent.class);
        Field blockField;
        try {
            blockField = org.bukkit.event.block.BlockEvent.class.getDeclaredField("block");
        } catch (NoSuchFieldException e) {
            blockField = BlockPlaceEvent.class.getDeclaredField("block");
        }
        blockField.setAccessible(true);
        blockField.set(event, placedBlock);

        try {
            Field itemField = BlockPlaceEvent.class.getDeclaredField("itemInHand");
            itemField.setAccessible(true);
            itemField.set(event, itemInHand);
        } catch (Exception ignored) {
        }

        return event;
    }

    @Test
    @DisplayName("Safeguard cancels block placement in vertical footprint of multi-block crop")
    void testSafeguardCancelsMultiBlockOccupancy() throws Exception {
        BlockPosition rootPos = BlockPosition.of("test_world", 10, 64, 20);
        CropInstance multiBlockCrop = new DefaultCropInstance(Key.croppy("grapes"), rootPos);

        // Register multi-block crop with height 2 (occupying Y=64 and Y=65)
        core.getCropManager().registerCrop(multiBlockCrop, 2);

        // 1. Placing at root position Y=64 must be cancelled
        Block rootBlock = createMockBlock("test_world", 10, 64, 20);
        BlockPlaceEvent eventRoot = createMockPlaceEvent(rootBlock);
        listener.onBlockCollisionSafeguard(eventRoot);
        assertTrue(eventRoot.isCancelled(), "Block placement at root crop coordinate must be cancelled");

        // 2. Placing at secondary position Y=65 must also be cancelled!
        Block aboveBlock = createMockBlock("test_world", 10, 65, 20);
        BlockPlaceEvent eventAbove = createMockPlaceEvent(aboveBlock);
        listener.onBlockCollisionSafeguard(eventAbove);
        assertTrue(eventAbove.isCancelled(), "Block placement at secondary occupied coordinate Y+1 must be cancelled");

        // 3. Placing at unoccupied position Y=66 must NOT be cancelled
        Block unoccupiedBlock = createMockBlock("test_world", 10, 66, 20);
        BlockPlaceEvent eventUnoccupied = createMockPlaceEvent(unoccupiedBlock);
        listener.onBlockCollisionSafeguard(eventUnoccupied);
        assertFalse(eventUnoccupied.isCancelled(), "Block placement outside footprint must not be cancelled");
    }

    @Test
    @DisplayName("Planting multi-block crop is cancelled when its vertical footprint is already occupied by a crop")
    void testPlantingMultiBlockCropCancelledWhenFootprintOccupied() throws Exception {
        // Register species with height = 2
        Species grapes = new SpeciesDefinition(
                Key.croppy("grapes"),
                "Grapes",
                new CropDisplay("model_3d", "minecraft:air", "minecraft:wheat_seeds", RotationMode.CARDINAL_4),
                List.of(new StageDefinition("mature", 1, 0)),
                new CropRequirements(0, 0),
                new HarvestDefinition(List.of()),
                SpatialFootprint.ofHeight(2)
        );
        core.getSpeciesRegistry().register(grapes);

        // Pre-occupy Y=65 with another crop
        BlockPosition obstacleCropPos = BlockPosition.of("test_world", 10, 65, 20);
        core.getCropManager().registerCrop(new DefaultCropInstance(Key.croppy("wheat"), obstacleCropPos));

        // Attempt to plant grapes at Y=64 (spanning Y=64 and Y=65)
        Block placedBlock = createMockBlock("test_world", 10, 64, 20);
        ItemStack seedItem = createTestItemStack(Material.WHEAT_SEEDS);
        BlockPlaceEvent plantEvent = createMockPlaceEvent(placedBlock, seedItem);

        listener.onBlockPlace(plantEvent);

        assertTrue(plantEvent.isCancelled(), "Planting must be cancelled when vertical footprint intersects an existing crop");
        assertTrue(core.getCropManager().getCrop(BlockPosition.of("test_world", 10, 64, 20)).isEmpty(),
                "No crop instance should be registered when plant is cancelled");
    }

    @Test
    @DisplayName("Planting multi-block crop is cancelled when an impassable solid obstacle exists in secondary coordinates")
    void testPlantingMultiBlockCropCancelledWhenPhysicalObstaclePresent() throws Exception {
        Species grapes = new SpeciesDefinition(
                Key.croppy("grapes"),
                "Grapes",
                new CropDisplay("model_3d", "minecraft:air", "minecraft:wheat_seeds", RotationMode.CARDINAL_4),
                List.of(new StageDefinition("mature", 1, 0)),
                new CropRequirements(0, 0),
                new HarvestDefinition(List.of()),
                SpatialFootprint.ofHeight(2)
        );
        core.getSpeciesRegistry().register(grapes);

        // Y=65 has a solid stone block
        Map<BlockPosition, Block> worldBlocks = new HashMap<>();
        BlockPosition stonePos = BlockPosition.of("test_world", 10, 65, 20);
        worldBlocks.put(stonePos, createMockBlock("test_world", 10, 65, 20, Material.STONE, false, null));

        Block placedBlock = createMockBlock("test_world", 10, 64, 20, Material.AIR, true, worldBlocks);
        ItemStack seedItem = createTestItemStack(Material.WHEAT_SEEDS);
        BlockPlaceEvent plantEvent = createMockPlaceEvent(placedBlock, seedItem);

        listener.onBlockPlace(plantEvent);

        assertTrue(plantEvent.isCancelled(), "Planting must be cancelled when an impassable block is in the vertical space");
        assertTrue(core.getCropManager().getCrop(BlockPosition.of("test_world", 10, 64, 20)).isEmpty(),
                "No crop instance should be registered when space is obstructed");
    }
}
