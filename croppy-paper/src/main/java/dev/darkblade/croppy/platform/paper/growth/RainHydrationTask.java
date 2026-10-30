package dev.darkblade.croppy.platform.paper.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;

import java.util.Objects;
import java.util.Set;

/**
 * Periodic task that hydrates active crops during rain storms in precipitation-supporting biomes
 * when crops have clear exposure to the sky.
 */
public class RainHydrationTask implements Runnable {

    private static final Set<Biome> NO_PRECIPITATION_BIOMES = Set.of(
            Biome.DESERT,
            Biome.BADLANDS,
            Biome.ERODED_BADLANDS,
            Biome.WOODED_BADLANDS,
            Biome.SAVANNA,
            Biome.SAVANNA_PLATEAU,
            Biome.WINDSWEPT_SAVANNA,
            Biome.NETHER_WASTES,
            Biome.CRIMSON_FOREST,
            Biome.WARPED_FOREST,
            Biome.SOUL_SAND_VALLEY,
            Biome.BASALT_DELTAS,
            Biome.THE_END,
            Biome.SMALL_END_ISLANDS,
            Biome.END_MIDLANDS,
            Biome.END_HIGHLANDS,
            Biome.END_BARRENS,
            Biome.THE_VOID
    );

    private final Server server;
    private final CropManager cropManager;

    public RainHydrationTask(Server server, CropManager cropManager) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
    }

    @Override
    public void run() {
        for (World world : server.getWorlds()) {
            if (!world.hasStorm()) {
                continue;
            }

            long currentDay = world.getFullTime() / 24000L;

            for (CropInstance crop : cropManager.getAllCrops()) {
                BlockPosition pos = crop.getPosition();
                if (!pos.worldName().equals(world.getName())) {
                    continue;
                }

                // Skip if already watered today
                if (crop.isWateredOn(currentDay)) {
                    continue;
                }

                int chunkX = pos.x() >> 4;
                int chunkZ = pos.z() >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                // Verify biome has precipitation (e.g. deserts and badlands do not rain)
                Biome biome = world.getBiome(pos.x(), pos.y(), pos.z());
                if (NO_PRECIPITATION_BIOMES.contains(biome)) {
                    continue;
                }

                // Verify sky line-of-sight (no roofs, ceilings, or greenhouse covers)
                int highestY = world.getHighestBlockYAt(pos.x(), pos.z(), HeightMap.MOTION_BLOCKING);
                if (highestY <= pos.y()) {
                    crop.setLastWateredDay(currentDay);

                    Block blockBelow = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());
                    if (blockBelow.getType() == Material.FARMLAND && blockBelow.getBlockData() instanceof Farmland farmland) {
                        farmland.setMoisture(7);
                        blockBelow.setBlockData(farmland, false);
                    }
                }
            }
        }
    }
}
