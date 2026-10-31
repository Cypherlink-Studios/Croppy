package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.gui.StationGuiListener;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles physical and GUI interactions when players right-click crafting station blocks.
 */
public final class StationInteractListener implements Listener {

    private final DefaultStationManager stationManager;
    private final ProcessEngine processEngine;
    private final ItemFactory itemFactory;
    private final StationGuiListener guiListener;

    public StationInteractListener(DefaultStationManager stationManager, ProcessEngine processEngine, ItemFactory itemFactory, StationGuiListener guiListener) {
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.guiListener = Objects.requireNonNull(guiListener, "guiListener cannot be null");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        BlockPosition pos = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        Optional<StationInstance> stationOpt = stationManager.getStation(pos);
        if (stationOpt.isEmpty()) {
            return;
        }

        StationInstance station = stationOpt.get();
        StationDefinition def = station.getDefinition();
        Player player = event.getPlayer();

        // Check if interaction style directs to virtual GUI
        boolean openGui = (def.interactionType() == StationInteractionType.GUI)
                || (def.interactionType() == StationInteractionType.HYBRID && !player.isSneaking());

        if (openGui) {
            event.setCancelled(true);
            guiListener.openStationGui(player, station);
            return;
        }

        // Physical In-World Interaction
        event.setCancelled(true);
        handlePhysicalInteraction(player, station);
    }

    private void handlePhysicalInteraction(Player player, StationInstance station) {
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        StorageComponent storage = storageOpt.get();

        // 1. Extract outputs if ready
        List<StoredItem> outputs = storage.extractOutputs();
        if (!outputs.isEmpty()) {
            for (StoredItem out : outputs) {
                ItemStack item = createBukkitItem(out);
                player.getInventory().addItem(item);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            return;
        }

        // 2. Handle manual interaction if awaiting input
        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        if (processOpt.isPresent() && processOpt.get().hasActiveProcess()) {
            ProcessInstance pi = processOpt.get().getActiveProcess().get();
            if (pi.getState() == ProcessState.AWAITING_INPUT) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                Key itemKey = resolveItemKey(handItem);
                int amount = handItem.getAmount();

                boolean advanced = processEngine.handleManualInteraction(station, itemKey, amount);
                if (advanced) {
                    handItem.subtract(1);
                    player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
                    return;
                }
            }
        }

        // 3. Insert input item if idle
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && !handItem.getType().isAir()) {
            Key itemKey = resolveItemKey(handItem);
            boolean added = storage.addInput(StoredItem.of(itemKey, 1));
            if (added) {
                handItem.subtract(1);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);

                EvaluationContext context = EvaluationContext.empty().with("player", player);
                processEngine.tryStartProcess(station, context);
            }
        }
    }

    private Key resolveItemKey(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return itemFactory.getItemId(item)
                .orElseGet(() -> Key.of("minecraft", item.getType().name().toLowerCase(Locale.ROOT)));
    }

    private ItemStack createBukkitItem(StoredItem stored) {
        return itemFactory.createItemStack(new dev.darkblade.croppy.api.item.ItemDefinition(
                stored.itemKey(),
                "misc",
                stored.itemKey().asString(),
                0,
                "<gray>" + stored.itemKey().value() + "</gray>",
                List.of(),
                java.util.Map.of()
        ), stored.amount());
    }
}
