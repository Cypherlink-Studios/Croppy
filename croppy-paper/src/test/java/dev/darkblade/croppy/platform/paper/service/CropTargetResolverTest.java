package dev.darkblade.croppy.platform.paper.service;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.display.PacketEventsBlockRenderer;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplay;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CropTargetResolverTest {

    private CropManager cropManager;
    private VirtualBlockDisplayTracker displayTracker;
    private CropTargetResolver targetResolver;
    private World mockWorld;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("test");
        cropManager = new CropManager();

        Server mockServer = (Server) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> null
        );

        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());
        displayTracker = new VirtualBlockDisplayTracker(
                mockServer,
                new PacketEventsBlockRenderer(logger),
                itemFactory,
                logger
        );

        targetResolver = new CropTargetResolver(cropManager, displayTracker);

        mockWorld = (World) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "world";
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Successfully resolve 3D crop by raytracing virtual interaction entity hitbox")
    void testResolveVirtualEntityHit() {
        BlockPosition cropPos = BlockPosition.of("world", 10, 64, 10);
        CropInstance crop = new DefaultCropInstance(
                Key.croppy("tomato"),
                cropPos,
                1,
                0.5,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        cropManager.registerCrop(crop);

        Model3DDefinition model = Model3DDefinition.of(Key.croppy("tomato_model"), 0.8f, 1.0f);
        displayTracker.getOrCreateDisplay(cropPos, 0.0f, model);

        // Player eyes at (10.5, 64.5, 7.0), looking straight along +Z axis towards (10.5, 64.5, 10.5)
        Vector start = new Vector(10.5, 64.5, 7.0);
        Vector dir = new Vector(0.0, 0.0, 1.0);

        Optional<CropTargetResolver.TargetResult> resultOpt = targetResolver.resolveTarget(mockWorld, start, dir, 5.0);

        assertTrue(resultOpt.isPresent(), "Expected crop to be resolved via virtual entity raycast");
        CropTargetResolver.TargetResult result = resultOpt.get();
        assertEquals(cropPos, result.cropPosition());
        assertSame(crop, result.crop());
        assertEquals(CropTargetResolver.TargetResult.TargetType.VIRTUAL_ENTITY, result.targetType());
    }

    @Test
    @DisplayName("Raycast misses when looking away from virtual interaction entity")
    void testResolveVirtualEntityMiss() {
        BlockPosition cropPos = BlockPosition.of("world", 10, 64, 10);
        CropInstance crop = new DefaultCropInstance(
                Key.croppy("tomato"),
                cropPos,
                1,
                0.5,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        cropManager.registerCrop(crop);

        Model3DDefinition model = Model3DDefinition.of(Key.croppy("tomato_model"), 0.8f, 1.0f);
        displayTracker.getOrCreateDisplay(cropPos, 0.0f, model);

        // Player eyes at (10.5, 64.5, 7.0), but looking along +X axis
        Vector start = new Vector(10.5, 64.5, 7.0);
        Vector dir = new Vector(1.0, 0.0, 0.0);

        Optional<CropTargetResolver.TargetResult> resultOpt = targetResolver.resolveTarget(mockWorld, start, dir, 5.0);

        assertTrue(resultOpt.isEmpty(), "Ray looking away must not resolve crop");
    }

    @Test
    @DisplayName("Raycast misses when target is beyond maxDistance")
    void testResolveVirtualEntityOutOfReach() {
        BlockPosition cropPos = BlockPosition.of("world", 10, 64, 20);
        CropInstance crop = new DefaultCropInstance(
                Key.croppy("tomato"),
                cropPos,
                1,
                0.5,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        cropManager.registerCrop(crop);

        Model3DDefinition model = Model3DDefinition.of(Key.croppy("tomato_model"), 0.8f, 1.0f);
        displayTracker.getOrCreateDisplay(cropPos, 0.0f, model);

        // Distance is 10 blocks (from Z=10 to Z=20), maxDistance is 5.0
        Vector start = new Vector(10.5, 64.5, 10.0);
        Vector dir = new Vector(0.0, 0.0, 1.0);

        Optional<CropTargetResolver.TargetResult> resultOpt = targetResolver.resolveTarget(mockWorld, start, dir, 5.0);

        assertTrue(resultOpt.isEmpty(), "Out of reach crop must not be resolved");
    }

    @Test
    @DisplayName("Solid block obstacle occludes virtual interaction entity")
    void testResolveOccludedBySolidBlock() {
        BlockPosition cropPos = BlockPosition.of("world", 10, 64, 10);
        CropInstance crop = new DefaultCropInstance(
                Key.croppy("tomato"),
                cropPos,
                1,
                0.5,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        cropManager.registerCrop(crop);

        Model3DDefinition model = Model3DDefinition.of(Key.croppy("tomato_model"), 0.8f, 1.0f);
        displayTracker.getOrCreateDisplay(cropPos, 0.0f, model);

        // Player at Z=7 looking towards Z=10.5 (crop hitbox starts at Z=10.1, distance ~3.1)
        Vector start = new Vector(10.5, 64.5, 7.0);
        Vector dir = new Vector(0.0, 0.0, 1.0);

        // Mock a solid obstacle at Z=8.5 (distance 1.5 blocks from player)
        World occludedWorld = (World) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "world";
                    }
                    if ("rayTraceBlocks".equals(method.getName())) {
                        boolean ignorePassable = (boolean) args[4];
                        if (ignorePassable) {
                            // Hit solid obstacle at (10.5, 64.5, 8.5)
                            return new RayTraceResult(new Vector(10.5, 64.5, 8.5));
                        }
                    }
                    return null;
                }
        );

        Optional<CropTargetResolver.TargetResult> resultOpt = targetResolver.resolveTarget(occludedWorld, start, dir, 5.0);

        assertTrue(resultOpt.isEmpty(), "Occluded crop behind solid wall must not be resolved");
    }

    @Test
    @DisplayName("Fallback to physical block hit when no virtual display is present")
    void testFallbackToPhysicalBlockHit() {
        BlockPosition cropPos = BlockPosition.of("world", 15, 64, 15);
        CropInstance crop = new DefaultCropInstance(
                Key.croppy("wheat"),
                cropPos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        cropManager.registerCrop(crop);

        Block mockBlock = (Block) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if ("getX".equals(method.getName())) return 15;
                    if ("getY".equals(method.getName())) return 64;
                    if ("getZ".equals(method.getName())) return 15;
                    if ("getType".equals(method.getName())) return Material.WHEAT;
                    return null;
                }
        );

        World blockWorld = (World) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "world";
                    }
                    if ("rayTraceBlocks".equals(method.getName())) {
                        boolean ignorePassable = (boolean) args[4];
                        if (!ignorePassable) {
                            return new RayTraceResult(new Vector(15.5, 64.2, 15.5), mockBlock, BlockFace.UP);
                        }
                    }
                    return null;
                }
        );

        Vector start = new Vector(15.5, 65.5, 12.0);
        Vector dir = new Vector(0.0, -0.3, 1.0);

        Optional<CropTargetResolver.TargetResult> resultOpt = targetResolver.resolveTarget(blockWorld, start, dir, 5.0);

        assertTrue(resultOpt.isPresent());
        CropTargetResolver.TargetResult result = resultOpt.get();
        assertEquals(cropPos, result.cropPosition());
        assertSame(crop, result.crop());
        assertEquals(CropTargetResolver.TargetResult.TargetType.PHYSICAL_BLOCK, result.targetType());
    }

    @Test
    @DisplayName("Fallback to supporting farmland block (Y + 1)")
    void testFallbackToSupportingFarmland() {
        BlockPosition cropPos = BlockPosition.of("world", 20, 64, 20);
        CropInstance crop = new DefaultCropInstance(
                Key.croppy("carrot"),
                cropPos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                0.0f
        );
        cropManager.registerCrop(crop);

        // Player hits farmland at Y=63
        Block mockFarmland = (Block) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if ("getX".equals(method.getName())) return 20;
                    if ("getY".equals(method.getName())) return 63;
                    if ("getZ".equals(method.getName())) return 20;
                    if ("getType".equals(method.getName())) return Material.FARMLAND;
                    return null;
                }
        );

        World farmlandWorld = (World) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return "world";
                    }
                    if ("rayTraceBlocks".equals(method.getName())) {
                        return new RayTraceResult(new Vector(20.5, 63.9, 20.5), mockFarmland, BlockFace.UP);
                    }
                    return null;
                }
        );

        Vector start = new Vector(20.5, 66.0, 18.0);
        Vector dir = new Vector(0.0, -0.8, 0.8);

        Optional<CropTargetResolver.TargetResult> resultOpt = targetResolver.resolveTarget(farmlandWorld, start, dir, 5.0);

        assertTrue(resultOpt.isPresent(), "Expected crop to be resolved above farmland block");
        CropTargetResolver.TargetResult result = resultOpt.get();
        assertEquals(cropPos, result.cropPosition());
        assertSame(crop, result.crop());
        assertEquals(CropTargetResolver.TargetResult.TargetType.SUPPORTING_SOIL, result.targetType());
    }
}
