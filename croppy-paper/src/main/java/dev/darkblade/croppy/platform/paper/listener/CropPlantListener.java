package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;
import dev.darkblade.croppy.core.species.RotationCalculator;
import dev.darkblade.croppy.platform.paper.event.CropPlantEvent;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.growth.PaperDisplaySynchronizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Listens for block placement to detect when registered crop seeds are planted and protects
 * active 3D crop spaces from accidental block placement collisions.
 */
public class CropPlantListener implements Listener {

    private final CroppyCore core;
    private final FertilizerService fertilizerService;
    private final PaperDisplaySynchronizer displaySynchronizer;

    public CropPlantListener(CroppyCore core, FertilizerService fertilizerService, PaperDisplaySynchronizer displaySynchronizer) {
        this.core = core;
        this.fertilizerService = fertilizerService;
        this.displaySynchronizer = displaySynchronizer;
    }

    public CropPlantListener(CroppyCore core, FertilizerService fertilizerService) {
        this(core, fertilizerService, null);
    }

    public CropPlantListener(CroppyCore core) {
        this(core, null, null);
    }

    private boolean isAir(Material material) {
        return material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    /**
     * Safeguard: Prevents players from placing blocks inside coordinates already occupied by an active crop.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockCollisionSafeguard(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        BlockPosition pos = BlockPosition.of(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (core.getCropManager().getCropAt(pos).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        Optional<Species> speciesOpt = Optional.empty();

        if (itemInHand != null && itemInHand.hasItemMeta()) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null) {
                // 1. Check standardized croppy:item_id
                String pdcItemId = meta.getPersistentDataContainer().get(
                        new NamespacedKey("croppy", "item_id"),
                        PersistentDataType.STRING
                );
                if (pdcItemId != null) {
                    try {
                        speciesOpt = core.getSpeciesRegistry().findBySeedItem(pdcItemId.toLowerCase());
                    } catch (Exception ignored) {
                    }
                }

                // 2. Fallback to legacy croppy:species
                if (speciesOpt.isEmpty()) {
                    String pdcSpecies = meta.getPersistentDataContainer().get(
                            new NamespacedKey("croppy", "species"),
                            PersistentDataType.STRING
                    );
                    if (pdcSpecies != null) {
                        try {
                            speciesOpt = core.getSpeciesRegistry().get(dev.darkblade.croppy.api.identifier.Key.parse(pdcSpecies));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        if (speciesOpt.isEmpty() && itemInHand != null) {
            String seedId = itemInHand.getType().getKey().toString().toLowerCase();
            speciesOpt = core.getSpeciesRegistry().findBySeedItem(seedId);
        }

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

        // Validate full multi-block spatial clearance and occupancy
        java.util.List<BlockPosition> occupiedPositions = species.getSpatialFootprint().calculateOccupiedPositions(pos);
        for (BlockPosition occPos : occupiedPositions) {
            if (core.getCropManager().isOccupied(occPos)) {
                event.setCancelled(true);
                if (event.getPlayer() != null) {
                    event.getPlayer().sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<red>Not enough space to plant this crop!</red>"));
                }
                return;
            }

            if (!occPos.equals(pos)) {
                try {
                    Block upperBlock = block.getWorld().getBlockAt(occPos.x(), occPos.y(), occPos.z());
                    if (upperBlock != null && !isAir(upperBlock.getType()) && !upperBlock.isPassable()) {
                        event.setCancelled(true);
                        if (event.getPlayer() != null) {
                            event.getPlayer().sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                                    .deserialize("<red>Not enough vertical space for this crop!</red>"));
                        }
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        float playerYaw = event.getPlayer() != null ? event.getPlayer().getLocation().getYaw() : 0.0f;
        float effectiveYaw = RotationCalculator.calculate(playerYaw, species.getDisplay().rotationMode());

        CropInstance instance = new DefaultCropInstance(
                species.getKey(),
                pos,
                0,
                0.0,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                -1L,
                null,
                effectiveYaw
        );

        if (fertilizerService != null) {
            fertilizerService.consumeBufferedSoilFertilizer(pos).ifPresent(instance::setFertilizerKey);
        }

        CropPlantEvent plantEvent = new CropPlantEvent(event.getPlayer(), species, block, instance);
        Bukkit.getPluginManager().callEvent(plantEvent);

        if (plantEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        core.getCropManager().registerCrop(instance, species.getHeight());
        core.getStorage().ifPresent(storage -> storage.saveCrop(instance));

        if ("model_3d".equalsIgnoreCase(species.getDisplay().type())) {
            if (displaySynchronizer != null && !species.getStages().isEmpty()) {
                displaySynchronizer.synchronizeDisplay(instance, species, species.getStages().get(0));
            }
        }
    }
}
