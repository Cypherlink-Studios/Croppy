package dev.darkblade.croppy.platform.paper.station.gui;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles inventory interactions for virtual station container GUIs.
 */
public final class StationGuiListener implements Listener {

    public static final int INPUT_SLOT = 11;
    public static final int STATUS_SLOT = 13;
    public static final int OUTPUT_SLOT = 15;

    private final ProcessEngine processEngine;
    private final ItemFactory itemFactory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public StationGuiListener(ProcessEngine processEngine, ItemFactory itemFactory) {
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
    }

    /**
     * Populates and opens the station GUI for a player.
     */
    public void openStationGui(Player player, StationInstance station) {
        StationGuiHolder holder = new StationGuiHolder(station);
        Inventory inv = holder.getInventory();

        // 1. Fill background panes
        ItemStack pane = createPane();
        for (int i = 0; i < 27; i++) {
            if (i != INPUT_SLOT && i != STATUS_SLOT && i != OUTPUT_SLOT) {
                inv.setItem(i, pane);
            }
        }

        // 2. Synchronize current station storage contents
        refreshGuiContents(holder);

        player.openInventory(inv);
    }

    public void refreshGuiContents(StationGuiHolder holder) {
        StationInstance station = holder.getStation();
        Inventory inv = holder.getInventory();

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isPresent()) {
            StorageComponent storage = storageOpt.get();

            // Render inputs in INPUT_SLOT
            List<StoredItem> inputs = storage.getInputs();
            if (!inputs.isEmpty()) {
                StoredItem in = inputs.get(0);
                inv.setItem(INPUT_SLOT, createBukkitItem(in));
            } else {
                inv.setItem(INPUT_SLOT, null);
            }

            // Render outputs in OUTPUT_SLOT
            List<StoredItem> outputs = storage.getOutputs();
            if (!outputs.isEmpty()) {
                StoredItem out = outputs.get(0);
                inv.setItem(OUTPUT_SLOT, createBukkitItem(out));
            } else {
                inv.setItem(OUTPUT_SLOT, null);
            }
        }

        // Status indicator in STATUS_SLOT
        inv.setItem(STATUS_SLOT, createStatusIndicator(station));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StationGuiHolder holder)) {
            return;
        }

        int rawSlot = event.getRawSlot();

        // Prevent interacting with decorative glass panes or status button
        if (rawSlot >= 0 && rawSlot < 27) {
            if (rawSlot != INPUT_SLOT && rawSlot != OUTPUT_SLOT) {
                event.setCancelled(true);
                return;
            }
        }

        // Run sync update on next tick
        if (event.getWhoClicked() instanceof Player player) {
            player.getServer().getScheduler().runTask(player.getServer().getPluginManager().getPlugin("Croppy"), () -> {
                syncStorageFromGui(holder);
                processEngine.tryStartProcess(holder.getStation(), EvaluationContext.empty().with("player", player));
                refreshGuiContents(holder);
            });
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof StationGuiHolder holder)) {
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < 27 && slot != INPUT_SLOT && slot != OUTPUT_SLOT) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getWhoClicked() instanceof Player player) {
            player.getServer().getScheduler().runTask(player.getServer().getPluginManager().getPlugin("Croppy"), () -> {
                syncStorageFromGui(holder);
                processEngine.tryStartProcess(holder.getStation(), EvaluationContext.empty().with("player", player));
                refreshGuiContents(holder);
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StationGuiHolder holder)) {
            return;
        }

        syncStorageFromGui(holder);
        if (event.getPlayer() instanceof Player player) {
            processEngine.tryStartProcess(holder.getStation(), EvaluationContext.empty().with("player", player));
        }
    }

    private void syncStorageFromGui(StationGuiHolder holder) {
        StationInstance station = holder.getStation();
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        StorageComponent storage = storageOpt.get();
        storage.clear();

        Inventory inv = holder.getInventory();
        ItemStack inItem = inv.getItem(INPUT_SLOT);
        if (inItem != null && !inItem.getType().isAir()) {
            Key key = resolveItemKey(inItem);
            storage.addInput(StoredItem.of(key, inItem.getAmount()));
        }

        ItemStack outItem = inv.getItem(OUTPUT_SLOT);
        if (outItem != null && !outItem.getType().isAir()) {
            Key key = resolveItemKey(outItem);
            storage.addOutput(StoredItem.of(key, outItem.getAmount()));
        }
    }

    private Key resolveItemKey(ItemStack item) {
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

    private ItemStack createStatusIndicator(StationInstance station) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
            if (processOpt.isPresent() && processOpt.get().hasActiveProcess()) {
                ProcessInstance pi = processOpt.get().getActiveProcess().get();
                meta.displayName(miniMessage.deserialize("<gold>Status: " + pi.getState().name() + "</gold>")
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                meta.displayName(miniMessage.deserialize("<gray>Status: Idle</gray>")
                        .decoration(TextDecoration.ITALIC, false));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.BLACK));
            pane.setItemMeta(meta);
        }
        return pane;
    }
}
