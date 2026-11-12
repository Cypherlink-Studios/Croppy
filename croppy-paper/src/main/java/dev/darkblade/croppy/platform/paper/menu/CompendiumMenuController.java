package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.api.world.ChunkPosition;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Controller orchestrating the culinary compendium cookbook interface,
 * handling dynamic category navigation, search queries, inventory availability
 * filtering, station proximity checks, and state-aware recipe card rendering.
 */
public class CompendiumMenuController {

    public static final Key MENU_KEY = Key.of("croppy", "recipes_menu");
    public static final NamespacedKey RECIPE_KEY = new NamespacedKey("croppy", "recipe_key");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey itemIdKey = new NamespacedKey("croppy", "item_id");

    private final MenuRegistry menuRegistry;
    private final RecipeRegistry recipeRegistry;
    private final RecipeDiscoveryEngine discoveryEngine;
    private final PaperMenuRenderer renderer;
    private final ItemFactory itemFactory;
    private final PaperMessageService messageService;

    private RecipeCategoryRegistry categoryRegistry;
    private ProcessRegistry processRegistry;
    private StationRegistry stationRegistry;
    private DefaultStationManager stationManager;
    private boolean showUnknownRecipes = true;
    private int stationProximityDistance = 5;

    private final Map<UUID, CompendiumSession> sessions = new ConcurrentHashMap<>();

    public record CompendiumSession(
            int page,
            Key categoryKey,
            String searchQuery,
            boolean onlyReadyToCook,
            int categoryPage
    ) {
        public CompendiumSession(int page, Key categoryKey) {
            this(page, categoryKey, null, false, 1);
        }

        public CompendiumSession withPage(int newPage) {
            return new CompendiumSession(newPage, categoryKey, searchQuery, onlyReadyToCook, categoryPage);
        }

        public CompendiumSession withCategory(Key newCat) {
            return new CompendiumSession(1, newCat, searchQuery, onlyReadyToCook, 1);
        }

        public CompendiumSession withSearch(String query) {
            return new CompendiumSession(1, categoryKey, query, onlyReadyToCook, categoryPage);
        }

        public CompendiumSession withReadyToCook(boolean ready) {
            return new CompendiumSession(1, categoryKey, searchQuery, ready, categoryPage);
        }

        public CompendiumSession withCategoryPage(int newCatPage) {
            return new CompendiumSession(page, categoryKey, searchQuery, onlyReadyToCook, newCatPage);
        }
    }

    public CompendiumMenuController(
            MenuRegistry menuRegistry,
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            PaperMenuRenderer renderer,
            ItemFactory itemFactory,
            PaperMessageService messageService,
            RecipeCategoryRegistry categoryRegistry,
            ProcessRegistry processRegistry,
            StationRegistry stationRegistry,
            DefaultStationManager stationManager,
            boolean showUnknownRecipes,
            int stationProximityDistance
    ) {
        this.menuRegistry = Objects.requireNonNull(menuRegistry, "menuRegistry cannot be null");
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "recipeRegistry cannot be null");
        this.discoveryEngine = Objects.requireNonNull(discoveryEngine, "discoveryEngine cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.messageService = messageService;
        this.categoryRegistry = categoryRegistry;
        this.processRegistry = processRegistry;
        this.stationRegistry = stationRegistry;
        this.stationManager = stationManager;
        this.showUnknownRecipes = showUnknownRecipes;
        this.stationProximityDistance = Math.max(1, stationProximityDistance);
    }

    public CompendiumMenuController(
            MenuRegistry menuRegistry,
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            PaperMenuRenderer renderer,
            ItemFactory itemFactory,
            PaperMessageService messageService
    ) {
        this(menuRegistry, recipeRegistry, discoveryEngine, renderer, itemFactory, messageService, null, null, null, null, true, 5);
    }

    public CompendiumMenuController(
            MenuRegistry menuRegistry,
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            PaperMenuRenderer renderer,
            ItemFactory itemFactory
    ) {
        this(menuRegistry, recipeRegistry, discoveryEngine, renderer, itemFactory, null);
    }

    public void setCategoryRegistry(RecipeCategoryRegistry categoryRegistry) {
        this.categoryRegistry = categoryRegistry;
    }

    public void setProcessRegistry(ProcessRegistry processRegistry) {
        this.processRegistry = processRegistry;
    }

    public void setStationRegistry(StationRegistry stationRegistry) {
        this.stationRegistry = stationRegistry;
    }

    public void setStationManager(DefaultStationManager stationManager) {
        this.stationManager = stationManager;
    }

    public void setShowUnknownRecipes(boolean showUnknownRecipes) {
        this.showUnknownRecipes = showUnknownRecipes;
    }

    public void setStationProximityDistance(int stationProximityDistance) {
        this.stationProximityDistance = Math.max(1, stationProximityDistance);
    }

    /**
     * Opens or refreshes the recipe compendium for a player with the given category
     * and page.
     *
     * @param player      player viewing the compendium
     * @param categoryKey category filter (or ALL)
     * @param page        page index (1-based)
     */
    public void openCompendium(Player player, Key categoryKey, int page) {
        Objects.requireNonNull(player, "player cannot be null");
        Key cat = (categoryKey == null) ? RecipeCategory.ALL.getKey() : categoryKey;

        CompendiumSession currentSession = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(page, cat, null, false, 1));
        CompendiumSession session = new CompendiumSession(page, cat, currentSession.searchQuery(), currentSession.onlyReadyToCook(), currentSession.categoryPage());

        openCompendiumWithSession(player, session);
    }

    public void openCompendiumWithSession(Player player, CompendiumSession session) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(session, "session cannot be null");

        Optional<MenuDefinition> menuOpt = menuRegistry.get(MENU_KEY);
        if (menuOpt.isEmpty()) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, "compendium.missing-config");
            } else {
                player.sendMessage(miniMessage.deserialize("<red>Recipe compendium menu configuration is missing.</red>"));
            }
            return;
        }

        MenuDefinition menu = menuOpt.get();
        List<RecipeDefinition> recipes = getFilteredRecipes(player, session);

        // Collect all slot indexes corresponding to recipe slots ('R')
        List<Integer> recipeSlotIndexes = new ArrayList<>();
        int totalSlots = menu.size();
        for (int i = 0; i < totalSlots; i++) {
            if (Character.toUpperCase(menu.getSymbolAt(i)) == 'R') {
                recipeSlotIndexes.add(i);
            }
        }

        if (recipeSlotIndexes.isEmpty()) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, "compendium.no-slots");
            } else {
                player.sendMessage(miniMessage.deserialize("<red>No recipe display slots found in compendium menu layout.</red>"));
            }
            return;
        }

        int pageSize = recipeSlotIndexes.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) recipes.size() / pageSize));
        int currentPage = Math.max(1, Math.min(session.page(), maxPages));

        CompendiumSession resolvedSession = new CompendiumSession(currentPage, session.categoryKey(), session.searchQuery(), session.onlyReadyToCook(), session.categoryPage());
        sessions.put(player.getUniqueId(), resolvedSession);

        EvaluationContext context = EvaluationContext.empty()
                .with("page", currentPage)
                .with("max_page", maxPages)
                .with("category", miniMessage.serialize(resolveCategoryComponent(player, session.categoryKey())))
                .with("filter_ready", session.onlyReadyToCook() ? "ON" : "OFF")
                .with("player", player);

        PaperMenuHolder holder = new PaperMenuHolder(menu, null, context);
        Inventory inv = renderer.createInventory(holder);

        // Highlight active category tab in row 0
        highlightActiveCategory(inv, menu, session.categoryKey());

        // Update Ready to Cook filter button ('F')
        updateFilterButton(inv, menu, session.onlyReadyToCook());

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

    /**
     * Filters and sorts recipes matching the current session criteria.
     */
    public List<RecipeDefinition> getFilteredRecipes(Player player, CompendiumSession session) {
        List<RecipeDefinition> base;
        if (session.categoryKey().equals(RecipeCategory.ALL.getKey())) {
            base = new ArrayList<>(recipeRegistry.values());
        } else {
            base = recipeRegistry.findByCategory(session.categoryKey());
        }

        // Search query filter
        if (session.searchQuery() != null && !session.searchQuery().isBlank()) {
            String query = session.searchQuery().toLowerCase(Locale.ROOT).trim();
            base = base.stream().filter(r -> matchesSearch(player, r, query)).toList();
        }

        // Ready to cook filter
        if (session.onlyReadyToCook()) {
            base = base.stream().filter(r -> {
                KnowledgeState state = discoveryEngine.getKnowledgeState(player.getUniqueId(), r.key());
                return state == KnowledgeState.DISCOVERED && isReadyToCook(player, r);
            }).toList();
        }

        // Unknown visibility filter
        if (!showUnknownRecipes) {
            base = base.stream().filter(r -> {
                KnowledgeState state = discoveryEngine.getKnowledgeState(player.getUniqueId(), r.key());
                return state != KnowledgeState.UNKNOWN;
            }).toList();
        }

        // Sort: Ready to Cook -> Discovered -> Hinted -> Unknown
        return base.stream().sorted((r1, r2) -> {
            KnowledgeState s1 = discoveryEngine.getKnowledgeState(player.getUniqueId(), r1.key());
            KnowledgeState s2 = discoveryEngine.getKnowledgeState(player.getUniqueId(), r2.key());
            int rank1 = getSortRank(player, r1, s1);
            int rank2 = getSortRank(player, r2, s2);
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
            return r1.key().compareTo(r2.key());
        }).toList();
    }

    private int getSortRank(Player player, RecipeDefinition recipe, KnowledgeState state) {
        if (state == KnowledgeState.DISCOVERED) {
            return isReadyToCook(player, recipe) ? 0 : 1;
        }
        if (state == KnowledgeState.HINTED) {
            return 2;
        }
        return 3;
    }

    public boolean isReadyToCook(Player player, RecipeDefinition recipe) {
        if (player == null || recipe == null) return false;
        for (StoredItem input : recipe.inputRequirements()) {
            if (countPlayerItem(player, input.itemKey()) < input.amount()) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesSearch(Player player, RecipeDefinition recipe, String query) {
        if (recipe.key().asString().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (recipe.displayName().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (recipe.resultItemKey().asString().toLowerCase(Locale.ROOT).contains(query)) return true;

        for (StoredItem input : recipe.inputRequirements()) {
            if (input.itemKey().asString().toLowerCase(Locale.ROOT).contains(query)) return true;
            Component name = resolveItemDisplayName(player, input.itemKey());
            String plainName = miniMessage.stripTags(miniMessage.serialize(name)).toLowerCase(Locale.ROOT);
            if (plainName.contains(query)) return true;
        }
        return false;
    }

    public void nextPage(Player player) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendiumWithSession(player, session.withPage(session.page() + 1));
    }

    public void prevPage(Player player) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendiumWithSession(player, session.withPage(Math.max(1, session.page() - 1)));
    }

    public void setCategory(Player player, Key categoryKey) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendiumWithSession(player, session.withCategory(categoryKey));
    }

    public void setSearchQuery(Player player, String query) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendiumWithSession(player, session.withSearch(query));
    }

    public void toggleReadyFilter(Player player) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendiumWithSession(player, session.withReadyToCook(!session.onlyReadyToCook()));
    }

    public void clearFilters(Player player) {
        CompendiumSession fresh = new CompendiumSession(1, RecipeCategory.ALL.getKey(), null, false, 1);
        sessions.put(player.getUniqueId(), fresh);
        openCompendiumWithSession(player, fresh);
    }

    public void backToCompendium(Player player) {
        CompendiumSession session = sessions.getOrDefault(player.getUniqueId(),
                new CompendiumSession(1, RecipeCategory.ALL.getKey()));
        openCompendiumWithSession(player, session);
    }

    public Optional<CompendiumSession> getSession(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return Optional.ofNullable(sessions.get(playerId));
    }

    public ItemStack renderRecipeCard(Player player, RecipeDefinition recipe, KnowledgeState state) {
        ItemStack stack = switch (state) {
            case DISCOVERED -> renderDiscoveredCard(player, recipe);
            case HINTED -> renderHintedCard(player, recipe);
            case UNKNOWN -> renderUnknownCard(player, recipe);
        };

        // Tag card with persistent recipe identifier for click interception
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(RECIPE_KEY, PersistentDataType.STRING, recipe.key().asString());
            stack.setItemMeta(meta);
        }
        return stack;
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

        Component recipeName = (messageService != null)
                ? messageService.resolveRecipeDisplayName(player, recipe)
                : miniMessage.deserialize(recipe.displayName());
        Component name = (messageService != null)
                ? messageService.render(player, "compendium.gui.discovered-title", "recipe", recipeName)
                        .decoration(TextDecoration.ITALIC, false)
                : miniMessage.deserialize("<gold><bold>" + recipe.displayName() + "</bold></gold>")
                        .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        meta.lore(buildDiscoveredLore(player, recipe));
        stack.setItemMeta(meta);
        return stack;
    }

    List<Component> buildDiscoveredLore(Player player, RecipeDefinition recipe) {
        List<Component> lore = new ArrayList<>();

        // Category line
        lore.add(buildCategoryLoreLine(player, recipe.categoryKey()));

        // Station line with proximity
        String stationDisplayName = resolveStationDisplayName(recipe.stationKey());
        Component stationComp = miniMessage.deserialize(stationDisplayName);
        boolean nearStation = isNearStation(player, recipe.stationKey(), stationProximityDistance);
        if (nearStation) {
            lore.add((messageService != null)
                    ? messageService.render(player, "compendium.gui.station-nearby", "station", stationComp)
                    : miniMessage.deserialize("<gray>Station: </gray>")
                            .append(stationComp)
                            .append(miniMessage.deserialize(" <green>(✔ Nearby)</green>")));
        } else {
            lore.add((messageService != null)
                    ? messageService.render(player, "compendium.gui.station-away", "station", stationComp)
                    : miniMessage.deserialize("<gray>Station: </gray>")
                            .append(stationComp)
                            .append(miniMessage.deserialize(" <gray>(✖ Not nearby)</gray>")));
        }

        // Cooking duration & Thermal bounds from Process
        if (processRegistry != null) {
            processRegistry.get(recipe.processKey()).ifPresent(proc -> {
                long durationTicks = proc.phases().stream().mapToLong(PhaseDefinition::durationTicks).sum();
                if (durationTicks > 0) {
                    double seconds = durationTicks / 20.0;
                    lore.add((messageService != null)
                            ? messageService.render(player, "compendium.gui.cooking-duration", "duration", String.format(Locale.ROOT, "%.1fs", seconds))
                            : miniMessage.deserialize("<gray>Duration: <yellow>" + String.format(Locale.ROOT, "%.1fs", seconds) + "</yellow></gray>"));
                }
            });
        }

        if (recipe.resultAmount() > 1) {
            lore.add((messageService != null)
                    ? messageService.render(player, "compendium.gui.yields", "amount", recipe.resultAmount())
                    : miniMessage.deserialize("<gray>Yields: <yellow>" + recipe.resultAmount() + "x</yellow></gray>"));
        }

        lore.add(Component.empty());
        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.required-ingredients")
                : miniMessage.deserialize("<yellow>Required Ingredients:</yellow>"));

        for (StoredItem input : recipe.inputRequirements()) {
            int available = countPlayerItem(player, input.itemKey());
            boolean satisfied = available >= input.amount();
            TextColor color = satisfied ? NamedTextColor.GREEN : NamedTextColor.RED;
            String prefix = satisfied ? "✔ " : "✖ ";

            Component itemName = resolveItemDisplayName(player, input.itemKey());
            Component ingLine = Component.text(prefix + input.amount() + "x ", color)
                    .append(itemName)
                    .append(Component.text(" (" + available + "/" + input.amount() + ")", color));
            lore.add(ingLine);
        }

        lore.add(Component.empty());
        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.status-discovered")
                : miniMessage.deserialize("<dark_green>Status: Discovered</dark_green>"));

        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.click-to-inspect")
                : miniMessage.deserialize("<yellow>▶ Click to inspect cooking steps</yellow>"));

        return lore;
    }

    public ItemStack renderHintedCard(RecipeDefinition recipe) {
        return renderHintedCard(null, recipe);
    }

    public ItemStack renderHintedCard(Player player, RecipeDefinition recipe) {
        Material material = recipe.hint().hideResultItem() ? Material.KNOWLEDGE_BOOK
                : resolveMaterial(recipe.resultItemKey());
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        Component recipeName = (messageService != null)
                ? messageService.resolveRecipeDisplayName(player, recipe)
                : miniMessage.deserialize(recipe.displayName());
        Component name = (messageService != null)
                ? messageService.render(player, "compendium.gui.hinted-title", "recipe", recipeName)
                        .decoration(TextDecoration.ITALIC, false)
                : miniMessage.deserialize("<gold>★ " + recipe.displayName() + " <italic><gray>(Hinted)</gray></italic></gold>")
                        .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        meta.lore(buildHintedLore(player, recipe));
        stack.setItemMeta(meta);
        return stack;
    }

    List<Component> buildHintedLore(Player player, RecipeDefinition recipe) {
        List<Component> lore = new ArrayList<>();
        lore.add(buildCategoryLoreLine(player, recipe.categoryKey()));
        lore.add(Component.empty());
        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.clue-title")
                : miniMessage.deserialize("<aqua><bold>Culinary Clue:</bold></aqua>"));
        String hintDesc = recipe.hint().description();
        Component hintComp = (hintDesc != null && !hintDesc.isBlank())
                ? miniMessage.deserialize(hintDesc)
                : Component.empty();
        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.clue-hint", "hint", hintComp)
                : Component.empty().color(NamedTextColor.GRAY).append(hintComp));

        if (!recipe.hint().revealedIngredientKeys().isEmpty()) {
            lore.add(Component.empty());
            lore.add((messageService != null)
                    ? messageService.render(player, "compendium.gui.clue-components")
                    : miniMessage.deserialize("<yellow>Known Components:</yellow>"));
            for (Key rev : recipe.hint().revealedIngredientKeys()) {
                Component ingName = resolveItemDisplayName(player, rev);
                lore.add(miniMessage.deserialize("<aqua>• </aqua>").append(ingName));
            }
            lore.add((messageService != null)
                    ? messageService.render(player, "compendium.gui.secret-ingredient")
                    : miniMessage.deserialize("<dark_gray>• ??? (Secret ingredients)</dark_gray>"));
        }

        return lore;
    }

    public ItemStack renderUnknownCard(RecipeDefinition recipe) {
        return renderUnknownCard(null, recipe);
    }

    public ItemStack renderUnknownCard(Player player, RecipeDefinition recipe) {
        ItemStack stack = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        Component name = (messageService != null)
                ? messageService.render(player, "compendium.gui.unknown-title")
                : miniMessage.deserialize("<gray><italic>??? Unknown Recipe</italic></gray>");
        meta.displayName(name);
        meta.lore(buildUnknownLore(player, recipe));
        stack.setItemMeta(meta);
        return stack;
    }

    List<Component> buildUnknownLore(Player player, RecipeDefinition recipe) {
        List<Component> lore = new ArrayList<>();
        lore.add(buildCategoryLoreLine(player, recipe.categoryKey()));
        lore.add(Component.empty());
        lore.add((messageService != null)
                ? messageService.render(player, "compendium.gui.unknown-experiment")
                : miniMessage.deserialize("<gray>Experiment in crafting stations to discover this dish!</gray>"));
        return lore;
    }

    private Component buildCategoryLoreLine(Player player, Key categoryKey) {
        return buildCategoryLoreLine(player, categoryKey, messageService, categoryRegistry, miniMessage);
    }

    static Component buildCategoryLoreLine(
            Player player,
            Key categoryKey,
            PaperMessageService messageService,
            RecipeCategoryRegistry categoryRegistry,
            MiniMessage miniMessage
    ) {
        Component categoryComp = resolveCategoryComponent(player, categoryKey, messageService, categoryRegistry, miniMessage);
        return (messageService != null)
                ? messageService.render(player, "compendium.gui.category", "category", categoryComp)
                : Component.text("Category: ", NamedTextColor.GRAY)
                        .append(Component.empty().color(NamedTextColor.YELLOW).append(categoryComp));
    }

    Component resolveCategoryComponent(Player player, Key categoryKey) {
        return resolveCategoryComponent(player, categoryKey, messageService, categoryRegistry, miniMessage);
    }

    static Component resolveCategoryComponent(
            Player player,
            Key categoryKey,
            PaperMessageService messageService,
            RecipeCategoryRegistry categoryRegistry,
            MiniMessage miniMessage
    ) {
        if (categoryKey == null) {
            return Component.empty();
        }
        if (messageService != null) {
            return messageService.resolveCategoryDisplayName(player, categoryKey, categoryRegistry);
        }
        if (categoryRegistry != null) {
            return categoryRegistry.get(categoryKey)
                    .map(RecipeCategory::displayName)
                    .filter(displayName -> !displayName.isBlank())
                    .map(miniMessage::deserialize)
                    .orElseGet(() -> Component.text(categoryKey.value()));
        }
        return Component.text(categoryKey.value());
    }

    public boolean isNearStation(Player player, Key stationKey, int radius) {
        if (stationManager == null || player == null || !player.isOnline()) {
            return false;
        }
        try {
            String worldName = player.getWorld().getName();
            int playerX = player.getLocation().getBlockX();
            int playerY = player.getLocation().getBlockY();
            int playerZ = player.getLocation().getBlockZ();

            int chunkX = playerX >> 4;
            int chunkZ = playerZ >> 4;

            for (int cx = chunkX - 1; cx <= chunkX + 1; cx++) {
                for (int cz = chunkZ - 1; cz <= chunkZ + 1; cz++) {
                    Set<StationInstance> stations = stationManager.getStationsInChunk(new ChunkPosition(worldName, cx, cz));
                    if (stations == null) continue;
                    for (StationInstance station : stations) {
                        if (station != null && station.getStationKey().equals(stationKey)) {
                            BlockPosition pos = station.getPosition();
                            int dx = pos.x() - playerX;
                            int dy = pos.y() - playerY;
                            int dz = pos.z() - playerZ;
                            if ((dx * dx + dy * dy + dz * dz) <= (radius * radius)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Safe fallback during mocked environments
        }
        return false;
    }

    public int countPlayerItem(Player player, Key itemKey) {
        if (player == null || player.getInventory() == null) return 0;
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

    private void highlightActiveCategory(Inventory inv, MenuDefinition menu, Key activeCategory) {
        for (int i = 0; i < 9 && i < menu.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || !stack.hasItemMeta()) continue;
            ItemMeta meta = stack.getItemMeta();

            char symbol = Character.toUpperCase(menu.getSymbolAt(i));
            boolean isCategorySlot = (symbol == 'A' || symbol == 'B' || symbol == 'S' || symbol == 'K' || symbol == 'V' || symbol == 'P');
            if (!isCategorySlot) continue;

            boolean isActive = false;
            if (symbol == 'A' && activeCategory.equals(RecipeCategory.ALL.getKey())) isActive = true;
            else if (symbol == 'B' && activeCategory.equals(RecipeCategory.BAKERY.getKey())) isActive = true;
            else if (symbol == 'S' && activeCategory.equals(RecipeCategory.SOUPS.getKey())) isActive = true;
            else if (symbol == 'K' && activeCategory.equals(RecipeCategory.COOKING.getKey())) isActive = true;
            else if (symbol == 'V' && activeCategory.equals(RecipeCategory.BEVERAGES.getKey())) isActive = true;
            else if (symbol == 'P' && activeCategory.equals(RecipeCategory.PRESERVES.getKey())) isActive = true;

            if (isActive) {
                try {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } catch (Exception ignored) {}

                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(miniMessage.deserialize("<green>✔ Currently Active Filter</green>"));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
        }
    }

    private void updateFilterButton(Inventory inv, MenuDefinition menu, boolean onlyReady) {
        for (int i = 0; i < menu.size(); i++) {
            if (Character.toUpperCase(menu.getSymbolAt(i)) == 'F') {
                ItemStack stack = inv.getItem(i);
                if (stack != null && stack.hasItemMeta()) {
                    ItemMeta meta = stack.getItemMeta();
                    if (onlyReady) {
                        try {
                            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        } catch (Exception ignored) {}
                        meta.displayName(miniMessage.deserialize("<green><bold>Ready to Cook: ON</bold></green>").decoration(TextDecoration.ITALIC, false));
                    } else {
                        meta.displayName(miniMessage.deserialize("<gray><bold>Ready to Cook: OFF</bold></gray>").decoration(TextDecoration.ITALIC, false));
                    }
                    stack.setItemMeta(meta);
                }
                break;
            }
        }
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

    private String resolveStationDisplayName(Key stationKey) {
        if (stationRegistry != null) {
            Optional<StationDefinition> def = stationRegistry.get(stationKey);
            if (def.isPresent() && def.get().displayName() != null) {
                return def.get().displayName();
            }
        }
        return stationKey.value();
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
