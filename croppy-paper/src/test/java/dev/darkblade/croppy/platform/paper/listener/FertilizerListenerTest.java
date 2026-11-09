package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FertilizerListenerTest {

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
        private int amount = 1;

        @Override
        public Material getType() {
            return Material.BONE_MEAL;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack subtract(int amt) {
            this.amount -= amt;
            return this;
        }
    }

    private DefaultFertilizerRegistry fertilizerRegistry;
    private CropManager cropManager;
    private FertilizerService fertilizerService;
    private FertilizerListener listener;
    private Fertilizer testFertilizer;
    private World mockWorld;

    @BeforeEach
    void setUp() {
        fertilizerRegistry = new DefaultFertilizerRegistry();
        testFertilizer = new Fertilizer(
                Key.of("croppy", "growth_fertilizer"),
                "Growth Fertilizer",
                List.of("Boosts speed"),
                FertilizerType.SPEED,
                1.25,
                0.0,
                0.0,
                "BONE_MEAL",
                1001
        );
        fertilizerRegistry.register(testFertilizer);

        fertilizerService = new FertilizerService(null, fertilizerRegistry) {
            @Override
            public Optional<Fertilizer> identifyFertilizer(ItemStack item) {
                return Optional.of(testFertilizer);
            }
        };

        cropManager = new CropManager();
        listener = new FertilizerListener(fertilizerService, cropManager, null);

        mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return "world";
                    if ("playSound".equals(method.getName())) return null;
                    if ("spawnParticle".equals(method.getName())) return null;
                    return null;
                }
        );
    }

    private Block createMockBlock(String worldName, int x, int y, int z) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getWorld" -> mockWorld;
                        case "getX" -> x;
                        case "getY" -> y;
                        case "getZ" -> z;
                        case "getLocation" -> new Location(mockWorld, x, y, z);
                        default -> null;
                    };
                }
        );
    }

    private Player createMockPlayer() {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return mockWorld;
                    if ("getLocation".equals(method.getName())) return new Location(mockWorld, 10, 65, 10);
                    if ("playSound".equals(method.getName())) return null;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Applying fertilizer on secondary vertical coordinate (Y+1) fertilizes the multi-block crop")
    void testFertilizeMultiBlockCropAtSecondaryCoordinate() {
        BlockPosition rootPos = BlockPosition.of("world", 10, 64, 10);
        BlockPosition secondaryPos = BlockPosition.of("world", 10, 65, 10);

        CropInstance crop = new DefaultCropInstance(Key.of("croppy", "grape"), rootPos);
        cropManager.registerCrop(crop, 2);

        Block clickedBlock = createMockBlock("world", 10, 65, 10);
        Player player = createMockPlayer();

        TestItemStack item;
        try {
            item = (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
            item.amount = 2;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Event must be cancelled after applying fertilizer");
        assertTrue(crop.hasFertilizer(), "Crop must now have fertilizer applied");
        assertEquals(testFertilizer.getKey(), crop.getFertilizerKey());
        assertEquals(1, item.amount, "Item amount must be decremented by 1");
    }
}
