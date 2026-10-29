package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
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

/**
 * Ensures crop instances are cleaned up when destroyed by environmental physics,
 * water flows, pistons, or farmland trampling.
 */
public class CropPhysicsListener implements Listener {

    private final CroppyCore core;

    public CropPhysicsListener(CroppyCore core) {
        this.core = core;
    }

    private void removeIfTracked(Block block) {
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (core.getCropManager().hasCrop(pos)) {
            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(storage -> storage.deleteCrop(pos));
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
        if (core.getCropManager().hasCrop(pos)) {
            Block soil = block.getRelative(BlockFace.DOWN);
            if (soil.getType() != Material.FARMLAND && !soil.getType().isSolid()) {
                core.getCropManager().removeCrop(pos);
                core.getStorage().ifPresent(storage -> storage.deleteCrop(pos));
            }
        }
    }
}
