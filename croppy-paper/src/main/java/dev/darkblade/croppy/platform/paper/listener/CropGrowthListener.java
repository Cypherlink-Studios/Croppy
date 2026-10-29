package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

/**
 * Suppresses vanilla Minecraft random growth ticks for blocks tracked by Croppy.
 */
public class CropGrowthListener implements Listener {

    private final CroppyCore core;

    public CropGrowthListener(CroppyCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        if (core.getCropManager().hasCrop(pos)) {
            event.setCancelled(true);
        }
    }
}
