package dev.darkblade.croppy.platform.paper.growth;

import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.EnvironmentProvider;
import dev.darkblade.croppy.api.world.BlockPosition;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

import java.util.Objects;

/**
 * Paper implementation of EnvironmentProvider querying real Bukkit worlds.
 * Ensures chunk-safety and scans the 9x9 horizontal area for water fluid levels (0-8).
 */
public class PaperEnvironmentProvider implements EnvironmentProvider {

    private final Server server;
    private final int searchRadius;

    public PaperEnvironmentProvider(Server server, int searchRadius) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.searchRadius = Math.max(0, searchRadius);
    }

    public PaperEnvironmentProvider(Server server) {
        this(server, 4);
    }

    @Override
    public CropEnvironment getEnvironment(BlockPosition position) {
        Objects.requireNonNull(position, "position must not be null");

        World world = server.getWorld(position.worldName());
        if (world == null) {
            return CropEnvironment.unloaded();
        }

        int chunkX = position.x() >> 4;
        int chunkZ = position.z() >> 4;

        // Ensure central chunk is loaded before accessing blocks
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return CropEnvironment.unloaded();
        }

        Block cropBlock = world.getBlockAt(position.x(), position.y(), position.z());
        int lightLevel = cropBlock.getLightLevel();
        int maxWaterFluidLevel = scanNearbyWaterFluidLevel(world, position.x(), position.y(), position.z());

        return CropEnvironment.of(lightLevel, maxWaterFluidLevel);
    }

    /**
     * Scans nearby blocks in a Chebyshev horizontal radius around the crop for the highest water fluid level.
     * Maps source water / waterlogged blocks to 8, and flowing water to 1-7.
     */
    private int scanNearbyWaterFluidLevel(World world, int centerX, int centerY, int centerZ) {
        int maxFluidLevel = 0;

        // Check farmland height (centerY - 1) and crop height (centerY)
        for (int y = centerY - 1; y <= centerY; y++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    int x = centerX + dx;
                    int z = centerZ + dz;

                    // Ensure target block's chunk is loaded to prevent chunk-thrashing
                    int targetChunkX = x >> 4;
                    int targetChunkZ = z >> 4;
                    if (!world.isChunkLoaded(targetChunkX, targetChunkZ)) {
                        continue;
                    }

                    Block block = world.getBlockAt(x, y, z);
                    int fluidLevel = resolveWaterFluidLevel(block);
                    if (fluidLevel > maxFluidLevel) {
                        maxFluidLevel = fluidLevel;
                        if (maxFluidLevel >= 8) {
                            return 8; // Maximum possible level reached, break early
                        }
                    }
                }
            }
        }

        return maxFluidLevel;
    }

    /**
     * Resolves the water fluid level (0-8) of a given block.
     */
    private int resolveWaterFluidLevel(Block block) {
        BlockData data = block.getBlockData();

        if (data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
            return 8;
        }

        if (block.getType() == Material.WATER) {
            if (data instanceof Levelled levelled) {
                int level = levelled.getLevel();
                // In Minecraft, level 0 is source block; levels 1-7 are flowing water with decreasing height
                return (level == 0) ? 8 : (8 - level);
            }
            return 8;
        }

        return 0;
    }
}
