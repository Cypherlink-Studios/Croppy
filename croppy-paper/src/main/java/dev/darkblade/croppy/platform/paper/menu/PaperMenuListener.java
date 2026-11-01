package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.ClickTypeFilter;
import dev.darkblade.croppy.api.menu.MenuAction;
import dev.darkblade.croppy.api.menu.MenuActionType;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.core.menu.MenuExpressionEvaluator;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.prompt.ChatPromptService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles Bukkit inventory events for {@link PaperMenuHolder} instances with strict anti-duplication security.
 */
public final class PaperMenuListener implements Listener {

    private final Plugin plugin;
    private final ProcessEngine processEngine;
    private final ItemFactory itemFactory;
    private final PaperMenuRenderer renderer;
    private final MiniMessage miniMessage;
    private ChatPromptService chatPromptService;
    private PaperMenuService menuService;
    private dev.darkblade.croppy.platform.paper.discovery.StationExperimentationHandler experimentationHandler;

    public PaperMenuListener(
            Plugin plugin,
            ProcessEngine processEngine,
            ItemFactory itemFactory,
            PaperMenuRenderer renderer,
            ChatPromptService chatPromptService,
            PaperMenuService menuService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.chatPromptService = chatPromptService;
        this.menuService = menuService;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public PaperMenuListener(Plugin plugin, ProcessEngine processEngine, ItemFactory itemFactory, PaperMenuRenderer renderer) {
        this(plugin, processEngine, itemFactory, renderer, null, null);
    }

    public void setChatPromptService(ChatPromptService chatPromptService) {
        this.chatPromptService = chatPromptService;
    }

    public void setMenuService(PaperMenuService menuService) {
        this.menuService = menuService;
    }

    public void setExperimentationHandler(dev.darkblade.croppy.platform.paper.discovery.StationExperimentationHandler experimentationHandler) {
        this.experimentationHandler = experimentationHandler;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PaperMenuHolder holder)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        MenuDefinition menu = holder.getMenu();
        int menuSize = menu.size();
        ClickType clickType = event.getClick();

        // 1. Double click in entire inventory window: cancel to prevent collecting from outputs or decorations
        if (clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        // 2. Interacting inside the top menu inventory
        if (rawSlot >= 0 && rawSlot < menuSize) {
            Optional<SlotDefinition> slotOpt = menu.getSlotAt(rawSlot);
            if (slotOpt.isEmpty()) {
                event.setCancelled(true);
                return;
            }

            SlotDefinition slotDef = slotOpt.get();
            SlotType type = slotDef.type();

            // Cancel any number key or offhand swap on non-input slots
            if (type != SlotType.INPUT && (clickType == ClickType.NUMBER_KEY || clickType == ClickType.SWAP_OFFHAND)) {
                event.setCancelled(true);
                return;
            }

            // A. Passive / Indicator / Button slots
            if (type == SlotType.DECORATION || type == SlotType.STATUS) {
                event.setCancelled(true);
                return;
            }

            if (type == SlotType.ACTION) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    handleActionClick(player, holder, slotDef, clickType);
                }
                return;
            }

            // B. Output slots (extraction / pickup only)
            if (type == SlotType.OUTPUT) {
                InventoryAction action = event.getAction();
                if (isPlacingAction(action)) {
                    event.setCancelled(true);
                    return;
                }
                // Allow pickup, schedule synchronization
                scheduleSync(holder, event.getWhoClicked() instanceof Player p ? p : null);
                return;
            }

            // C. Input slots (allow placing / taking, schedule synchronization)
            if (type == SlotType.INPUT) {
                scheduleSync(holder, event.getWhoClicked() instanceof Player p ? p : null);
                return;
            }
        }

        // 3. Interacting inside player inventory while menu is open
        if (rawSlot >= menuSize) {
            // Shift-click from player inventory: redirect strictly to INPUT slots
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack current = event.getCurrentItem();
                if (current != null && !current.getType().isAir()) {
                    routeShiftClickToInputs(holder, current);
                    scheduleSync(holder, event.getWhoClicked() instanceof Player p ? p : null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof PaperMenuHolder holder)) {
            return;
        }

        MenuDefinition menu = holder.getMenu();
        int menuSize = menu.size();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < menuSize) {
                Optional<SlotDefinition> slotOpt = menu.getSlotAt(rawSlot);
                if (slotOpt.isEmpty() || slotOpt.get().type() != SlotType.INPUT) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        scheduleSync(holder, event.getWhoClicked() instanceof Player p ? p : null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PaperMenuHolder holder)) {
            return;
        }

        syncStorageFromGui(holder);
        if (event.getPlayer() instanceof Player player && holder.getStation().isPresent()) {
            processEngine.tryStartProcess(holder.getStation().get(), holder.getContext().with("player", player));
        }
    }

    private void handleActionClick(Player player, PaperMenuHolder holder, SlotDefinition slotDef, ClickType clickType) {
        for (MenuAction action : slotDef.actions()) {
            if (!matchesFilter(action.click(), clickType)) {
                continue;
            }
            if (!MenuExpressionEvaluator.isActionAllowed(action, holder.getContext())) {
                continue;
            }

            executeAction(player, holder, action);
        }
        renderer.refreshDynamicSlots(holder);
    }

    private void executeAction(Player player, PaperMenuHolder holder, MenuAction action) {
        MenuActionType type = action.type();
        String target = action.target();

        switch (type) {
            case STATION_ACTION -> {
                if (holder.getStation().isPresent()) {
                    StationInstance station = holder.getStation().get();
                    if ("START_PROCESS".equalsIgnoreCase(target)) {
                        syncStorageFromGui(holder);
                        processEngine.tryStartProcess(station, holder.getContext().with("player", player));
                    } else if ("EXPERIMENT".equalsIgnoreCase(target) || "DISCOVER_RECIPE".equalsIgnoreCase(target)) {
                        syncStorageFromGui(holder);
                        if (experimentationHandler != null) {
                            experimentationHandler.handleExperimentation(player, station);
                        }
                    } else if ("CANCEL_PROCESS".equalsIgnoreCase(target)) {
                        station.getComponent(ProcessComponent.class).ifPresent(ProcessComponent::clearActiveProcess);
                    } else if ("TOGGLE_POWER".equalsIgnoreCase(target)) {
                        station.getComponent(HeatSourceComponent.class).ifPresent(heat -> {
                            heat.setActive(!heat.isActive());
                        });
                    } else if (target.toUpperCase(Locale.ROOT).startsWith("ADJUST_TEMP:")
                            || "INCREASE_HEAT".equalsIgnoreCase(target)
                            || "DECREASE_HEAT".equalsIgnoreCase(target)) {
                        station.getComponent(HeatSourceComponent.class).ifPresent(heat -> {
                            double delta = 0.0;
                            if ("INCREASE_HEAT".equalsIgnoreCase(target)) {
                                delta = 10.0;
                            } else if ("DECREASE_HEAT".equalsIgnoreCase(target)) {
                                delta = -10.0;
                            } else {
                                String rawDelta = target.substring("ADJUST_TEMP:".length()).trim();
                                if (rawDelta.startsWith("+")) {
                                    rawDelta = rawDelta.substring(1);
                                }
                                try {
                                    delta = Double.parseDouble(rawDelta);
                                } catch (NumberFormatException ignored) {}
                            }
                            heat.setTargetTemperature(heat.getTargetTemperature() + delta);
                        });
                    } else if ("PROMPT_CHAT_TEMP".equalsIgnoreCase(target) || "SET_TEMP_CHAT".equalsIgnoreCase(target)) {
                        station.getComponent(HeatSourceComponent.class).ifPresent(heat -> {
                            if (chatPromptService != null) {
                                chatPromptService.startTemperaturePrompt(
                                        player,
                                        heat.getMinTemperature(),
                                        heat.getMaxTemperature(),
                                        newTemp -> {
                                            heat.setTargetTemperature(newTemp);
                                            if (player.isOnline() && menuService != null) {
                                                menuService.openStationMenu(player, station);
                                            }
                                        },
                                        () -> {
                                            if (player.isOnline() && menuService != null) {
                                                menuService.openStationMenu(player, station);
                                            }
                                        }
                                );
                            }
                        });
                    }
                }
            }
            case PLAYER_COMMAND -> {
                String cmd = PaperMenuRenderer.replacePlaceholders(target, holder.getContext());
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                player.performCommand(cmd);
            }
            case CONSOLE_COMMAND -> {
                String cmd = PaperMenuRenderer.replacePlaceholders(target, holder.getContext());
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            case SOUND -> {
                try {
                    player.playSound(player.getLocation(), target, 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
            case MESSAGE -> {
                String msg = PaperMenuRenderer.replacePlaceholders(target, holder.getContext());
                player.sendMessage(miniMessage.deserialize(msg));
            }
            case CLOSE -> player.closeInventory();
            default -> {}
        }
    }

    private void routeShiftClickToInputs(PaperMenuHolder holder, ItemStack item) {
        Inventory inv = holder.getInventory();
        if (inv == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return;
        }

        List<Integer> inputSlots = holder.getMenu().getSlotsForType(SlotType.INPUT);
        int remaining = item.getAmount();

        // 1. Try stacking into existing matching input stacks
        for (int slot : inputSlots) {
            ItemStack existing = inv.getItem(slot);
            if (existing != null && existing.isSimilar(item)) {
                int max = existing.getMaxStackSize();
                int space = max - existing.getAmount();
                if (space > 0) {
                    int add = Math.min(space, remaining);
                    existing.setAmount(existing.getAmount() + add);
                    remaining -= add;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }

        // 2. Place into empty input slots
        if (remaining > 0) {
            for (int slot : inputSlots) {
                ItemStack existing = inv.getItem(slot);
                if (existing == null || existing.getType().isAir()) {
                    ItemStack placed = item.clone();
                    placed.setAmount(remaining);
                    inv.setItem(slot, placed);
                    remaining = 0;
                    break;
                }
            }
        }

        // Update player's stack
        item.setAmount(remaining);
    }

    private void scheduleSync(PaperMenuHolder holder, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            syncStorageFromGui(holder);
            if (player != null && holder.getStation().isPresent()) {
                EvaluationContext ctx = holder.getContext().with("player", player);
                holder.setContext(ctx);
                processEngine.tryStartProcess(holder.getStation().get(), ctx);
            }
            renderer.refreshDynamicSlots(holder);
        });
    }

    public void syncStorageFromGui(PaperMenuHolder holder) {
        Optional<StationInstance> stationOpt = holder.getStation();
        if (stationOpt.isEmpty()) {
            return;
        }

        StationInstance station = stationOpt.get();
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        StorageComponent storage = storageOpt.get();
        storage.clear();

        Inventory inv = holder.getInventory();
        if (inv == null) {
            return;
        }

        MenuDefinition menu = holder.getMenu();

        // Sync inputs
        for (int slot : menu.getSlotsForType(SlotType.INPUT)) {
            ItemStack inItem = inv.getItem(slot);
            if (inItem != null && !inItem.getType().isAir()) {
                Key key = resolveItemKey(inItem);
                storage.addInput(StoredItem.of(key, inItem.getAmount()));
            }
        }

        // Sync outputs
        for (int slot : menu.getSlotsForType(SlotType.OUTPUT)) {
            ItemStack outItem = inv.getItem(slot);
            if (outItem != null && !outItem.getType().isAir()) {
                Key key = resolveItemKey(outItem);
                storage.addOutput(StoredItem.of(key, outItem.getAmount()));
            }
        }
    }

    private Key resolveItemKey(ItemStack item) {
        return itemFactory.getItemId(item)
                .orElseGet(() -> Key.of("minecraft", item.getType().name().toLowerCase(Locale.ROOT)));
    }

    private boolean isPlacingAction(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR
                || action == InventoryAction.HOTBAR_SWAP;
    }

    private boolean matchesFilter(ClickTypeFilter filter, ClickType clickType) {
        if (filter == ClickTypeFilter.ALL) return true;
        return switch (filter) {
            case LEFT -> clickType == ClickType.LEFT;
            case RIGHT -> clickType == ClickType.RIGHT;
            case SHIFT_LEFT -> clickType == ClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> clickType == ClickType.SHIFT_RIGHT;
            case SHIFT -> clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;
            case MIDDLE -> clickType == ClickType.MIDDLE;
            default -> false;
        };
    }
}
