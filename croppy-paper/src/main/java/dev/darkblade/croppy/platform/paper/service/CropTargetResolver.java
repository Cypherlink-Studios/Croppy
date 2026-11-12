package dev.darkblade.croppy.platform.paper.service;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplay;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;

/**
 * Service resolving line-of-sight targeted crops for players across virtual interaction entity
 * bounding boxes, physical crop blocks, and supporting farmland with obstacle occlusion checks.
 */
public class CropTargetResolver {

    public static final double DEFAULT_MAX_DISTANCE = 5.0;

    private final CropManager cropManager;
    private final VirtualBlockDisplayTracker displayTracker;
    private final CroppyLogger logger;

    public CropTargetResolver(CropManager cropManager, VirtualBlockDisplayTracker displayTracker) {
        this(cropManager, displayTracker, NoOpCroppyLogger.INSTANCE);
    }

    public CropTargetResolver(CropManager cropManager, VirtualBlockDisplayTracker displayTracker, CroppyLogger logger) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.displayTracker = displayTracker;
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    public CropTargetResolver(CropManager cropManager) {
        this(cropManager, null, NoOpCroppyLogger.INSTANCE);
    }

    public CropManager getCropManager() {
        return cropManager;
    }

    public VirtualBlockDisplayTracker getDisplayTracker() {
        return displayTracker;
    }

    /**
     * Resolves the targeted crop for an in-game player using the default 5.0 block reach distance.
     *
     * @param player executing player
     * @return Optional containing the resolved TargetResult if a crop was hit
     */
    public Optional<TargetResult> resolveTarget(Player player) {
        return resolveTarget(player, DEFAULT_MAX_DISTANCE);
    }

    /**
     * Resolves the targeted crop for an in-game player within the specified maximum distance.
     *
     * @param player      executing player
     * @param maxDistance maximum ray reach distance
     * @return Optional containing the resolved TargetResult if a crop was hit
     */
    public Optional<TargetResult> resolveTarget(Player player, double maxDistance) {
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        Location eye = player.getEyeLocation();
        return resolveTarget(player.getWorld(), eye.toVector(), eye.getDirection(), maxDistance);
    }

    /**
     * Resolves the targeted crop from explicit ray start and direction vectors within a world.
     *
     * @param world       the Bukkit world
     * @param start       ray start origin (player eye location vector)
     * @param direction   ray look direction vector
     * @param maxDistance maximum ray reach distance
     * @return Optional containing the resolved TargetResult if a crop was hit
     */
    public Optional<TargetResult> resolveTarget(World world, Vector start, Vector direction, double maxDistance) {
        if (world == null || start == null || direction == null || maxDistance <= 0.0) {
            return Optional.empty();
        }

        Vector dir = direction.clone().normalize();
        Location startLoc = new Location(world, start.getX(), start.getY(), start.getZ());

        // 1. Raytrace solid blocks to evaluate physical obstacle occlusion
        // ignorePassableBlocks = true ensures passable/transparent blocks (tripwire, air, tall grass)
        // do NOT block line of sight, while solid blocks (stone, wood, dirt, farmland) do.
        double solidBlockDistance = Double.MAX_VALUE;
        RayTraceResult solidBlockHit = null;
        try {
            solidBlockHit = world.rayTraceBlocks(startLoc, dir, maxDistance, FluidCollisionMode.NEVER, true);
            if (solidBlockHit != null && solidBlockHit.getHitPosition() != null) {
                solidBlockDistance = start.distance(solidBlockHit.getHitPosition());
            }
        } catch (Throwable t) {
            logger.trace(LogChannel.CORE,
                    () -> "Solid block raytrace failed in world '" + world.getName() + "'", t);
        }

        // 2. Raytrace nearby virtual interaction entity bounding boxes
        BlockPosition bestVirtualPos = null;
        CropInstance bestVirtualCrop = null;
        double bestVirtualDist = Double.MAX_VALUE;

        if (displayTracker != null) {
            double pruneDistSq = (maxDistance + 1.5) * (maxDistance + 1.5);
            for (VirtualBlockDisplay display : displayTracker.getActiveDisplays().values()) {
                BlockPosition pos = display.getPosition();
                if (!pos.worldName().equals(world.getName())) {
                    continue;
                }

                double centerX = pos.x() + 0.5;
                double centerY = pos.y() + 0.5;
                double centerZ = pos.z() + 0.5;

                double dx = centerX - start.getX();
                double dy = centerY - start.getY();
                double dz = centerZ - start.getZ();
                if ((dx * dx + dy * dy + dz * dz) > pruneDistSq) {
                    continue;
                }

                float width = (display.getModel() != null) ? display.getModel().hitboxWidth() : 0.8f;
                float height = (display.getModel() != null) ? display.getModel().hitboxHeight() : 0.8f;

                double minX = pos.x() + 0.5 - (width / 2.0);
                double maxX = pos.x() + 0.5 + (width / 2.0);
                double minY = pos.y();
                double maxY = pos.y() + height;
                double minZ = pos.z() + 0.5 - (width / 2.0);
                double maxZ = pos.z() + 0.5 + (width / 2.0);

                BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
                RayTraceResult hit = box.rayTrace(start, dir, maxDistance);
                if (hit != null && hit.getHitPosition() != null) {
                    double dist = start.distance(hit.getHitPosition());
                    if (dist <= maxDistance && dist < bestVirtualDist && dist < solidBlockDistance) {
                        Optional<CropInstance> cropOpt = cropManager.getCropAt(pos);
                        if (cropOpt.isPresent()) {
                            bestVirtualDist = dist;
                            bestVirtualPos = pos;
                            bestVirtualCrop = cropOpt.get();
                        }
                    }
                }
            }
        }

        if (bestVirtualCrop != null) {
            return Optional.of(new TargetResult(bestVirtualPos, bestVirtualCrop, TargetResult.TargetType.VIRTUAL_ENTITY));
        }

        // 3. Raytrace physical blocks (with ignorePassableBlocks = false to catch passable crop blocks like tripwire)
        RayTraceResult anyBlockHit = null;
        try {
            anyBlockHit = world.rayTraceBlocks(startLoc, dir, maxDistance, FluidCollisionMode.NEVER, false);
        } catch (Throwable t) {
            logger.trace(LogChannel.CORE,
                    () -> "Supporting soil raytrace failed in world '" + world.getName() + "'", t);
        }

        if (anyBlockHit != null && anyBlockHit.getHitBlock() != null) {
            Block hitBlock = anyBlockHit.getHitBlock();
            BlockPosition hitPos = BlockPosition.of(world.getName(), hitBlock.getX(), hitBlock.getY(), hitBlock.getZ());
            Optional<CropInstance> directCrop = cropManager.getCropAt(hitPos);
            if (directCrop.isPresent()) {
                return Optional.of(new TargetResult(hitPos, directCrop.get(), TargetResult.TargetType.PHYSICAL_BLOCK));
            }

            // 4. Farmland fallback: if hit block is farmland, check y + 1
            if (hitBlock.getType() == Material.FARMLAND) {
                BlockPosition abovePos = BlockPosition.of(world.getName(), hitBlock.getX(), hitBlock.getY() + 1, hitBlock.getZ());
                Optional<CropInstance> aboveCrop = cropManager.getCropAt(abovePos);
                if (aboveCrop.isPresent()) {
                    return Optional.of(new TargetResult(abovePos, aboveCrop.get(), TargetResult.TargetType.SUPPORTING_SOIL));
                }
            }
        }

        // Also evaluate solidBlockHit for farmland fallback if anyBlockHit was not farmland
        if (solidBlockHit != null && solidBlockHit.getHitBlock() != null && solidBlockHit.getHitBlock().getType() == Material.FARMLAND) {
            Block hitBlock = solidBlockHit.getHitBlock();
            BlockPosition abovePos = BlockPosition.of(world.getName(), hitBlock.getX(), hitBlock.getY() + 1, hitBlock.getZ());
            Optional<CropInstance> aboveCrop = cropManager.getCropAt(abovePos);
            if (aboveCrop.isPresent()) {
                return Optional.of(new TargetResult(abovePos, aboveCrop.get(), TargetResult.TargetType.SUPPORTING_SOIL));
            }
        }

        return Optional.empty();
    }

    /**
     * Data record describing a resolved crop target and its detection source.
     *
     * @param cropPosition canonical or occupancy coordinates of the crop
     * @param crop         the resolved crop instance
     * @param targetType   how the target was detected
     */
    public record TargetResult(
            BlockPosition cropPosition,
            CropInstance crop,
            TargetType targetType
    ) {
        public enum TargetType {
            VIRTUAL_ENTITY,
            PHYSICAL_BLOCK,
            SUPPORTING_SOIL
        }
    }
}
