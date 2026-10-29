package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.platform.paper.event.CropPlantEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

/**
 * Listens for block placement to detect when registered crop seeds are planted.
 */
public class CropPlantListener implements Listener {

    private final CroppyCore core;

    public CropPlantListener(CroppyCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        String seedId = event.getItemInHand().getType().getKey().toString().toLowerCase();
        Optional<Species> speciesOpt = core.getSpeciesRegistry().findBySeedItem(seedId);

        if (speciesOpt.isEmpty()) {
            String blockId = event.getBlockPlaced().getType().getKey().toString().toLowerCase();
            speciesOpt = core.getSpeciesRegistry().findByBlock(blockId);
        }

        if (speciesOpt.isEmpty()) {
            return;
        }

        Species species = speciesOpt.get();
        Block block = event.getBlockPlaced();
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        CropInstance instance = new DefaultCropInstance(species.getKey(), pos);

        CropPlantEvent plantEvent = new CropPlantEvent(event.getPlayer(), species, block, instance);
        Bukkit.getPluginManager().callEvent(plantEvent);

        if (plantEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        core.getCropManager().registerCrop(instance);
        core.getStorage().ifPresent(storage -> storage.saveCrop(instance));
    }
}
