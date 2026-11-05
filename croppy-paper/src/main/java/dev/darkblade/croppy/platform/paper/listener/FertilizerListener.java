package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

/**
 * Handles player right-click application of fertilizers to active crops or tilled farmland.
 */
public class FertilizerListener implements Listener {

    private final FertilizerService fertilizerService;
    private final CropManager cropManager;
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public FertilizerListener(FertilizerService fertilizerService, CropManager cropManager, PaperMessageService messageService) {
        this.fertilizerService = Objects.requireNonNull(fertilizerService, "fertilizerService cannot be null");
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.messageService = messageService;
    }

    public FertilizerListener(FertilizerService fertilizerService, CropManager cropManager) {
        this(fertilizerService, cropManager, null);
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
        Optional<Fertilizer> fertOpt = fertilizerService.identifyFertilizer(item);
        if (fertOpt.isEmpty()) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Fertilizer fertilizer = fertOpt.get();
        Player player = event.getPlayer();
        World world = clickedBlock.getWorld();
        BlockPosition clickedPos = BlockPosition.of(world.getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
        BlockPosition abovePos = BlockPosition.of(world.getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ());

        // 1. Check for active crop at clicked block or block above
        Optional<CropInstance> cropOpt = cropManager.getCropAt(clickedPos);
        if (cropOpt.isEmpty()) {
            cropOpt = cropManager.getCropAt(abovePos);
        }

        if (cropOpt.isPresent()) {
            CropInstance crop = cropOpt.get();
            if (crop.hasFertilizer()) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), "block.dispenser.fail", 0.8f, 1.5f);
                if (messageService != null) {
                    messageService.sendActionBar(player, MessageKey.of("fertilizer.already-applied"));
                } else {
                    player.sendActionBar(miniMessage.deserialize("<red>This crop already has a fertilizer applied!</red>"));
                }
                return;
            }

            event.setCancelled(true);
            crop.setFertilizerKey(fertilizer.getKey());
            item.subtract(1);

            world.playSound(clickedBlock.getLocation(), "item.bone_meal.use", 0.8f, 1.1f);
            world.spawnParticle(Particle.HAPPY_VILLAGER, clickedBlock.getLocation().add(0.5, 0.5, 0.5), 12, 0.2, 0.2, 0.2, 0.1);
            if (messageService != null) {
                messageService.sendActionBar(player, MessageKey.of("fertilizer.applied"), PaperMessageService.arg("name", fertilizer.name()));
            } else {
                player.sendActionBar(miniMessage.deserialize("<green>Fertilizer applied: " + fertilizer.name() + "!</green>"));
            }
            return;
        }

        // 2. If no crop, check if clicking empty farmland to pre-fertilize soil
        if (clickedBlock.getType() == Material.FARMLAND) {
            event.setCancelled(true);
            fertilizerService.bufferSoilFertilizer(abovePos, fertilizer.getKey());
            item.subtract(1);

            world.playSound(clickedBlock.getLocation(), "item.bone_meal.use", 0.8f, 1.0f);
            world.spawnParticle(Particle.COMPOSTER, clickedBlock.getLocation().add(0.5, 1.0, 0.5), 10, 0.2, 0.1, 0.2, 0.1);
            if (messageService != null) {
                messageService.sendActionBar(player, MessageKey.of("fertilizer.soil-enriched"));
            } else {
                player.sendActionBar(miniMessage.deserialize("<green>Soil enriched! Plant a seed within 30s.</green>"));
            }
        }
    }
}
