package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemCategory;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.prompt.ChatPromptService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller orchestrating the interactive item catalog GUI (/croppy items),
 * handling session state, category tabs, horizontal scrolling, real-time search,
 * and multi-action administrative item spawning.
 */
public class ItemCatalogController {

    public static final Key MENU_KEY = Key.of("croppy", "items_menu");
    public static final NamespacedKey CATALOG_ITEM_KEY = new NamespacedKey("croppy", "catalog_item_id");
    public static final NamespacedKey CATEGORY_TAB_KEY = new NamespacedKey("croppy", "category_tab");

    private final MenuRegistry menuRegistry;
    private final ItemRegistry itemRegistry;
    private final ItemCategoryRegistry categoryRegistry;
    private final ItemFactory itemFactory;
    private final PaperMenuRenderer renderer;
    private final PaperMessageService messageService;
    private ChatPromptService chatPromptService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Map<UUID, ItemCatalogSession> sessions = new ConcurrentHashMap<>();

    public record ItemCatalogSession(
            int page,
            Key categoryKey,
            String searchQuery,
            int categoryPage
    ) {
        public ItemCatalogSession(int page, Key categoryKey) {
            this(page, categoryKey, null, 1);
        }

        public ItemCatalogSession withPage(int newPage) {
            return new ItemCatalogSession(newPage, categoryKey, searchQuery, categoryPage);
        }

        public ItemCatalogSession withCategory(Key newCat) {
            return new ItemCatalogSession(1, newCat, searchQuery, categoryPage);
        }

        public ItemCatalogSession withSearch(String query) {
            return new ItemCatalogSession(1, categoryKey, query, categoryPage);
        }

        public ItemCatalogSession withCategoryPage(int newCatPage) {
            return new ItemCatalogSession(page, categoryKey, searchQuery, newCatPage);
        }
    }

    public ItemCatalogController(
            MenuRegistry menuRegistry,
            ItemRegistry itemRegistry,
            ItemCategoryRegistry categoryRegistry,
            ItemFactory itemFactory,
            PaperMenuRenderer renderer,
            PaperMessageService messageService,
            ChatPromptService chatPromptService
    ) {
        this.menuRegistry = Objects.requireNonNull(menuRegistry, "menuRegistry cannot be null");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry cannot be null");
        this.categoryRegistry = Objects.requireNonNull(categoryRegistry, "categoryRegistry cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.messageService = messageService;
        this.chatPromptService = chatPromptService;
    }

    public void setChatPromptService(ChatPromptService chatPromptService) {
        this.chatPromptService = chatPromptService;
    }

    public void openCatalog(Player player) {
        openCatalog(player, ItemCategory.ALL.getKey(), 1);
    }

    public void openCatalog(Player player, Key categoryKey, int page) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(page, categoryKey));
        session = session.withCategory(categoryKey).withPage(page);
        openCatalogWithSession(player, session);
    }

    public void openCatalogWithSession(Player player, ItemCatalogSession session) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(session, "session cannot be null");

        Optional<MenuDefinition> menuOpt = menuRegistry.get(MENU_KEY);
        if (menuOpt.isEmpty()) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, "catalog.missing-config");
            } else {
                player.sendMessage(miniMessage.deserialize("<red>Item catalog menu configuration is missing.</red>"));
            }
            return;
        }

        MenuDefinition menu = menuOpt.get();
        List<ItemDefinition> items = getFilteredItems(player, session);

        // Collect all slot indexes corresponding to item display slots ('I')
        List<Integer> itemSlotIndexes = new ArrayList<>();
        List<Integer> categorySlotIndexes = new ArrayList<>();
        int totalSlots = menu.size();
        for (int i = 0; i < totalSlots; i++) {
            char symbol = Character.toUpperCase(menu.getSymbolAt(i));
            if (symbol == 'I') {
                itemSlotIndexes.add(i);
            } else if (symbol == 'C') {
                categorySlotIndexes.add(i);
            }
        }

        if (itemSlotIndexes.isEmpty()) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, "catalog.no-slots");
            } else {
                player.sendMessage(miniMessage.deserialize("<red>No item display slots found in catalog menu layout.</red>"));
            }
            return;
        }

        int pageSize = itemSlotIndexes.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / pageSize));
        int currentPage = Math.max(1, Math.min(session.page(), maxPages));

        ItemCatalogSession resolvedSession = new ItemCatalogSession(
                currentPage, session.categoryKey(), session.searchQuery(), session.categoryPage()
        );
        sessions.put(player.getUniqueId(), resolvedSession);

        EvaluationContext context = EvaluationContext.empty()
                .with("page", currentPage)
                .with("max_page", maxPages)
                .with("category", session.categoryKey().value())
                .with("total_items", items.size())
                .with("player", player);

        PaperMenuHolder holder = new PaperMenuHolder(menu, null, context);
        Inventory inv = renderer.createInventory(holder);

        // Populate category tabs in row 0
        populateCategoryTabs(inv, categorySlotIndexes, resolvedSession);

        // Populate dynamic item slots in rows 1..4
        int startIndex = (currentPage - 1) * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int slot = itemSlotIndexes.get(i);
            int itemIndex = startIndex + i;

            if (itemIndex < items.size()) {
                ItemDefinition def = items.get(itemIndex);
                ItemStack card = renderItemCard(player, def);
                inv.setItem(slot, card);
            } else {
                inv.setItem(slot, null);
            }
        }

        player.openInventory(inv);
    }

    private void populateCategoryTabs(Inventory inv, List<Integer> categorySlotIndexes, ItemCatalogSession session) {
        if (categorySlotIndexes.isEmpty()) {
            return;
        }

        List<ItemCategory> categories = new ArrayList<>(categoryRegistry.values());
        Collections.sort(categories);

        int catPageSize = categorySlotIndexes.size();
        int maxCatPages = Math.max(1, (int) Math.ceil((double) categories.size() / catPageSize));
        int currentCatPage = Math.max(1, Math.min(session.categoryPage(), maxCatPages));
        int catStartIndex = (currentCatPage - 1) * catPageSize;

        for (int i = 0; i < catPageSize; i++) {
            int slot = categorySlotIndexes.get(i);
            int catIndex = catStartIndex + i;

            if (catIndex < categories.size()) {
                ItemCategory category = categories.get(catIndex);
                ItemStack tabStack = renderCategoryTab(category, session.categoryKey());
                inv.setItem(slot, tabStack);
            } else {
                inv.setItem(slot, null);
            }
        }
    }

    private ItemStack renderCategoryTab(ItemCategory category, Key activeCategoryKey) {
        Material material = Material.matchMaterial(category.iconMaterial());
        if (material == null || material.isAir()) {
            material = Material.CHEST;
        }

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(category.displayName()));

            List<Component> lore = new ArrayList<>();
            boolean isActive = category.key().equals(activeCategoryKey);
            if (isActive) {
                try {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } catch (Exception ignored) {}
                lore.add(miniMessage.deserialize("<green>✔ Currently Active Filter</green>"));
            } else {
                lore.add(miniMessage.deserialize("<gray>Click to filter items.</gray>"));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(CATEGORY_TAB_KEY, PersistentDataType.STRING, category.key().asString());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack renderItemCard(Player player, ItemDefinition def) {
        ItemStack baseStack = itemFactory.createItemStack(def, 1);
        ItemStack card = baseStack.clone();
        ItemMeta meta = card.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(miniMessage.deserialize("<dark_gray>------------------------</dark_gray>"));
            lore.add(miniMessage.deserialize("<yellow>• Left-Click:</yellow> <gray>Give 1x</gray>"));
            lore.add(miniMessage.deserialize("<yellow>• Right-Click:</yellow> <gray>Give 64x (Max Stack)</gray>"));
            lore.add(miniMessage.deserialize("<yellow>• Shift-Click:</yellow> <gray>Suggest /give command</gray>"));
            lore.add(miniMessage.deserialize("<yellow>• Drop [Q]:</yellow> <gray>Drop at feet</gray>"));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(CATALOG_ITEM_KEY, PersistentDataType.STRING, def.key().asString());
            card.setItemMeta(meta);
        }
        return card;
    }

    public List<ItemDefinition> getFilteredItems(Player player, ItemCatalogSession session) {
        List<ItemDefinition> base;
        if (session.categoryKey().equals(ItemCategory.ALL.getKey())) {
            base = new ArrayList<>(itemRegistry.values());
        } else {
            base = new ArrayList<>(itemRegistry.getByCategory(session.categoryKey().value()));
        }

        if (session.searchQuery() != null && !session.searchQuery().isBlank()) {
            String query = session.searchQuery().toLowerCase(Locale.ROOT).trim();
            base = base.stream().filter(item -> matchesSearch(item, query)).toList();
        }

        return base.stream().sorted(Comparator.comparing(a -> a.key().asString())).toList();
    }

    private boolean matchesSearch(ItemDefinition def, String query) {
        if (def.key().asString().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (def.category().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (def.materialKey().toLowerCase(Locale.ROOT).contains(query)) return true;
        String plainName = miniMessage.stripTags(def.displayName()).toLowerCase(Locale.ROOT);
        return plainName.contains(query);
    }

    public void nextPage(Player player) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(1, ItemCategory.ALL.getKey()));
        openCatalogWithSession(player, session.withPage(session.page() + 1));
    }

    public void prevPage(Player player) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(1, ItemCategory.ALL.getKey()));
        openCatalogWithSession(player, session.withPage(Math.max(1, session.page() - 1)));
    }

    public void nextCategoryPage(Player player) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(1, ItemCategory.ALL.getKey()));
        openCatalogWithSession(player, session.withCategoryPage(session.categoryPage() + 1));
    }

    public void prevCategoryPage(Player player) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(1, ItemCategory.ALL.getKey()));
        openCatalogWithSession(player, session.withCategoryPage(Math.max(1, session.categoryPage() - 1)));
    }

    public void setCategory(Player player, Key categoryKey) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(1, ItemCategory.ALL.getKey()));
        openCatalogWithSession(player, session.withCategory(categoryKey));
    }

    public void setSearchQuery(Player player, String query) {
        ItemCatalogSession session = sessions.getOrDefault(player.getUniqueId(),
                new ItemCatalogSession(1, ItemCategory.ALL.getKey()));
        openCatalogWithSession(player, session.withSearch(query));
    }

    public void clearFilters(Player player) {
        ItemCatalogSession fresh = new ItemCatalogSession(1, ItemCategory.ALL.getKey(), null, 1);
        sessions.put(player.getUniqueId(), fresh);
        openCatalogWithSession(player, fresh);
    }

    public void startSearch(Player player) {
        if (chatPromptService != null) {
            chatPromptService.startItemSearchPrompt(
                    player,
                    query -> {
                        if (query == null || query.isBlank()) {
                            setSearchQuery(player, null);
                        } else {
                            setSearchQuery(player, query);
                            if (messageService != null) {
                                messageService.sendPrefixedMessage(player, "catalog.filter.search", "query", query);
                            }
                        }
                    },
                    () -> openCatalog(player)
            );
        }
    }

    public Optional<ItemCatalogSession> getSession(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void removeSession(UUID playerId) {
        if (playerId != null) {
            sessions.remove(playerId);
        }
    }

    public void handleItemSpawn(Player player, ItemDefinition def, int amount) {
        ItemStack stack = itemFactory.createItemStack(def, amount);
        var leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        if (messageService != null) {
            Component itemName = messageService.resolveItemDisplayName(player, def);
            messageService.sendPrefixedMessage(player, "catalog.feedback.given", "amount", amount, "item", itemName);
        }
    }

    public void handleItemDrop(Player player, ItemDefinition def, int amount) {
        ItemStack stack = itemFactory.createItemStack(def, amount);
        player.getWorld().dropItemNaturally(player.getLocation(), stack);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        if (messageService != null) {
            Component itemName = messageService.resolveItemDisplayName(player, def);
            messageService.sendPrefixedMessage(player, "catalog.feedback.dropped", "amount", amount, "item", itemName);
        }
    }

    public void handleCommandSuggest(Player player, ItemDefinition def) {
        String suggest = "/croppy item give " + def.category() + " " + def.key().asString() + " 1";
        Component msg = miniMessage.deserialize(
                "<gray>Command: <yellow><click:suggest_command:'" + suggest + "'><hover:show_text:'<gray>Click to insert into chat</gray>'>" + suggest + "</click></yellow></gray>"
        );
        player.sendMessage(msg);
    }
}
