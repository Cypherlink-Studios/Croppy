package dev.darkblade.croppy.platform.paper.watering;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.fertilizer.FertilizerType;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles daily world day transitions, moisture dry-out, and water-retention rolls for fertilizers.
 */
public class DailyTransitionService {

    private final CropManager cropManager;
    private final FertilizerRegistry fertilizerRegistry;
    private final Random random = new Random();
    private final ConcurrentMap<String, Long> lastKnownDays = new ConcurrentHashMap<>();

    public DailyTransitionService(CropManager cropManager, FertilizerRegistry fertilizerRegistry) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.fertilizerRegistry = Objects.requireNonNull(fertilizerRegistry, "fertilizerRegistry cannot be null");
    }

    /**
     * Checks if a world has advanced to a new day and triggers the transition if so.
     */
    public void checkWorldDay(World world) {
        long currentDay = world.getFullTime() / 24000L;
        Long previousDay = lastKnownDays.putIfAbsent(world.getName(), currentDay);
        if (previousDay != null && currentDay > previousDay) {
            handleTransition(world, previousDay, currentDay);
            lastKnownDays.put(world.getName(), currentDay);
        }
    }

    /**
     * Executes transition logic between two day indices for a given world.
     */
    public void handleTransition(World world, long oldDay, long newDay) {
        if (newDay <= oldDay) return;

        for (CropInstance crop : cropManager.getAllCrops()) {
            BlockPosition pos = crop.getPosition();
            if (!pos.worldName().equals(world.getName())) {
                continue;
            }

            // Check if crop was watered on the prior day
            if (crop.getLastWateredDay() == oldDay) {
                Key fertKey = crop.getFertilizerKey();
                boolean retained = false;

                if (fertKey != null) {
                    Optional<Fertilizer> fertOpt = fertilizerRegistry.get(fertKey);
                    if (fertOpt.isPresent() && fertOpt.get().type() == FertilizerType.RETAINING) {
                        double chance = fertOpt.get().retentionChance();
                        if (random.nextDouble() < chance) {
                            crop.setLastWateredDay(newDay);
                            retained = true;
                        }
                    }
                }

                if (!retained) {
                    int chunkX = pos.x() >> 4;
                    int chunkZ = pos.z() >> 4;
                    if (world.isChunkLoaded(chunkX, chunkZ)) {
                        Block blockBelow = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());
                        if (blockBelow.getType() == Material.FARMLAND && blockBelow.getBlockData() instanceof Farmland farmland) {
                            farmland.setMoisture(0);
                            blockBelow.setBlockData(farmland, false);
                        }
                    }
                }
            }
        }
    }
}
