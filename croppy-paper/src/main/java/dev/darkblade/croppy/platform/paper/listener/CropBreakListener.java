package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles crop breakage and harvesting by players on physical blocks.
 */
public class CropBreakListener implements Listener {

    private final CroppyCore core;
    private final DisplaySynchronizer displaySynchronizer;

    public CropBreakListener(CroppyCore core, DisplaySynchronizer displaySynchronizer) {
        this.core = Objects.requireNonNull(core, "core cannot be null");
        this.displaySynchronizer = displaySynchronizer;
    }

    public CropBreakListener(CroppyCore core) {
        this(core, null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        Optional<CropInstance> cropOpt = core.getCropManager().getCropAt(pos);
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

            event.setDropItems(false);

            // Handle cyclic regrowth
            if (species.getHarvest().hasRegrowth()) {
                String targetStageId = species.getHarvest().regrowth().get().targetStageId();
                int targetIdx = species.getStageIndex(targetStageId);
                if (targetIdx >= 0) {
                    crop.setStageIndex(targetIdx);
                    crop.setProgress(0.0);
                    crop.updateTimestamp();
                    core.getStorage().ifPresent(storage -> storage.saveCrop(crop));

                    if (displaySynchronizer != null && targetIdx < species.getStages().size()) {
                        displaySynchronizer.synchronizeDisplay(crop, species, species.getStages().get(targetIdx));
                    }

                    for (ItemStack drop : drops) {
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                    event.setCancelled(true); // Retain the block in the world
                    return;
                }
            }

            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(storage -> storage.deleteCrop(pos));

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
