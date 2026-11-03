package dev.darkblade.croppy.platform.paper.display;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.YieldEntry;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.platform.paper.event.CropBreakEvent;
import dev.darkblade.croppy.platform.paper.event.CropHarvestEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * PacketEvents inbound packet listener intercepting player clicks on virtual Interaction entities.
 */
public class VirtualInteractionPacketListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final CroppyCore core;
    private final VirtualBlockDisplayTracker displayTracker;
    private final Logger logger;

    public VirtualInteractionPacketListener(Plugin plugin, CroppyCore core, VirtualBlockDisplayTracker displayTracker, Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.core = Objects.requireNonNull(core, "core cannot be null");
        this.displayTracker = Objects.requireNonNull(displayTracker, "displayTracker cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();

        Optional<VirtualBlockDisplay> displayOpt = displayTracker.getDisplayByInteractionId(entityId);
        if (displayOpt.isEmpty()) {
            return;
        }

        // Cancel vanilla entity interaction packet processing
        event.setCancelled(true);

        Player player = (Player) event.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        WrapperPlayClientInteractEntity.InteractAction action = packet.getAction();
        InteractionHand hand = packet.getHand();

        // Avoid double-firing on off-hand for right-click interactions
        if (action != WrapperPlayClientInteractEntity.InteractAction.ATTACK && hand == InteractionHand.OFF_HAND) {
            return;
        }

        BlockPosition pos = displayOpt.get().getPosition();

        // Switch to Bukkit main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttack(player, pos);
            } else {
                handleInteract(player, pos);
            }
        });
    }

    private void handleAttack(Player player, BlockPosition pos) {
        Optional<CropInstance> cropOpt = core.getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            displayTracker.removeDisplay(pos);
            return;
        }

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = core.getSpeciesRegistry().get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) {
            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(s -> s.deleteCrop(pos));
            displayTracker.removeDisplay(pos);
            return;
        }

        Species species = speciesOpt.get();
        boolean isMature = species.isMature(crop.getStageIndex());

        World world = player.getWorld();
        Location dropLoc = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());

        if (isMature) {
            List<ItemStack> drops = new ArrayList<>();
            for (YieldEntry yield : species.getHarvest().yields()) {
                Material mat = Material.matchMaterial(yield.itemId());
                if (mat != null) {
                    int count = yield.min() == yield.max() ? yield.min() : ThreadLocalRandom.current().nextInt(yield.min(), yield.max() + 1);
                    drops.add(new ItemStack(mat, count));
                }
            }

            CropHarvestEvent harvestEvent = new CropHarvestEvent(player, species, block, crop, drops);
            Bukkit.getPluginManager().callEvent(harvestEvent);

            if (!harvestEvent.isCancelled()) {
                core.getCropManager().removeCrop(pos);
                core.getStorage().ifPresent(s -> s.deleteCrop(pos));
                displayTracker.removeDisplay(pos);

                for (ItemStack drop : drops) {
                    world.dropItemNaturally(dropLoc, drop);
                }
            }
        } else {
            List<ItemStack> drops = new ArrayList<>();
            if (player.getGameMode() != GameMode.CREATIVE) {
                Material seedMat = Material.matchMaterial(species.getDisplay().seedItemId());
                if (seedMat != null) {
                    drops.add(new ItemStack(seedMat, 1));
                }
            }

            CropBreakEvent breakEvent = new CropBreakEvent(player, species, block, crop);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (!breakEvent.isCancelled()) {
                core.getCropManager().removeCrop(pos);
                core.getStorage().ifPresent(s -> s.deleteCrop(pos));
                displayTracker.removeDisplay(pos);

                for (ItemStack drop : drops) {
                    world.dropItemNaturally(dropLoc, drop);
                }
            }
        }
    }

    private void handleInteract(Player player, BlockPosition pos) {
        Optional<CropInstance> cropOpt = core.getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) return;

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = core.getSpeciesRegistry().get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) return;

        Species species = speciesOpt.get();
        if (species.isMature(crop.getStageIndex())) {
            // Right-click harvest on mature crops
            handleAttack(player, pos);
        }
    }
}
