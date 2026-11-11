package dev.darkblade.croppy.platform.paper.display;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionAction;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.YieldEntry;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.platform.paper.event.CropBreakEvent;
import dev.darkblade.croppy.platform.paper.event.CropHarvestEvent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * PacketEvents inbound packet listener intercepting player clicks on virtual Interaction entities,
 * routing through the declarative InteractionRouter or defaulting to harvest/break behaviors.
 */
public class VirtualInteractionPacketListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final CroppyCore core;
    private final VirtualBlockDisplayTracker displayTracker;
    private final Logger logger;
    private final DisplaySynchronizer displaySynchronizer;
    private final dev.darkblade.croppy.api.protection.ProtectionPort protectionPort;
    private final ItemFactory itemFactory;
    private final CropInspectorService inspectorService;

    public VirtualInteractionPacketListener(
            Plugin plugin,
            CroppyCore core,
            VirtualBlockDisplayTracker displayTracker,
            Logger logger,
            DisplaySynchronizer displaySynchronizer,
            dev.darkblade.croppy.api.protection.ProtectionPort protectionPort,
            ItemFactory itemFactory,
            CropInspectorService inspectorService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.core = Objects.requireNonNull(core, "core cannot be null");
        this.displayTracker = Objects.requireNonNull(displayTracker, "displayTracker cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.displaySynchronizer = displaySynchronizer;
        this.protectionPort = protectionPort;
        this.itemFactory = (itemFactory != null) ? itemFactory : new ItemFactory(plugin, core.getItemRegistry());
        this.inspectorService = inspectorService;
    }

    public VirtualInteractionPacketListener(
            Plugin plugin,
            CroppyCore core,
            VirtualBlockDisplayTracker displayTracker,
            Logger logger,
            DisplaySynchronizer displaySynchronizer,
            dev.darkblade.croppy.api.protection.ProtectionPort protectionPort,
            ItemFactory itemFactory
    ) {
        this(plugin, core, displayTracker, logger, displaySynchronizer, protectionPort, itemFactory, null);
    }

    public VirtualInteractionPacketListener(
            Plugin plugin,
            CroppyCore core,
            VirtualBlockDisplayTracker displayTracker,
            Logger logger,
            DisplaySynchronizer displaySynchronizer,
            dev.darkblade.croppy.api.protection.ProtectionPort protectionPort
    ) {
        this(plugin, core, displayTracker, logger, displaySynchronizer, protectionPort, null, null);
    }

    public VirtualInteractionPacketListener(
            Plugin plugin,
            CroppyCore core,
            VirtualBlockDisplayTracker displayTracker,
            Logger logger,
            DisplaySynchronizer displaySynchronizer
    ) {
        this(plugin, core, displayTracker, logger, displaySynchronizer, null, null, null);
    }

    public VirtualInteractionPacketListener(Plugin plugin, CroppyCore core, VirtualBlockDisplayTracker displayTracker, Logger logger) {
        this(plugin, core, displayTracker, logger, null, null, null, null);
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

            handlePlayerInteraction(player, pos, action);
        });
    }

    private void handlePlayerInteraction(Player player, BlockPosition pos, WrapperPlayClientInteractEntity.InteractAction action) {
        Optional<CropInstance> cropOpt = core.getCropManager().getCropAt(pos);
        if (cropOpt.isEmpty()) {
            displayTracker.removeDisplay(pos);
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Read-only inspection tool check runs before harvest/break region protection
        if (action != WrapperPlayClientInteractEntity.InteractAction.ATTACK && CropInspectorService.isInspectionTool(itemInHand, plugin)) {
            CropInspectorService inspector = resolveInspectorService();
            if (inspector != null) {
                Optional<net.kyori.adventure.text.Component> card = inspector.inspect(pos, player);
                if (card.isPresent()) {
                    player.sendMessage(card.get());
                } else if (plugin instanceof dev.darkblade.croppy.platform.paper.CroppyPlugin cp && cp.getMessageService() != null) {
                    cp.getMessageService().sendPrefixedMessage(player, dev.darkblade.croppy.api.i18n.MessageKey.of("command.inspect.no-crop-found"));
                }
            }
            return;
        }

        dev.darkblade.croppy.api.protection.ProtectionAction protAction =
                (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK)
                        ? dev.darkblade.croppy.api.protection.ProtectionAction.BREAK
                        : dev.darkblade.croppy.api.protection.ProtectionAction.HARVEST;

        if (protectionPort != null && !protectionPort.canInteract(player.getUniqueId(), pos, protAction)) {
            return;
        }

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = core.getSpeciesRegistry().get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) {
            core.getCropManager().removeCrop(pos);
            core.getStorage().ifPresent(s -> s.deleteCrop(crop.getPosition()));
            displayTracker.removeDisplay(crop.getPosition());
            return;
        }

        Species species = speciesOpt.get();
        boolean isMature = species.isMature(crop.getStageIndex());

        // Determine interaction trigger
        InteractionTrigger trigger;
        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            trigger = player.isSneaking() ? InteractionTrigger.SNEAK_LEFT_CLICK : InteractionTrigger.LEFT_CLICK;
        } else {
            trigger = player.isSneaking() ? InteractionTrigger.SNEAK_RIGHT_CLICK : InteractionTrigger.RIGHT_CLICK;
        }

        boolean emptyHand = itemInHand.getType().isAir();
        String handMaterial = emptyHand ? null : itemInHand.getType().getKey().toString();
        Key handItemKey = null;
        if (itemInHand.hasItemMeta() && itemInHand.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("croppy", "item_id"), PersistentDataType.STRING)) {
            String id = itemInHand.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("croppy", "item_id"), PersistentDataType.STRING);
            if (id != null) {
                try {
                    handItemKey = Key.parse(id);
                } catch (Exception ignored) {}
            }
        }

        String currentStageId = (crop.getStageIndex() < species.getStages().size())
                ? species.getStage(crop.getStageIndex()).id()
                : "unknown";

        EvaluationContext evalCtx = EvaluationContext.empty()
                .with("crop.stage_id", currentStageId)
                .with("crop.is_mature", isMature)
                .with("crop.height", species.getHeight())
                .with("player.is_sneaking", player.isSneaking());

        // Query declarative InteractionRouter
        Optional<InteractionDefinition> matchOpt = core.getInteractionRouter().matchCrop(
                species.getKey(),
                trigger,
                handItemKey,
                handMaterial,
                emptyHand,
                evalCtx
        );

        if (matchOpt.isPresent()) {
            executeInteractionPipeline(player, crop, species, matchOpt.get(), itemInHand);
            return;
        }

        // Fallback default handling
        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            handleDefaultAttack(player, crop, species, pos);
        } else {
            if (isMature) {
                performHarvest(player, crop, species, 1.0);
            }
        }
    }

    private void executeInteractionPipeline(
            Player player,
            CropInstance crop,
            Species species,
            InteractionDefinition def,
            ItemStack itemInHand
    ) {
        double yieldMultiplier = 1.0;
        World world = player.getWorld();
        Location soundLoc = new Location(world, crop.getPosition().x() + 0.5, crop.getPosition().y() + 0.5, crop.getPosition().z() + 0.5);

        for (InteractionAction act : def.actions()) {
            switch (act.type().toUpperCase(Locale.ROOT)) {
                case "HARVEST_CROP" -> {
                    if (act.parameters().containsKey("bonus_yield_multiplier")) {
                        Object multObj = act.parameters().get("bonus_yield_multiplier");
                        if (multObj instanceof Number n) {
                            yieldMultiplier = n.doubleValue();
                        }
                    }
                    performHarvest(player, crop, species, yieldMultiplier);
                }
                case "DISMANTLE_CROP" -> {
                    performDismantle(player, crop, species);
                }
                case "DAMAGE_ITEM" -> {
                    if (itemInHand != null && !itemInHand.getType().isAir() && itemInHand.getItemMeta() instanceof Damageable dmg) {
                        int amount = 1;
                        if (act.parameters().get("amount") instanceof Number n) {
                            amount = n.intValue();
                        }
                        dmg.setDamage(dmg.getDamage() + amount);
                        itemInHand.setItemMeta(dmg);
                    }
                }
                case "DROP_ITEM" -> {
                    Object itemObj = act.parameters().get("item");
                    if (itemObj != null) {
                        int amt = 1;
                        if (act.parameters().get("amount") instanceof Number n) {
                            amt = n.intValue();
                        }
                        itemFactory.resolveItemStack(itemObj.toString(), amt)
                                .ifPresent(stack -> world.dropItemNaturally(soundLoc, stack));
                    }
                }
                case "PLAY_SOUND" -> {
                    Object sObj = act.parameters().get("sound");
                    if (sObj != null) {
                        try {
                            float vol = 1.0f;
                            float pitch = 1.0f;
                            if (act.parameters().get("volume") instanceof Number n) vol = n.floatValue();
                            if (act.parameters().get("pitch") instanceof Number n) pitch = n.floatValue();
                            world.playSound(soundLoc, sObj.toString().toLowerCase(Locale.ROOT), vol, pitch);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void performHarvest(Player player, CropInstance crop, Species species, double multiplier) {
        BlockPosition pos = crop.getPosition();
        World world = player.getWorld();
        Location dropLoc = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());

        List<ItemStack> drops = new ArrayList<>();
        for (YieldEntry yield : species.getHarvest().yields()) {
            int baseCount = yield.min() == yield.max() ? yield.min() : ThreadLocalRandom.current().nextInt(yield.min(), yield.max() + 1);
            int count = Math.max(1, (int) Math.round(baseCount * multiplier));
            itemFactory.resolveItemStack(yield.itemId(), count).ifPresent(drops::add);
        }

        CropHarvestEvent harvestEvent = new CropHarvestEvent(player, species, block, crop, drops);
        Bukkit.getPluginManager().callEvent(harvestEvent);

        if (harvestEvent.isCancelled()) {
            return;
        }

        // Cyclic regrowth support
        if (species.getHarvest().hasRegrowth()) {
            String targetStageId = species.getHarvest().regrowth().get().targetStageId();
            int targetIdx = species.getStageIndex(targetStageId);
            if (targetIdx >= 0) {
                crop.setStageIndex(targetIdx);
                crop.setProgress(0.0);
                crop.updateTimestamp();
                core.getStorage().ifPresent(s -> s.saveCrop(crop));

                if (displaySynchronizer != null && targetIdx < species.getStages().size()) {
                    displaySynchronizer.synchronizeDisplay(crop, species, species.getStages().get(targetIdx));
                }

                for (ItemStack drop : drops) {
                    world.dropItemNaturally(dropLoc, drop);
                }
                return;
            }
        }

        // Monocarpic removal
        core.getCropManager().removeCrop(pos);
        core.getStorage().ifPresent(s -> s.deleteCrop(pos));
        displayTracker.removeDisplay(pos);
        world.getBlockAt(pos.x(), pos.y(), pos.z()).setType(Material.AIR);

        for (ItemStack drop : drops) {
            world.dropItemNaturally(dropLoc, drop);
        }
    }

    private void performDismantle(Player player, CropInstance crop, Species species) {
        BlockPosition pos = crop.getPosition();
        World world = player.getWorld();
        Location dropLoc = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());

        CropBreakEvent breakEvent = new CropBreakEvent(player, species, block, crop);
        Bukkit.getPluginManager().callEvent(breakEvent);

        if (breakEvent.isCancelled()) {
            return;
        }

        core.getCropManager().removeCrop(pos);
        core.getStorage().ifPresent(s -> s.deleteCrop(pos));
        displayTracker.removeDisplay(pos);
        block.setType(Material.AIR);

        if (player.getGameMode() != GameMode.CREATIVE) {
            itemFactory.resolveItemStack(species.getDisplay().seedItemId(), 1)
                    .ifPresent(stack -> world.dropItemNaturally(dropLoc, stack));
        }
    }

    private void handleDefaultAttack(Player player, CropInstance crop, Species species, BlockPosition pos) {
        boolean isMature = species.isMature(crop.getStageIndex());
        if (isMature) {
            performHarvest(player, crop, species, 1.0);
        } else {
            performDismantle(player, crop, species);
        }
    }

    private CropInspectorService resolveInspectorService() {
        if (this.inspectorService != null) {
            return this.inspectorService;
        }
        if (plugin instanceof dev.darkblade.croppy.platform.paper.CroppyPlugin cp) {
            return cp.getInspectorService();
        }
        return null;
    }
}
