package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanTier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles player right-click interactions for watering crops and refilling watering cans.
 */
public class WateringListener implements Listener {

    private final WateringCanService wateringCanService;
    private final CropManager cropManager;
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public WateringListener(WateringCanService wateringCanService, CropManager cropManager, PaperMessageService messageService) {
        this.wateringCanService = Objects.requireNonNull(wateringCanService, "wateringCanService cannot be null");
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.messageService = messageService;
    }

    public WateringListener(WateringCanService wateringCanService, CropManager cropManager) {
        this(wateringCanService, cropManager, null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!wateringCanService.isWateringCan(item)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Player player = event.getPlayer();
        World world = clickedBlock.getWorld();

        // 1. Refill detection
        if (isWaterSource(clickedBlock)) {
            event.setCancelled(true);
            boolean refilled = wateringCanService.refill(item);
            if (refilled) {
                world.playSound(clickedBlock.getLocation(), Sound.ITEM_BUCKET_FILL, 0.8f, 1.0f);
                world.spawnParticle(Particle.SPLASH, clickedBlock.getLocation().add(0.5, 0.8, 0.5), 15, 0.2, 0.2, 0.2, 0.1);
                if (messageService != null) {
                    messageService.sendActionBar(player, MessageKey.of("watering.refilled"));
                } else {
                    player.sendActionBar(miniMessage.deserialize("<aqua>Watering can refilled!</aqua>"));
                }
            }
            return;
        }

        // 2. Watering action
        Optional<WateringCanTier> tierOpt = wateringCanService.getTier(item);
        if (tierOpt.isEmpty()) {
            return;
        }
        WateringCanTier tier = tierOpt.get();

        int waterLevel = wateringCanService.getWaterLevel(item);
        if (waterLevel <= 0) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.5f);
            if (messageService != null) {
                messageService.sendActionBar(player, MessageKey.of("watering.empty"));
            } else {
                player.sendActionBar(miniMessage.deserialize("<red>The watering can is empty! Refill it with water.</red>"));
            }
            return;
        }

        event.setCancelled(true);

        BlockPosition center = BlockPosition.of(world.getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
        List<BlockPosition> aoe = wateringCanService.calculateAoe(center, player.getFacing(), tier);
        long currentDay = world.getFullTime() / 24000L;

        boolean wateredAny = false;
        for (BlockPosition pos : aoe) {
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            Block blockBelow = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());
            Block blockAbove = world.getBlockAt(pos.x(), pos.y() + 1, pos.z());

            // Check for crop at current block or above block
            Optional<CropInstance> cropOpt = cropManager.getCrop(pos);
            if (cropOpt.isEmpty()) {
                cropOpt = cropManager.getCrop(BlockPosition.of(world.getName(), pos.x(), pos.y() + 1, pos.z()));
            }

            if (cropOpt.isPresent()) {
                CropInstance crop = cropOpt.get();
                crop.setLastWateredDay(currentDay);
                wateredAny = true;
            }

            // Hydrate farmland visuals
            if (block.getType() == Material.FARMLAND && block.getBlockData() instanceof Farmland farmland) {
                farmland.setMoisture(7);
                block.setBlockData(farmland, false);
                wateredAny = true;
            } else if (blockBelow.getType() == Material.FARMLAND && blockBelow.getBlockData() instanceof Farmland farmland) {
                farmland.setMoisture(7);
                blockBelow.setBlockData(farmland, false);
                wateredAny = true;
            }

            world.spawnParticle(Particle.SPLASH, block.getLocation().add(0.5, 0.6, 0.5), 8, 0.25, 0.2, 0.25, 0.1);
        }

        wateringCanService.consumeWater(item, 1);
        world.playSound(clickedBlock.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.7f, 1.2f);
    }

    private boolean isWaterSource(Block block) {
        if (block.getType() == Material.WATER) {
            BlockData data = block.getBlockData();
            if (data instanceof Levelled levelled) {
                return levelled.getLevel() == 0;
            }
            return true;
        }
        if (block.getType() == Material.WATER_CAULDRON) {
            return true;
        }
        if (block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
            return true;
        }
        return false;
    }
}
