package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.YieldEntry;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.platform.paper.event.CropBreakEvent;
import dev.darkblade.croppy.platform.paper.event.CropHarvestEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles crop breakage and harvesting by players.
 */
public class CropBreakListener implements Listener {

    private final CroppyCore core;

    public CropBreakListener(CroppyCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        Optional<CropInstance> cropOpt = core.getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            return;
        }

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = core.getSpeciesRegistry().get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) {
            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(storage -> storage.deleteCrop(pos));
            return;
        }

        Species species = speciesOpt.get();
        Player player = event.getPlayer();
        boolean isMature = species.isMature(crop.getStageIndex());

        if (isMature) {
            List<ItemStack> drops = new ArrayList<>();
            for (YieldEntry yield : species.getHarvest().yields()) {
                Material mat = Material.matchMaterial(yield.itemId());
                if (mat != null) {
                    int count = yield.min();
                    if (yield.max() > yield.min()) {
                        count += ThreadLocalRandom.current().nextInt(yield.max() - yield.min() + 1);
                    }
                    if (count > 0) {
                        drops.add(new ItemStack(mat, count));
                    }
                }
            }

            CropHarvestEvent harvestEvent = new CropHarvestEvent(player, species, block, crop, drops);
            Bukkit.getPluginManager().callEvent(harvestEvent);

            if (harvestEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(storage -> storage.deleteCrop(pos));
            event.setDropItems(false);

            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        } else {
            CropBreakEvent breakEvent = new CropBreakEvent(player, species, block, crop);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (breakEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(storage -> storage.deleteCrop(pos));
            event.setDropItems(false);

            Material seedMat = Material.matchMaterial(species.getDisplay().seedItemId());
            if (seedMat != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(seedMat, 1));
            }
        }
    }
}
