package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseType;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller managing the recipe inspection detail sub-menu, rendering
 * step-by-step sequential cooking phases, required temperatures, timeouts,
 * manual interactions, and failure consequences.
 */
public class RecipeDetailMenuController {

    public static final Key DETAIL_MENU_KEY = Key.of("croppy", "recipe_detail_menu");
    private static final NamespacedKey ACTION_KEY = new NamespacedKey("croppy", "action");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final MenuRegistry menuRegistry;
    private final ProcessRegistry processRegistry;
    private final StationRegistry stationRegistry;
    private final ItemFactory itemFactory;
    private final PaperMenuRenderer renderer;
    private final PaperMessageService messageService;
    private final CompendiumMenuController compendiumController;
    private final int stationProximityDistance;
    private final RecipeCategoryRegistry categoryRegistry;

    public RecipeDetailMenuController(
            MenuRegistry menuRegistry,
            ProcessRegistry processRegistry,
            StationRegistry stationRegistry,
            ItemFactory itemFactory,
            PaperMenuRenderer renderer,
            PaperMessageService messageService,
            CompendiumMenuController compendiumController,
            int stationProximityDistance
    ) {
        this(menuRegistry, processRegistry, stationRegistry, itemFactory, renderer, messageService, compendiumController, stationProximityDistance, null);
    }

    public RecipeDetailMenuController(
            MenuRegistry menuRegistry,
            ProcessRegistry processRegistry,
            StationRegistry stationRegistry,
            ItemFactory itemFactory,
            PaperMenuRenderer renderer,
            PaperMessageService messageService,
            CompendiumMenuController compendiumController,
            int stationProximityDistance,
            RecipeCategoryRegistry categoryRegistry
    ) {
        this.menuRegistry = Objects.requireNonNull(menuRegistry, "menuRegistry cannot be null");
        this.processRegistry = processRegistry;
        this.stationRegistry = stationRegistry;
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.messageService = messageService;
        this.compendiumController = compendiumController;
        this.stationProximityDistance = Math.max(1, stationProximityDistance);
        this.categoryRegistry = categoryRegistry;
    }

    /**
     * Opens the detailed preparation inspection sub-menu for a discovered recipe.
     *
     * @param player player viewing the recipe
     * @param recipe recipe definition to inspect
     */
    public void openDetail(Player player, RecipeDefinition recipe) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");

        Optional<MenuDefinition> menuOpt = menuRegistry.get(DETAIL_MENU_KEY);
        if (menuOpt.isEmpty()) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, "compendium.missing-detail-config");
            } else {
                player.sendMessage(miniMessage.deserialize("<red>Recipe detail menu configuration is missing.</red>"));
            }
            return;
        }

        MenuDefinition menu = menuOpt.get();

        String stationName = resolveStationDisplayName(recipe.stationKey());
        EvaluationContext context = EvaluationContext.empty()
                .with("recipe.name", recipe.displayName())
                .with("station.name", stationName)
                .with("player", player);

        PaperMenuHolder holder = new PaperMenuHolder(menu, null, context);
        Inventory inv = renderer.createInventory(holder);

        // 1. Populate Result Item slot ('R')
        int resultSlot = findFirstSlotBySymbol(menu, 'R');
        if (resultSlot != -1) {
            inv.setItem(resultSlot, renderResultCard(player, recipe));
        }

        // 2. Populate Station slot ('S')
        int stationSlot = findFirstSlotBySymbol(menu, 'S');
        if (stationSlot != -1) {
            inv.setItem(stationSlot, renderStationCard(player, recipe.stationKey(), stationName));
        }

        // 3. Populate Ingredients slots ('I')
        List<Integer> ingredientSlots = findAllSlotsBySymbol(menu, 'I');
        List<StoredItem> inputs = recipe.inputRequirements();
        for (int i = 0; i < ingredientSlots.size(); i++) {
            int slot = ingredientSlots.get(i);
            if (i < inputs.size()) {
                inv.setItem(slot, renderIngredientCard(player, inputs.get(i)));
            } else {
                inv.setItem(slot, null);
            }
        }

        // 4. Populate Phase Timeline slots ('1' through '7')
        Optional<ProcessDefinition> procOpt = (processRegistry != null)
                ? processRegistry.get(recipe.processKey())
                : Optional.empty();

        List<PhaseDefinition> phases = procOpt.map(ProcessDefinition::phases).orElse(List.of());
        for (int i = 1; i <= 7; i++) {
            char symbol = Character.forDigit(i, 10);
            int phaseSlot = findFirstSlotBySymbol(menu, symbol);
            if (phaseSlot != -1) {
                int phaseIndex = i - 1;
                if (phaseIndex < phases.size()) {
                    inv.setItem(phaseSlot, renderPhaseCard(player, i, phases.get(phaseIndex)));
                } else {
                    inv.setItem(phaseSlot, null);
                }
            }
        }

        // 5. Back button ('<')
        int backSlot = findFirstSlotBySymbol(menu, '<');
        if (backSlot != -1) {
            ItemStack backItem = inv.getItem(backSlot);
            if (backItem != null && backItem.hasItemMeta()) {
                ItemMeta meta = backItem.getItemMeta();
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "compendium_back");
                backItem.setItemMeta(meta);
            }
        }

        player.openInventory(inv);
    }

    protected ItemStack renderResultCard(Player player, RecipeDefinition recipe) {
        ItemStack stack;
        var defOpt = itemFactory.getItemRegistry().get(recipe.resultItemKey());
        if (defOpt.isPresent()) {
            stack = itemFactory.createItemStack(defOpt.get());
        } else {
            Material material = resolveMaterial(recipe.resultItemKey());
            stack = new ItemStack(material);
        }
        stack.setAmount(Math.max(1, recipe.resultAmount()));

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        Component recipeName = (messageService != null)
                ? messageService.resolveRecipeDisplayName(player, recipe)
                : miniMessage.deserialize(recipe.displayName());
        Component name = (messageService != null)
                ? messageService.render(player, "compendium.gui.discovered-title", "recipe", recipeName)
                        .decoration(TextDecoration.ITALIC, false)
                : recipeName.decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        meta.lore(buildResultLore(player, recipe));
        stack.setItemMeta(meta);
        return stack;
    }

    List<Component> buildResultLore(Player player, RecipeDefinition recipe) {
        List<Component> lore = new ArrayList<>();
        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.yields", "amount", recipe.resultAmount())
                : miniMessage.deserialize("<gray>Yields: <yellow>" + recipe.resultAmount() + "x</yellow></gray>"));
        lore.add(CompendiumMenuController.buildCategoryLoreLine(
                player, recipe.categoryKey(), messageService, categoryRegistry, miniMessage));

        if (!recipe.properties().isEmpty()) {
            lore.add(Component.empty());
            for (Map.Entry<String, String> prop : recipe.properties().entrySet()) {
                lore.add(miniMessage.deserialize("<dark_gray>• " + prop.getKey() + ": <gray>" + prop.getValue() + "</gray></dark_gray>"));
            }
        }

        return lore;
    }

    protected ItemStack renderStationCard(Player player, Key stationKey, String stationName) {
        Material material = resolveMaterial(stationKey);
        if (material == Material.COOKED_BEEF || material.isAir()) {
            material = Material.CRAFTING_TABLE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        boolean nearby = compendiumController != null && compendiumController.isNearStation(player, stationKey, stationProximityDistance);
        Component stationTitle = miniMessage.deserialize("<aqua><bold>Station: </bold></aqua>")
                .append(miniMessage.deserialize(stationName))
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(stationTitle);

        List<Component> lore = new ArrayList<>();
        if (nearby) {
            lore.add(miniMessage.deserialize("<green>✔ Station detected nearby</green>"));
        } else {
            lore.add(miniMessage.deserialize("<gray>✖ No station detected nearby (< 5m)</gray>"));
        }
        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<gray>Must be performed at a </gray>")
                .append(miniMessage.deserialize(stationName))
                .append(miniMessage.deserialize("<gray>.</gray>")));

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    protected ItemStack renderIngredientCard(Player player, StoredItem input) {
        ItemStack stack;
        var defOpt = itemFactory.getItemRegistry().get(input.itemKey());
        if (defOpt.isPresent()) {
            stack = itemFactory.createItemStack(defOpt.get());
        } else {
            Material material = resolveMaterial(input.itemKey());
            stack = new ItemStack(material);
        }
        stack.setAmount(Math.max(1, Math.min(64, input.amount())));

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        int available = (compendiumController != null) ? compendiumController.countPlayerItem(player, input.itemKey()) : 0;
        boolean satisfied = available >= input.amount();

        Component itemName = resolveItemDisplayName(player, input.itemKey());
        TextColor color = satisfied ? NamedTextColor.GREEN : NamedTextColor.RED;
        String statusPrefix = satisfied ? "✔ " : "✖ ";

        Component nameComp = Component.text(statusPrefix, color)
                .append(itemName)
                .append(Component.text(" x" + input.amount(), color))
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(nameComp);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("In Inventory: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(available), color))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(input.amount()), NamedTextColor.WHITE)));
        if (satisfied) {
            lore.add(miniMessage.deserialize("<green>Ready for cooking!</green>"));
        } else {
            lore.add(miniMessage.deserialize("<red>Missing " + (input.amount() - available) + " more.</red>"));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    protected ItemStack renderPhaseCard(Player player, int stepNumber, PhaseDefinition phase) {
        Material mat = switch (phase.type()) {
            case TIMED -> Material.CLOCK;
            case MANUAL_INTERACTION -> (phase.requiredItem() != null) ? resolveMaterial(phase.requiredItem()) : Material.WOODEN_SHOVEL;
            case ENVIRONMENTAL -> Material.CAMPFIRE;
        };
        if (mat == null || mat.isAir()) {
            mat = Material.PAPER;
        }

        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(miniMessage.deserialize("<gold><bold>Step " + stepNumber + ": </bold><yellow>" + phase.id() + "</yellow></gold>")
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<gray>Type: <aqua>" + phase.type() + "</aqua></gray>"));

        if (phase.type() == PhaseType.TIMED && phase.durationTicks() > 0) {
            double secs = phase.durationTicks() / 20.0;
            lore.add(miniMessage.deserialize("<gray>Duration: <yellow>" + String.format(Locale.ROOT, "%.1fs", secs) + "</yellow></gray>"));
        }

        if (phase.type() == PhaseType.MANUAL_INTERACTION) {
            if (phase.timeoutTicks() > 0) {
                double timeoutSecs = phase.timeoutTicks() / 20.0;
                lore.add(miniMessage.deserialize("<gray>Time Window: <yellow>" + String.format(Locale.ROOT, "%.1fs", timeoutSecs) + "</yellow></gray>"));
            }
            if (phase.promptMessage() != null && !phase.promptMessage().isBlank()) {
                lore.add(miniMessage.deserialize("<yellow>Action: <white>" + phase.promptMessage() + "</white></yellow>"));
            }
            if (phase.requiredItem() != null) {
                Component toolName = resolveItemDisplayName(player, phase.requiredItem());
                lore.add(miniMessage.deserialize("<gray>Requires Tool: </gray>").append(toolName));
            }
            lore.add(miniMessage.deserialize("<gray>On Timeout: <red>" + phase.failurePolicy() + "</red></gray>"));
            if (phase.byproductItem() != null) {
                lore.add(miniMessage.deserialize("<dark_gray>• Spoils into: " + phase.byproductItem().value() + "</dark_gray>"));
            }
        }

        String condStr = phase.properties().get("condition");
        if (condStr != null && !condStr.isBlank()) {
            lore.add(miniMessage.deserialize("<gray>Condition: <gold>" + condStr + "</gold></gray>"));
        }

        if (phase.pauseOnConditionFailure()) {
            lore.add(miniMessage.deserialize("<dark_gray>• Pauses progress if conditions drop.</dark_gray>"));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private Component resolveItemDisplayName(Player player, Key itemKey) {
        if (itemFactory != null && itemFactory.getItemRegistry().get(itemKey).isPresent()) {
            var itemDef = itemFactory.getItemRegistry().get(itemKey).get();
            return (messageService != null)
                    ? messageService.resolveItemDisplayName(player, itemDef)
                    : miniMessage.deserialize(itemDef.displayName());
        }
        String rawName = itemKey.value().replace('_', ' ');
        String titleCase = Arrays.stream(rawName.split(" "))
                .map(w -> w.isEmpty() ? "" : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        return Component.text(titleCase);
    }

    String resolveStationDisplayName(Key stationKey) {
        if (stationRegistry != null) {
            Optional<StationDefinition> def = stationRegistry.get(stationKey);
            if (def.isPresent() && def.get().displayName() != null) {
                return def.get().displayName();
            }
        }
        String val = stationKey.value().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : val.split(" ")) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }

    private Material resolveMaterial(Key key) {
        if (itemFactory != null) {
            var defOpt = itemFactory.getItemRegistry().get(key);
            if (defOpt.isPresent()) {
                String matKey = defOpt.get().materialKey();
                if (matKey.toUpperCase(Locale.ROOT).startsWith("MINECRAFT:")) {
                    matKey = matKey.substring("MINECRAFT:".length());
                }
                Material mat = Material.matchMaterial(matKey);
                if (mat != null && !mat.isAir()) {
                    return mat;
                }
            }
        }
        Material mat = Material.matchMaterial(key.value());
        if (mat != null && !mat.isAir()) {
            return mat;
        }
        return Material.COOKED_BEEF;
    }

    private int findFirstSlotBySymbol(MenuDefinition menu, char symbol) {
        char upper = Character.toUpperCase(symbol);
        for (int i = 0; i < menu.size(); i++) {
            if (Character.toUpperCase(menu.getSymbolAt(i)) == upper) {
                return i;
            }
        }
        return -1;
    }

    private List<Integer> findAllSlotsBySymbol(MenuDefinition menu, char symbol) {
        List<Integer> slots = new ArrayList<>();
        char upper = Character.toUpperCase(symbol);
        for (int i = 0; i < menu.size(); i++) {
            if (Character.toUpperCase(menu.getSymbolAt(i)) == upper) {
                slots.add(i);
            }
        }
        return slots;
    }
}
