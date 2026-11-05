package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Ensures crop instances are cleaned up when destroyed by environmental physics,
 * water flows, pistons, or farmland trampling.
 */
public class CropPhysicsListener implements Listener {

    private final CroppyCore core;
    private final VirtualBlockDisplayTracker displayTracker;

    public CropPhysicsListener(CroppyCore core, VirtualBlockDisplayTracker displayTracker) {
        this.core = Objects.requireNonNull(core, "core cannot be null");
        this.displayTracker = displayTracker;
    }

    public CropPhysicsListener(CroppyCore core) {
        this(core, null);
    }

    private void removeIfTracked(Block block) {
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        core.getCropManager().getCropAt(pos).ifPresent(crop -> {
            core.getCropManager().removeCrop(crop.getPosition());
            core.getStorage().ifPresent(storage -> storage.deleteCrop(crop.getPosition()));
            if (displayTracker != null) {
                displayTracker.removeDisplay(crop.getPosition());
            }
            Block rootBlock = block.getWorld().getBlockAt(crop.getPosition().x(), crop.getPosition().y(), crop.getPosition().z());
            if (!isAir(rootBlock.getType())) {
                rootBlock.setType(Material.AIR);
            }
        });
    }

    private boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private boolean isSolidSoil(Material mat) {
        if (mat == null) return false;
        if (mat == Material.FARMLAND) return true;
        try {
            return mat.isSolid();
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWaterFlow(BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        removeIfTracked(toBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            removeIfTracked(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            removeIfTracked(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.FARMLAND) {
                Block cropBlock = event.getClickedBlock().getRelative(BlockFace.UP);
                removeIfTracked(cropBlock);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTrample(EntityInteractEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND) {
            Block cropBlock = event.getBlock().getRelative(BlockFace.UP);
            removeIfTracked(cropBlock);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        core.getCropManager().getCropAt(pos).ifPresent(crop -> {
            Block soil = block.getWorld().getBlockAt(crop.getPosition().x(), crop.getPosition().y() - 1, crop.getPosition().z());
            Material soilType = soil.getType();
            if (soilType != Material.FARMLAND && !isSolidSoil(soilType)) {
                removeIfTracked(block);
            }
        });
    }
}
