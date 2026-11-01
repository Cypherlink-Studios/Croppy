package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.menu.DefaultMenuRegistry;
import dev.darkblade.croppy.core.menu.YamlMenuLoader;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
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
                        return KnowledgeState.UNKNOWN;
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
}
