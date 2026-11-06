package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanTier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

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
    private final dev.darkblade.croppy.api.protection.ProtectionPort protectionPort;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public WateringListener(WateringCanService wateringCanService, CropManager cropManager, PaperMessageService messageService, dev.darkblade.croppy.api.protection.ProtectionPort protectionPort) {
        this.wateringCanService = Objects.requireNonNull(wateringCanService, "wateringCanService cannot be null");
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.messageService = messageService;
        this.protectionPort = protectionPort;
    }

    public WateringListener(WateringCanService wateringCanService, CropManager cropManager, PaperMessageService messageService) {
        this(wateringCanService, cropManager, messageService, null);
    }

    public WateringListener(WateringCanService wateringCanService, CropManager cropManager) {
        this(wateringCanService, cropManager, null, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!wateringCanService.isWateringCan(item)) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        // 1. Refill detection
        Block waterSourceBlock = findTargetWaterSource(player, event);
        if (waterSourceBlock != null) {
            event.setCancelled(true);
            boolean refilled = wateringCanService.refill(item);
            if (refilled) {
                world.playSound(waterSourceBlock.getLocation(), "item.bucket.fill", 0.8f, 1.0f);
                world.spawnParticle(Particle.SPLASH, waterSourceBlock.getLocation().add(0.5, 0.8, 0.5), 15, 0.2, 0.2, 0.2, 0.1);
                if (messageService != null) {
                    messageService.sendActionBar(player, MessageKey.of("watering.refilled"));
                } else {
                    player.sendActionBar(miniMessage.deserialize("<aqua>Watering can refilled!</aqua>"));
                }
            }
            return;
        }

        // If not refilling, RIGHT_CLICK_AIR has no block to hydrate
        if (action == Action.RIGHT_CLICK_AIR) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
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
            player.playSound(player.getLocation(), "block.dispenser.fail", 0.8f, 1.5f);
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
            if (protectionPort != null && !protectionPort.canInteract(player.getUniqueId(), pos, dev.darkblade.croppy.api.protection.ProtectionAction.WATER)) {
                continue;
            }

            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            Block blockBelow = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());
            Block blockAbove = world.getBlockAt(pos.x(), pos.y() + 1, pos.z());

            // Check for crop at current block or above block
            Optional<CropInstance> cropOpt = cropManager.getCropAt(pos);
            if (cropOpt.isEmpty()) {
                cropOpt = cropManager.getCropAt(BlockPosition.of(world.getName(), pos.x(), pos.y() + 1, pos.z()));
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
        world.playSound(clickedBlock.getLocation(), "item.bucket.empty", 0.7f, 1.2f);
    }

    Block findTargetWaterSource(Player player, PlayerInteractEvent event) {
        // Step A: Server-side fluid raytrace from player eye location
        RayTraceResult rayTrace = player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS);
        if (rayTrace != null && rayTrace.getHitBlock() != null) {
            Block hitBlock = rayTrace.getHitBlock();
            if (isWaterSource(hitBlock)) {
                return hitBlock;
            }
        }

        // Step B: Direct clicked block check
        Block clicked = event.getClickedBlock();
        if (clicked != null && isWaterSource(clicked)) {
            return clicked;
        }

        // Step C: Fallback to adjacent block facing clicked block (e.g. shallow 1-block hole where client hit dirt beneath)
        if (clicked != null && event.getBlockFace() != null) {
            Block adjacent = clicked.getRelative(event.getBlockFace());
            if (isWaterSource(adjacent)) {
                return adjacent;
            }
        }

        return null;
    }

    boolean isWaterSource(Block block) {
        if (block == null) {
            return false;
        }
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
