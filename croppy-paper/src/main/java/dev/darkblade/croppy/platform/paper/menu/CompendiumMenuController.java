package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller orchestrating the culinary compendium cookbook interface,
 * handling pagination, category filtering, and state-aware recipe card rendering.
 */
public class CompendiumMenuController {

    public static final Key MENU_KEY = Key.of("croppy", "recipes_menu");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey itemIdKey = new NamespacedKey("croppy", "item_id");

    private final MenuRegistry menuRegistry;
    private final RecipeRegistry recipeRegistry;
    private final RecipeDiscoveryEngine discoveryEngine;
    private final PaperMenuRenderer renderer;
    private final ItemFactory itemFactory;

    private final Map<UUID, CompendiumSession> sessions = new ConcurrentHashMap<>();

    public record CompendiumSession(int page, Key categoryKey) {}

    public CompendiumMenuController(
            MenuRegistry menuRegistry,
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            PaperMenuRenderer renderer,
            ItemFactory itemFactory
    ) {
        this.menuRegistry = Objects.requireNonNull(menuRegistry, "menuRegistry cannot be null");
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "recipeRegistry cannot be null");
        this.discoveryEngine = Objects.requireNonNull(discoveryEngine, "discoveryEngine cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
    }

    /**
     * Opens or refreshes the recipe compendium for a player with the given category and page.
     *
     * @param player      player viewing the compendium
     * @param categoryKey category filter (or ALL)
     * @param page        page index (1-based)
     */
    public void openCompendium(Player player, Key categoryKey, int page) {
        Objects.requireNonNull(player, "player cannot be null");
        Key cat = (categoryKey == null) ? RecipeCategory.ALL.getKey() : categoryKey;

        Optional<MenuDefinition> menuOpt = menuRegistry.get(MENU_KEY);
        if (menuOpt.isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<red>Recipe compendium menu configuration is missing.</red>"));
            return;
        }

        MenuDefinition menu = menuOpt.get();
        List<RecipeDefinition> recipes = recipeRegistry.findByCategory(cat);

        // Collect all slot indexes corresponding to recipe slots ('R')
        List<Integer> recipeSlotIndexes = new ArrayList<>();
        int totalSlots = menu.size();
        for (int i = 0; i < totalSlots; i++) {
            if (Character.toUpperCase(menu.getSymbolAt(i)) == 'R') {
                recipeSlotIndexes.add(i);
            }
        }

        if (recipeSlotIndexes.isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<red>No recipe display slots ('R') found in compendium menu layout.</red>"));
            return;
        }

        int pageSize = recipeSlotIndexes.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) recipes.size() / pageSize));
        int currentPage = Math.max(1, Math.min(page, maxPages));

        sessions.put(player.getUniqueId(), new CompendiumSession(currentPage, cat));

        EvaluationContext context = EvaluationContext.empty()
                .with("page", currentPage)
                .with("max_page", maxPages)
                .with("category", cat.value())
                .with("player", player);

        PaperMenuHolder holder = new PaperMenuHolder(menu, null, context);
        Inventory inv = renderer.createInventory(holder);

        // Fill dynamic recipe slots
        int startIndex = (currentPage - 1) * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int slot = recipeSlotIndexes.get(i);
            int recipeIndex = startIndex + i;

            if (recipeIndex < recipes.size()) {
                RecipeDefinition recipe = recipes.get(recipeIndex);
                KnowledgeState state = discoveryEngine.getKnowledgeState(player.getUniqueId(), recipe.key());
                ItemStack card = renderRecipeCard(player, recipe, state);
                inv.setItem(slot, card);
            } else {
                inv.setItem(slot, null);
            }
        }

        player.openInventory(inv);
    }

    public void nextPage(Player player) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(), new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendium(player, session.categoryKey(), session.page() + 1);
    }

    public void prevPage(Player player) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(), new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendium(player, session.categoryKey(), Math.max(1, session.page() - 1));
    }

    public void setCategory(Player player, Key categoryKey) {
        openCompendium(player, categoryKey, 1);
    }

    /**
     * Retrieves the active compendium session for a player, if present.
     *
     * @param playerId player's unique identifier
     * @return optional session
     */
    public Optional<CompendiumSession> getSession(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return Optional.ofNullable(sessions.get(playerId));
    }

    /**
     * Renders an item card representing a recipe based on the player's knowledge state.
     */
    public ItemStack renderRecipeCard(Player player, RecipeDefinition recipe, KnowledgeState state) {
        return switch (state) {
            case DISCOVERED -> renderDiscoveredCard(player, recipe);
            case HINTED -> renderHintedCard(recipe);
            case UNKNOWN -> renderUnknownCard(recipe);
        };
    }

    private ItemStack renderDiscoveredCard(Player player, RecipeDefinition recipe) {
        ItemStack stack;
        var defOpt = itemFactory.getItemRegistry().get(recipe.resultItemKey());
        if (defOpt.isPresent()) {
            stack = itemFactory.createItemStack(defOpt.get());
        } else {
            Material material = resolveMaterial(recipe.resultItemKey());
            stack = new ItemStack(material);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        Component name = miniMessage.deserialize("<gold><bold>" + recipe.displayName() + "</bold></gold>")
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<gray>Category: <yellow>" + recipe.categoryKey().value() + "</yellow></gray>"));
        lore.add(miniMessage.deserialize("<gray>Station: <aqua>" + recipe.stationKey().value() + "</aqua></gray>"));
        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<yellow>Required Ingredients:</yellow>"));

        for (StoredItem input : recipe.inputRequirements()) {
            int available = countPlayerItem(player, input.itemKey());
            boolean satisfied = available >= input.amount();
            String prefix = satisfied ? "<green>✔ " : "<red>✖ ";
            String color = satisfied ? "<green>" : "<red>";
            String closeColor = satisfied ? "</green>" : "</red>";
            String line = prefix + color + input.amount() + "x " + input.itemKey().value() + " (" + available + "/" + input.amount() + ")" + closeColor;
            lore.add(miniMessage.deserialize(line));
        }

        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<dark_green>Status: Discovered</dark_green>"));

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack renderHintedCard(RecipeDefinition recipe) {
        Material material = recipe.hint().hideResultItem() ? Material.KNOWLEDGE_BOOK : resolveMaterial(recipe.resultItemKey());
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        Component name = miniMessage.deserialize("<gold>★ " + recipe.displayName() + " <italic><gray>(Hinted)</gray></italic></gold>")
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<gray>Category: <yellow>" + recipe.categoryKey().value() + "</yellow></gray>"));
        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<aqua><bold>Culinary Clue:</bold></aqua>"));
        lore.add(miniMessage.deserialize("<gray>" + recipe.hint().description() + "</gray>"));

        if (!recipe.hint().revealedIngredientKeys().isEmpty()) {
            lore.add(Component.empty());
            lore.add(miniMessage.deserialize("<yellow>Known Components:</yellow>"));
            for (Key rev : recipe.hint().revealedIngredientKeys()) {
                lore.add(miniMessage.deserialize("<aqua>• " + rev.value() + "</aqua>"));
            }
            lore.add(miniMessage.deserialize("<dark_gray>• ??? (Secret ingredients)</dark_gray>"));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack renderUnknownCard(RecipeDefinition recipe) {
        ItemStack stack = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.displayName(miniMessage.deserialize("<gray><italic>??? Unknown Recipe</italic></gray>"));
        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<dark_gray>Category: " + recipe.categoryKey().value() + "</dark_gray>"));
        lore.add(miniMessage.deserialize("<dark_gray>Station: " + recipe.stationKey().value() + "</dark_gray>"));
        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<gray>Experiment in crafting stations to discover this dish!</gray>"));

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private int countPlayerItem(Player player, Key itemKey) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                String customId = pdc.get(itemIdKey, PersistentDataType.STRING);
                if (customId != null && Key.parse(customId).equals(itemKey)) {
                    count += stack.getAmount();
                    continue;
                }
            }

            Key vanillaKey = Key.of("minecraft", stack.getType().name().toLowerCase(Locale.ROOT));
            if (vanillaKey.equals(itemKey)) {
                count += stack.getAmount();
            }
        }
        return count;
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
}
