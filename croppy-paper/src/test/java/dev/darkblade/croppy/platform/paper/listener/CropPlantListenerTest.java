package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

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

    private CroppyCore core;
    private CropPlantListener listener;

    @BeforeEach
    void setUp() {
        core = new CroppyCore();
        core.initialize();
        listener = new CropPlantListener(core);
    }

    private Block createMockBlock(String worldName, int x, int y, int z) {
        World worldProxy = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return worldName;
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
                        default -> null;
                    };
                }
        );
    }

    private BlockPlaceEvent createMockPlaceEvent(Block placedBlock) throws Exception {
        BlockPlaceEvent event = (BlockPlaceEvent) UNSAFE.allocateInstance(BlockPlaceEvent.class);
        Field blockField;
        try {
            blockField = org.bukkit.event.block.BlockEvent.class.getDeclaredField("block");
        } catch (NoSuchFieldException e) {
            blockField = BlockPlaceEvent.class.getDeclaredField("block");
        }
        blockField.setAccessible(true);
        blockField.set(event, placedBlock);
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
}
