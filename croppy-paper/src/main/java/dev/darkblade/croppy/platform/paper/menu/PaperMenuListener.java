package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;

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
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.core.menu.MenuExpressionEvaluator;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.prompt.ChatPromptService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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
    private PaperMessageService messageService;
    private dev.darkblade.croppy.platform.paper.discovery.StationExperimentationHandler experimentationHandler;
    private RecipeRegistry recipeRegistry;
    private RecipeDiscoveryEngine discoveryEngine;
    private RecipeDetailMenuController recipeDetailController;
    private CompendiumMenuController compendiumController;

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
        this.messageService = (plugin instanceof CroppyPlugin cp) ? cp.getMessageService() : null;
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

    public void setRecipeRegistry(RecipeRegistry recipeRegistry) {
        this.recipeRegistry = recipeRegistry;
    }

    public void setDiscoveryEngine(RecipeDiscoveryEngine discoveryEngine) {
        this.discoveryEngine = discoveryEngine;
    }

    public void setRecipeDetailController(RecipeDetailMenuController recipeDetailController) {
        this.recipeDetailController = recipeDetailController;
    }

    public void setCompendiumController(CompendiumMenuController compendiumController) {
        this.compendiumController = compendiumController;
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
                if (event.getWhoClicked() instanceof Player player) {
                    handleSpecialMenuClick(player, holder, event.getCurrentItem());
                }
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
            StationInstance station = holder.getStation().get();
            PaperMenuTicker ticker = (menuService != null) ? menuService.getTicker() : null;
            EvaluationContext ctx = (ticker != null)
                    ? ticker.enrichContext(holder.getContext(), station, player)
                    : holder.getContext().with("player", player);
            processEngine.tryStartProcess(station, ctx);
        }
    }

    private void handleSpecialMenuClick(Player player, PaperMenuHolder holder, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1. Recipe card click in compendium menu -> open detailed inspection
        if (CompendiumMenuController.MENU_KEY.equals(holder.getMenu().getKey())) {
            String recKeyStr = pdc.get(CompendiumMenuController.RECIPE_KEY, PersistentDataType.STRING);
            if (recKeyStr != null && recipeRegistry != null && discoveryEngine != null && recipeDetailController != null) {
                Key recKey = Key.parse(recKeyStr);
                recipeRegistry.get(recKey).ifPresent(recipe -> {
                    KnowledgeState state = discoveryEngine.getKnowledgeState(player.getUniqueId(), recipe.key());
                    if (state == KnowledgeState.DISCOVERED) {
                        recipeDetailController.openDetail(player, recipe);
                    }
                });
            }
            return;
        }

        // 2. Back button click in recipe detail menu
        if (RecipeDetailMenuController.DETAIL_MENU_KEY.equals(holder.getMenu().getKey())) {
            String action = pdc.get(new NamespacedKey("croppy", "action"), PersistentDataType.STRING);
            if ("compendium_back".equalsIgnoreCase(action) && compendiumController != null) {
                compendiumController.backToCompendium(player);
            }
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
                        PaperMenuTicker ticker = (menuService != null) ? menuService.getTicker() : null;
                        EvaluationContext freshContext = (ticker != null)
                                ? ticker.enrichContext(holder.getContext(), station, player)
                                : holder.getContext().with("player", player);
                        boolean started = processEngine.tryStartProcess(station, freshContext);
                        if (started) {
                            EvaluationContext updatedContext = (ticker != null)
                                    ? ticker.enrichContext(freshContext, station, player)
                                    : freshContext.with("process.state", "RUNNING");
                            holder.setContext(updatedContext);
                            renderer.populateStorageContents(holder);
                            renderer.refreshDynamicSlots(holder);
                        } else {
                            Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
                            double temp = thermalOpt.map(ThermalComponent::getTemperature).orElse(20.0);
                            Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
                            boolean hasInputs = storageOpt.isPresent() && !storageOpt.get().getInputs().isEmpty();
                            if (!hasInputs) {
                                if (messageService != null) {
                                    messageService.sendPrefixedMessage(player, "station.cooking.missing-ingredients");
                                } else {
                                    player.sendMessage(miniMessage.deserialize("<red>No valid recipe ingredients found in the input slots.</red>"));
                                }
                            } else if (temp < 75.0) {
                                if (messageService != null) {
                                    messageService.sendPrefixedMessage(player, "station.cooking.not-hot-enough",
                                            "temp", (int) temp,
                                            "required", 75);
                                } else {
                                    player.sendMessage(miniMessage.deserialize("<red>The cookware is not hot enough! (<gold>" + (int) temp + "°C</gold> / <yellow>75°C required</yellow>).</red>"));
                                }
                            } else {
                                if (messageService != null) {
                                    messageService.sendPrefixedMessage(player, "station.cooking.no-recipe-match");
                                } else {
                                    player.sendMessage(miniMessage.deserialize("<red>No matching recipe found for the provided ingredients.</red>"));
                                }
                            }
                        }
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
                StationInstance station = holder.getStation().get();
                PaperMenuTicker ticker = (menuService != null) ? menuService.getTicker() : null;
                EvaluationContext ctx = (ticker != null)
                        ? ticker.enrichContext(holder.getContext(), station, player)
                        : holder.getContext().with("player", player);
                holder.setContext(ctx);

                if (!hasStartProcessButton(holder.getMenu())) {
                    boolean started = processEngine.tryStartProcess(station, ctx);
                    if (started) {
                        EvaluationContext updated = (ticker != null)
                                ? ticker.enrichContext(ctx, station, player)
                                : ctx.with("process.state", "RUNNING");
                        holder.setContext(updated);
                        renderer.populateStorageContents(holder);
                    }
                }
            }
            renderer.refreshDynamicSlots(holder);
        });
    }

    private boolean hasStartProcessButton(MenuDefinition menu) {
        if (menu == null) return false;
        for (int i = 0; i < menu.size(); i++) {
            Optional<SlotDefinition> slot = menu.getSlotAt(i);
            if (slot.isPresent() && slot.get().type() == SlotType.ACTION) {
                for (MenuAction action : slot.get().actions()) {
                    if (action.type() == MenuActionType.STATION_ACTION && "START_PROCESS".equalsIgnoreCase(action.target())) {
                        return true;
                    }
                }
            }
        }
        return false;
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

        station.getComponent(DisplayComponent.class).ifPresent(display -> {
            if (!storage.getInputs().isEmpty()) {
                display.setDisplayedItem(storage.getInputs().get(0).itemKey());
            } else if (!storage.getOutputs().isEmpty()) {
                display.setDisplayedItem(storage.getOutputs().get(0).itemKey());
            } else {
                display.setDisplayedItem(null);
                display.setStatusText(null);
            }
            display.setDirty(true);
        });
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
