package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.core.i18n.DefaultMessageCatalog;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.menu.DefaultMenuRegistry;
import dev.darkblade.croppy.core.menu.YamlMenuLoader;
import dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CompendiumMenuControllerTest {

    private DefaultMenuRegistry menuRegistry;
    private DefaultRecipeRegistry recipeRegistry;
    private RecipeDiscoveryEngine discoveryEngine;
    private PaperMenuRenderer mockRenderer;
    private ItemFactory itemFactory;
    private CompendiumMenuController controller;

    private Player mockPlayer;
    private UUID playerUuid;
    private Map<Integer, ItemStack> inventorySlots;
    private AtomicBoolean inventoryOpened;
    private Map<Key, KnowledgeState> playerKnowledge;

    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestItemStack extends ItemStack {
    }

    @BeforeEach
    void setUp() {
        menuRegistry = new DefaultMenuRegistry();
        recipeRegistry = new DefaultRecipeRegistry();
        itemFactory = new ItemFactory(null, new DefaultItemRegistry());

        inventorySlots = new HashMap<>();
        inventoryOpened = new AtomicBoolean(false);
        playerKnowledge = new HashMap<>();

        Inventory mockInventory = (Inventory) Proxy.newProxyInstance(
                Inventory.class.getClassLoader(),
                new Class<?>[]{Inventory.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setItem".equals(name)) {
                        int slot = (int) args[0];
                        ItemStack item = (ItemStack) args[1];
                        inventorySlots.put(slot, item);
                        return null;
                    } else if ("getItem".equals(name)) {
                        int slot = (int) args[0];
                        return inventorySlots.get(slot);
                    }
                    return null;
                }
        );

        mockRenderer = new PaperMenuRenderer(itemFactory) {
            @Override
            public Inventory createInventory(PaperMenuHolder holder) {
                holder.setInventory(mockInventory);
                return mockInventory;
            }
        };

        playerUuid = UUID.randomUUID();

        PlayerInventory mockPlayerInventory = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if ("getContents".equals(method.getName())) {
                        return new ItemStack[0];
                    }
                    return null;
                }
        );

        mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getUniqueId".equals(name)) {
                        return playerUuid;
                    } else if ("getInventory".equals(name)) {
                        return mockPlayerInventory;
                    } else if ("openInventory".equals(name)) {
                        inventoryOpened.set(true);
                        return null;
                    } else if ("locale".equals(name)) {
                        return Locale.US;
                    } else if ("sendMessage".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        discoveryEngine = (RecipeDiscoveryEngine) Proxy.newProxyInstance(
                RecipeDiscoveryEngine.class.getClassLoader(),
                new Class<?>[]{RecipeDiscoveryEngine.class},
                (proxy, method, args) -> {
                    if ("getKnowledgeState".equals(method.getName())) {
                        Key key = (Key) args[1];
                        return playerKnowledge.getOrDefault(key, KnowledgeState.UNKNOWN);
                    }
                    return null;
                }
        );

        controller = new CompendiumMenuController(menuRegistry, recipeRegistry, discoveryEngine, mockRenderer, itemFactory) {
            @Override
            public ItemStack renderRecipeCard(Player player, RecipeDefinition recipe, KnowledgeState state) {
                try {
                    return (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // Load bundled recipes_menu.yml
        YamlMenuLoader loader = new YamlMenuLoader();
        InputStream recStream = getClass().getClassLoader().getResourceAsStream("menus/recipes_menu.yml");
        assertNotNull(recStream, "menus/recipes_menu.yml resource must exist");
        MenuDefinition recMenu = loader.load(recStream, "recipes_menu.yml");
        menuRegistry.register(recMenu);
    }

    @Test
    @DisplayName("openCompendium successfully opens with empty recipe registry without throwing IndexOutOfBoundsException")
    void testOpenCompendiumWithEmptyRecipes() {
        assertDoesNotThrow(() -> controller.openCompendium(mockPlayer, RecipeCategory.ALL.getKey(), 1));
        assertTrue(inventoryOpened.get());

        // All 28 'R' slots should have been cleared to null
        assertEquals(28, inventorySlots.size());
        for (ItemStack item : inventorySlots.values()) {
            assertNull(item);
        }
    }

    @Test
    @DisplayName("openCompendium populates recipe slots when recipes exist")
    void testOpenCompendiumWithRecipes() {
        RecipeDefinition testRecipe = new RecipeDefinition(
                Key.of("croppy", "test_bread"),
                "Artisan Bread",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "skillet"),
                Key.of("croppy", "bake_bread"),
                List.of(StoredItem.of(Key.of("minecraft", "wheat"), 3)),
                Key.of("minecraft", "bread"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        recipeRegistry.register(testRecipe);

        assertDoesNotThrow(() -> controller.openCompendium(mockPlayer, RecipeCategory.ALL.getKey(), 1));
        assertTrue(inventoryOpened.get());

        // Slot 10 (row 1, col 1) corresponds to first 'R' slot
        int firstRSlot = 1 * 9 + 1; // 10
        ItemStack card = inventorySlots.get(firstRSlot);
        assertNotNull(card, "First recipe slot should be populated with card");
    }

    @Test
    @DisplayName("Compendium pagination transitions pages properly")
    void testCompendiumPagination() {
        // Register 30 recipes (pageSize = 28, maxPages = 2)
        for (int i = 0; i < 30; i++) {
            recipeRegistry.register(new RecipeDefinition(
                    Key.of("croppy", "recipe_" + i),
                    "Recipe " + i,
                    RecipeCategory.ALL.getKey(),
                    Key.of("croppy", "skillet"),
                    Key.of("croppy", "proc_" + i),
                    List.of(),
                    Key.of("minecraft", "bread"),
                    1,
                    HintDefinition.DEFAULT,
                    false,
                    List.of(),
                    Map.of()
            ));
        }

        controller.openCompendium(mockPlayer, RecipeCategory.ALL.getKey(), 1);
        CompendiumMenuController.CompendiumSession s1 = controller.getSession(mockPlayer.getUniqueId()).orElse(null);
        assertNotNull(s1);
        assertEquals(1, s1.page());

        controller.nextPage(mockPlayer);
        CompendiumMenuController.CompendiumSession s2 = controller.getSession(mockPlayer.getUniqueId()).orElse(null);
        assertNotNull(s2);
        assertEquals(2, s2.page());

        controller.prevPage(mockPlayer);
        CompendiumMenuController.CompendiumSession s3 = controller.getSession(mockPlayer.getUniqueId()).orElse(null);
        assertNotNull(s3);
        assertEquals(1, s3.page());
    }

    @Test
    @DisplayName("openCompendium handles layout without recipe slots gracefully")
    void testOpenCompendiumWithNoRecipeSlotsInLayout() {
        DefaultMenuRegistry emptyMenuReg = new DefaultMenuRegistry();
        MenuDefinition noRMenu = new MenuDefinition(
                CompendiumMenuController.MENU_KEY,
                "No R",
                1,
                List.of("XXXXXXXXX"),
                Map.of('X', SlotDefinition.decoration(null)),
                Map.of()
        );
        emptyMenuReg.register(noRMenu);

        CompendiumMenuController emptyController = new CompendiumMenuController(
                emptyMenuReg, recipeRegistry, discoveryEngine, mockRenderer, itemFactory
        );

        // Should return early and not crash
        assertDoesNotThrow(() -> emptyController.openCompendium(mockPlayer, RecipeCategory.ALL.getKey(), 1));
        assertFalse(inventoryOpened.get());
    }

    @Test
    @DisplayName("Search query filters recipes by display name, result item, or ingredients")
    void testFilterBySearchQuery() {
        RecipeDefinition bread = new RecipeDefinition(
                Key.of("croppy", "artisan_bread"),
                "Artisan Bread",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "oven"),
                Key.of("croppy", "bake_bread"),
                List.of(StoredItem.of(Key.of("minecraft", "wheat"), 3)),
                Key.of("minecraft", "bread"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        RecipeDefinition soup = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "pot"),
                Key.of("croppy", "simmer_soup"),
                List.of(StoredItem.of(Key.of("croppy", "tomato"), 2)),
                Key.of("croppy", "canned_soup"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        recipeRegistry.register(bread);
        recipeRegistry.register(soup);

        // Search by name
        CompendiumMenuController.CompendiumSession nameSearch = new CompendiumMenuController.CompendiumSession(
                1, RecipeCategory.ALL.getKey(), "tomato", false, 0
        );
        List<RecipeDefinition> filteredByName = controller.getFilteredRecipes(mockPlayer, nameSearch);
        assertEquals(1, filteredByName.size());
        assertEquals("Tomato Soup", filteredByName.getFirst().displayName());

        // Search by result item key
        CompendiumMenuController.CompendiumSession resultSearch = new CompendiumMenuController.CompendiumSession(
                1, RecipeCategory.ALL.getKey(), "bread", false, 0
        );
        List<RecipeDefinition> filteredByResult = controller.getFilteredRecipes(mockPlayer, resultSearch);
        assertEquals(1, filteredByResult.size());
        assertEquals("Artisan Bread", filteredByResult.getFirst().displayName());

        // Search by input ingredient key
        CompendiumMenuController.CompendiumSession inputSearch = new CompendiumMenuController.CompendiumSession(
                1, RecipeCategory.ALL.getKey(), "wheat", false, 0
        );
        List<RecipeDefinition> filteredByInput = controller.getFilteredRecipes(mockPlayer, inputSearch);
        assertEquals(1, filteredByInput.size());
        assertEquals("Artisan Bread", filteredByInput.getFirst().displayName());
    }

    @Test
    @DisplayName("Ready-to-cook filter displays only recipes with satisfied inventory items")
    void testFilterByReadyToCook() {
        RecipeDefinition freeRecipe = new RecipeDefinition(
                Key.of("croppy", "free_snack"),
                "Free Snack",
                RecipeCategory.COOKING.getKey(),
                Key.of("croppy", "skillet"),
                Key.of("croppy", "snack_proc"),
                List.of(), // No inputs needed
                Key.of("minecraft", "apple"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        RecipeDefinition expensiveRecipe = new RecipeDefinition(
                Key.of("croppy", "expensive_feast"),
                "Expensive Feast",
                RecipeCategory.COOKING.getKey(),
                Key.of("croppy", "skillet"),
                Key.of("croppy", "feast_proc"),
                List.of(StoredItem.of(Key.of("minecraft", "diamond"), 5)),
                Key.of("minecraft", "cake"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        recipeRegistry.register(freeRecipe);
        recipeRegistry.register(expensiveRecipe);
        playerKnowledge.put(freeRecipe.key(), KnowledgeState.DISCOVERED);
        playerKnowledge.put(expensiveRecipe.key(), KnowledgeState.DISCOVERED);

        assertTrue(controller.isReadyToCook(mockPlayer, freeRecipe));
        assertFalse(controller.isReadyToCook(mockPlayer, expensiveRecipe));

        CompendiumMenuController.CompendiumSession readyOnlySession = new CompendiumMenuController.CompendiumSession(
                1, RecipeCategory.ALL.getKey(), null, true, 0
        );
        List<RecipeDefinition> readyRecipes = controller.getFilteredRecipes(mockPlayer, readyOnlySession);
        assertEquals(1, readyRecipes.size());
        assertEquals("Free Snack", readyRecipes.getFirst().displayName());
    }

    @Test
    @DisplayName("showUnknownRecipes setting toggles visibility of undiscovered recipes")
    void testFilterByUnknownVisibility() {
        RecipeDefinition unknownRecipe = new RecipeDefinition(
                Key.of("croppy", "secret_dish"),
                "Secret Dish",
                RecipeCategory.COOKING.getKey(),
                Key.of("croppy", "skillet"),
                Key.of("croppy", "secret_proc"),
                List.of(),
                Key.of("minecraft", "cookie"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );
        recipeRegistry.register(unknownRecipe);

        CompendiumMenuController.CompendiumSession session = new CompendiumMenuController.CompendiumSession(
                1, RecipeCategory.ALL.getKey(), null, false, 0
        );

        controller.setShowUnknownRecipes(true);
        List<RecipeDefinition> withUnknown = controller.getFilteredRecipes(mockPlayer, session);
        assertEquals(1, withUnknown.size());

        controller.setShowUnknownRecipes(false);
        List<RecipeDefinition> withoutUnknown = controller.getFilteredRecipes(mockPlayer, session);
        assertTrue(withoutUnknown.isEmpty(), "Unknown recipe should be hidden when showUnknownRecipes is false");
    }

    @Test
    @DisplayName("CompendiumSession immutable mutators preserve state correctly")
    void testCompendiumSessionMutators() {
        CompendiumMenuController.CompendiumSession initial = new CompendiumMenuController.CompendiumSession(
                2, RecipeCategory.BAKERY.getKey(), "pie", true, 1
        );

        // withCategory resets page to 1
        CompendiumMenuController.CompendiumSession catChanged = initial.withCategory(RecipeCategory.SOUPS.getKey());
        assertEquals(1, catChanged.page());
        assertEquals(RecipeCategory.SOUPS.getKey(), catChanged.categoryKey());
        assertEquals("pie", catChanged.searchQuery());
        assertTrue(catChanged.onlyReadyToCook());
        assertEquals(1, catChanged.categoryPage());

        // withPage updates page
        CompendiumMenuController.CompendiumSession pageChanged = initial.withPage(5);
        assertEquals(5, pageChanged.page());
        assertEquals(RecipeCategory.BAKERY.getKey(), pageChanged.categoryKey());

        // withSearch resets page to 1
        CompendiumMenuController.CompendiumSession searchChanged = initial.withSearch("bread");
        assertEquals(1, searchChanged.page());
        assertEquals("bread", searchChanged.searchQuery());

        // withReadyToCook resets page to 1
        CompendiumMenuController.CompendiumSession readyChanged = initial.withReadyToCook(false);
        assertEquals(1, readyChanged.page());
        assertFalse(readyChanged.onlyReadyToCook());

        // withCategoryPage updates category page
        CompendiumMenuController.CompendiumSession catPageChanged = initial.withCategoryPage(2);
        assertEquals(2, catPageChanged.categoryPage());
    }

    @Test
    @DisplayName("DISCOVERED, HINTED, and UNKNOWN lore render MiniMessage category and hint tags cleanly")
    void testLoreRendersMiniMessageCategoryAndHintWithMessageService() {
        DefaultMessageCatalog catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "compendium.gui.category", "<gray>Category: <yellow><category></yellow></gray>",
                        "compendium.gui.station-away", "<gray>Station: <station> <gray>(✖ Not nearby)</gray></gray>",
                        "compendium.gui.required-ingredients", "<yellow>Required Ingredients:</yellow>",
                        "compendium.gui.status-discovered", "<dark_green>Status: Discovered</dark_green>",
                        "compendium.gui.click-to-inspect", "<yellow>▶ Click to inspect cooking steps</yellow>",
                        "compendium.gui.clue-title", "<aqua><bold>Culinary Clue:</bold></aqua>",
                        "compendium.gui.clue-hint", "<gray><hint></gray>",
                        "compendium.gui.unknown-experiment", "<gray>Experiment in crafting stations to discover this dish!</gray>"
                )
        ));
        PaperMessageService messageService = new PaperMessageService(catalog, NoOpCroppyLogger.INSTANCE);
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();

        CompendiumMenuController localizedController = new CompendiumMenuController(
                menuRegistry, recipeRegistry, discoveryEngine, mockRenderer, itemFactory,
                messageService, categoryRegistry, null, null, null, true, 5
        );

        RecipeDefinition soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "simmer_soup"),
                List.of(),
                Key.of("minecraft", "beetroot_soup"),
                1,
                new HintDefinition("<gray>Simmer <aqua>tomatoes</aqua> cleanly</gray>", List.of(), true, Map.of()),
                false,
                List.of(),
                Map.of()
        );

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        MiniMessage mm = MiniMessage.miniMessage();

        List<Component> discoveredLore = localizedController.buildDiscoveredLore(mockPlayer, soupRecipe);
        assertEquals("Category: Soups & Stews", plain.serialize(discoveredLore.getFirst()));
        String discoveredCategoryMini = mm.serialize(discoveredLore.getFirst());
        assertFalse(discoveredCategoryMini.contains("\\<aqua>"), "Discovered category escaped MiniMessage tags: " + discoveredCategoryMini);
        assertTrue(discoveredCategoryMini.contains("<aqua>Soups & Stews"), "Discovered category missing aqua style: " + discoveredCategoryMini);

        List<Component> hintedLore = localizedController.buildHintedLore(mockPlayer, soupRecipe);
        assertEquals("Category: Soups & Stews", plain.serialize(hintedLore.getFirst()));
        assertTrue(mm.serialize(hintedLore.getFirst()).contains("<aqua>Soups & Stews"));
        assertEquals("Simmer tomatoes cleanly", plain.serialize(hintedLore.get(3)));
        String hintMini = mm.serialize(hintedLore.get(3));
        assertFalse(hintMini.contains("\\<aqua>") || hintMini.contains("\\<gray>"), "Hint escaped MiniMessage tags: " + hintMini);
        assertTrue(hintMini.contains("<aqua>tomatoes"), "Hint should preserve inner aqua style: " + hintMini);

        List<Component> unknownLore = localizedController.buildUnknownLore(mockPlayer, soupRecipe);
        assertEquals("Category: Soups & Stews", plain.serialize(unknownLore.getFirst()));
        assertTrue(mm.serialize(unknownLore.getFirst()).contains("<aqua>Soups & Stews"));
    }

    @Test
    @DisplayName("Null messageService and unregistered category keys fall back cleanly without raw tag leakage")
    void testLoreFallbacksForNullMessageServiceAndUnregisteredCategory() {
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();
        CompendiumMenuController fallbackController = new CompendiumMenuController(
                menuRegistry, recipeRegistry, discoveryEngine, mockRenderer, itemFactory,
                null, categoryRegistry, null, null, null, true, 5
        );

        RecipeDefinition soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "simmer_soup"),
                List.of(),
                Key.of("minecraft", "beetroot_soup"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        RecipeDefinition customCategoryRecipe = new RecipeDefinition(
                Key.of("croppy", "mystery_tart"),
                "Mystery Tart",
                Key.of("custom", "artisan_tarts"),
                Key.of("croppy", "oven"),
                Key.of("croppy", "bake_tart"),
                List.of(),
                Key.of("minecraft", "pumpkin_pie"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        MiniMessage mm = MiniMessage.miniMessage();

        // Registered category under null messageService
        Component discCat = fallbackController.buildDiscoveredLore(mockPlayer, soupRecipe).getFirst();
        assertEquals("Category: Soups & Stews", plain.serialize(discCat));
        assertTrue(mm.serialize(discCat).contains("<aqua>Soups & Stews"));

        List<Component> hintedLore = fallbackController.buildHintedLore(mockPlayer, soupRecipe);
        assertEquals("Category: Soups & Stews", plain.serialize(hintedLore.getFirst()));
        assertTrue(mm.serialize(hintedLore.getFirst()).contains("<aqua>Soups & Stews"));
        assertEquals("A culinary mystery waiting to be unraveled...", plain.serialize(hintedLore.get(3)));
        assertFalse(plain.serialize(hintedLore.get(3)).contains("<gray>"));

        Component unknownCat = fallbackController.buildUnknownLore(mockPlayer, soupRecipe).getFirst();
        assertEquals("Category: Soups & Stews", plain.serialize(unknownCat));
        assertTrue(mm.serialize(unknownCat).contains("<aqua>Soups & Stews"));

        // Unregistered category falls back to categoryKey.value()
        assertEquals("Category: artisan_tarts", plain.serialize(fallbackController.buildDiscoveredLore(mockPlayer, customCategoryRecipe).getFirst()));
        assertEquals("Category: artisan_tarts", plain.serialize(fallbackController.buildHintedLore(mockPlayer, customCategoryRecipe).getFirst()));
        assertEquals("Category: artisan_tarts", plain.serialize(fallbackController.buildUnknownLore(mockPlayer, customCategoryRecipe).getFirst()));
    }

    @Test
    @DisplayName("openCompendium passes resolved MiniMessage category display name in menu evaluation context")
    void testOpenCompendiumResolvesCategoryContext() {
        DefaultMessageCatalog catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);
        catalog.setCatalogs(Map.of(LocaleRef.EN_US, Map.of()));
        PaperMessageService messageService = new PaperMessageService(catalog, NoOpCroppyLogger.INSTANCE);
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();

        final dev.darkblade.croppy.api.expression.EvaluationContext[] capturedContext = new dev.darkblade.croppy.api.expression.EvaluationContext[1];
        PaperMenuRenderer capturingRenderer = new PaperMenuRenderer(itemFactory) {
            @Override
            public Inventory createInventory(PaperMenuHolder holder) {
                capturedContext[0] = holder.getContext();
                return mockRenderer.createInventory(holder);
            }
        };

        CompendiumMenuController capturingController = new CompendiumMenuController(
                menuRegistry, recipeRegistry, discoveryEngine, capturingRenderer, itemFactory,
                messageService, categoryRegistry, null, null, null, true, 5
        ) {
            @Override
            public ItemStack renderRecipeCard(Player player, RecipeDefinition recipe, KnowledgeState state) {
                try {
                    return (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        capturingController.openCompendium(mockPlayer, RecipeCategory.SOUPS.getKey(), 1);
        assertNotNull(capturedContext[0]);
        assertEquals("<aqua>Soups & Stews", capturedContext[0].getVariable("category").orElse(null));
        String renderedLine = PaperMenuRenderer.replacePlaceholders("- <gray>Category: <yellow>{category}</yellow></gray>", capturedContext[0]);
        Component renderedComp = MiniMessage.miniMessage().deserialize(renderedLine);
        assertEquals("- Category: Soups & Stews", PlainTextComponentSerializer.plainText().serialize(renderedComp));
        assertTrue(MiniMessage.miniMessage().serialize(renderedComp).contains("<aqua>Soups & Stews"));
    }
}
