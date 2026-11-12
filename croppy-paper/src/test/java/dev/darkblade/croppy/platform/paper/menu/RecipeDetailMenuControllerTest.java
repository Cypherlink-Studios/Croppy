package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.process.*;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.core.i18n.DefaultMessageCatalog;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.menu.DefaultMenuRegistry;
import dev.darkblade.croppy.core.menu.YamlMenuLoader;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry;
import dev.darkblade.croppy.core.station.DefaultStationRegistry;
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

class RecipeDetailMenuControllerTest {

    private DefaultMenuRegistry menuRegistry;
    private DefaultProcessRegistry processRegistry;
    private DefaultStationRegistry stationRegistry;
    private ItemFactory itemFactory;
    private PaperMenuRenderer mockRenderer;
    private RecipeDetailMenuController controller;

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
        processRegistry = new DefaultProcessRegistry();
        stationRegistry = new DefaultStationRegistry();
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
                    } else if ("locale".equals(name)) {
                        return Locale.US;
                    } else if ("sendMessage".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        controller = new RecipeDetailMenuController(
                menuRegistry, processRegistry, stationRegistry, itemFactory, mockRenderer, null, null, 5
        ) {
            @Override
            protected ItemStack renderResultCard(Player player, RecipeDefinition recipe) {
                return allocateMockItem();
            }

            @Override
            protected ItemStack renderStationCard(Player player, Key stationKey, String stationName) {
                return allocateMockItem();
            }

            @Override
            protected ItemStack renderIngredientCard(Player player, StoredItem input) {
                return allocateMockItem();
            }

            @Override
            protected ItemStack renderPhaseCard(Player player, int stepNumber, PhaseDefinition phase) {
                return allocateMockItem();
            }
        };

        // Load bundled recipe_detail_menu.yml
        YamlMenuLoader loader = new YamlMenuLoader();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("menus/recipe_detail_menu.yml");
        assertNotNull(stream, "menus/recipe_detail_menu.yml resource must exist");
        MenuDefinition detailMenu = loader.load(stream, "recipe_detail_menu.yml");
        menuRegistry.register(detailMenu);
    }

    private static ItemStack allocateMockItem() {
        try {
            return (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("openDetail successfully populates result, station, ingredients, and sequential phases")
    void testOpenDetailSuccessfullyPopulatesSlots() {
        // 1. Register station
        Key stationKey = Key.of("croppy", "clay_oven");
        stationRegistry.register(new StationDefinition(
                stationKey,
                "Clay Oven",
                "minecraft:furnace",
                StationInteractionType.HYBRID,
                4,
                2,
                true,
                List.of(),
                Map.of()
        ));

        // 2. Register a process with 3 phases
        Key processKey = Key.of("croppy", "bake_bread_process");
        PhaseDefinition phase1 = PhaseDefinition.interactive(
                "kneading", 40L, Key.of("croppy", "rolling_pin"), 1, "Right-click with rolling pin"
        );
        PhaseDefinition phase2 = PhaseDefinition.timed("baking", 100L, Expression.ALWAYS_TRUE);
        PhaseDefinition phase3 = PhaseDefinition.environmental("cooling", Expression.ALWAYS_TRUE);
        processRegistry.register(new ProcessDefinition(
                processKey,
                "Bake Bread Process",
                stationKey,
                List.of(),
                Expression.ALWAYS_TRUE,
                List.of(phase1, phase2, phase3),
                null,
                Key.of("minecraft", "bread"),
                1,
                Map.of()
        ));

        // 3. Create recipe with 2 input ingredients
        RecipeDefinition recipe = new RecipeDefinition(
                Key.of("croppy", "artisan_bread"),
                "Artisan Bread",
                RecipeCategory.BAKERY.getKey(),
                stationKey,
                processKey,
                List.of(
                        StoredItem.of(Key.of("croppy", "flour"), 2),
                        StoredItem.of(Key.of("minecraft", "water_bucket"), 1)
                ),
                Key.of("minecraft", "bread"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        assertDoesNotThrow(() -> controller.openDetail(mockPlayer, recipe));
        assertTrue(inventoryOpened.get(), "Player inventory should have been opened");

        // Result slot: Row 0, Col 4 -> 4
        assertNotNull(inventorySlots.get(4), "Result card slot 'R' should be populated");

        // Station slot: Row 0, Col 8 -> 8
        assertNotNull(inventorySlots.get(8), "Station slot 'S' should be populated");

        // Ingredient slots: Row 1 has 'I' at cols 1-7 (slots 10-16)
        assertNotNull(inventorySlots.get(10), "First ingredient slot should be populated");
        assertNotNull(inventorySlots.get(11), "Second ingredient slot should be populated");
        assertNull(inventorySlots.get(12), "Unused ingredient slot 12 should be cleared to null");
        assertNull(inventorySlots.get(13), "Unused ingredient slot 13 should be cleared to null");

        // Phase sequence slots: Row 3 has '1'-'7' at cols 1-7 (slots 28-34)
        assertNotNull(inventorySlots.get(28), "Phase 1 slot should be populated");
        assertNotNull(inventorySlots.get(29), "Phase 2 slot should be populated");
        assertNotNull(inventorySlots.get(30), "Phase 3 slot should be populated");
        assertNull(inventorySlots.get(31), "Unused phase 4 slot should be cleared to null");
        assertNull(inventorySlots.get(32), "Unused phase 5 slot should be cleared to null");
    }

    @Test
    @DisplayName("openDetail handles missing menu layout gracefully without throwing")
    void testOpenDetailMissingMenuConfiguration() {
        DefaultMenuRegistry emptyMenuReg = new DefaultMenuRegistry();
        RecipeDetailMenuController emptyController = new RecipeDetailMenuController(
                emptyMenuReg, processRegistry, stationRegistry, itemFactory, mockRenderer, null, null, 5
        );

        RecipeDefinition dummyRecipe = new RecipeDefinition(
                Key.of("croppy", "dummy"),
                "Dummy",
                RecipeCategory.ALL.getKey(),
                Key.of("croppy", "station"),
                Key.of("croppy", "proc"),
                List.of(),
                Key.of("minecraft", "apple"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        assertDoesNotThrow(() -> emptyController.openDetail(mockPlayer, dummyRecipe));
        assertFalse(inventoryOpened.get(), "Inventory should not open when menu config is missing");
    }

    @Test
    @DisplayName("Station display name resolution falls back to formatted key if unmapped")
    void testStationDisplayNameResolution() {
        // When station exists in stationRegistry
        stationRegistry.register(new StationDefinition(
                Key.of("croppy", "skillet"),
                "Cast Iron Skillet",
                "minecraft:iron_block",
                StationInteractionType.HYBRID,
                4,
                2,
                true,
                List.of(),
                Map.of()
        ));
        assertEquals("Cast Iron Skillet", controller.resolveStationDisplayName(Key.of("croppy", "skillet")));

        // When station does not exist, capitalizes value
        assertEquals("Coffee Roaster", controller.resolveStationDisplayName(Key.of("croppy", "coffee_roaster")));
    }

    @Test
    @DisplayName("buildResultLore renders localized yields and parsed MiniMessage category display names")
    void testBuildResultLoreLocalizedCategoryAndYields() {
        DefaultMessageCatalog catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of(
                        "compendium.gui.yields", "<gray>Yields: <yellow><amount>x</yellow></gray>",
                        "compendium.gui.category", "<gray>Category: <yellow><category></yellow></gray>"
                ),
                LocaleRef.ES_ES, Map.of(
                        "compendium.gui.yields", "<gray>Produce: <yellow><amount>x</yellow></gray>",
                        "compendium.gui.category", "<gray>Categoría: <yellow><category></yellow></gray>",
                        "content.category.croppy.soups.name", "<aqua>Sopas y Estofados</aqua>"
                )
        ));
        PaperMessageService messageService = new PaperMessageService(catalog, NoOpCroppyLogger.INSTANCE);
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();

        RecipeDetailMenuController localizedController = new RecipeDetailMenuController(
                menuRegistry, processRegistry, stationRegistry, itemFactory, mockRenderer,
                messageService, null, 5, categoryRegistry
        );

        RecipeDefinition soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "simmer_soup"),
                List.of(),
                Key.of("minecraft", "beetroot_soup"),
                2,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        MiniMessage mm = MiniMessage.miniMessage();

        List<Component> enLore = localizedController.buildResultLore(mockPlayer, soupRecipe);
        assertEquals(2, enLore.size());
        assertEquals("Yields: 2x", plain.serialize(enLore.get(0)));
        assertEquals("Category: Soups & Stews", plain.serialize(enLore.get(1)));
        String categoryMini = mm.serialize(enLore.get(1));
        assertFalse(categoryMini.contains("\\<aqua>"), "Category should not escape MiniMessage tags: " + categoryMini);
        assertTrue(categoryMini.contains("<aqua>Soups & Stews"), "Category should preserve aqua styling: " + categoryMini);

        Player spanishPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> "locale".equals(method.getName()) ? Locale.forLanguageTag("es-ES") : null
        );
        List<Component> esLore = localizedController.buildResultLore(spanishPlayer, soupRecipe);
        assertEquals("Produce: 2x", plain.serialize(esLore.get(0)));
        assertEquals("Categoría: Sopas y Estofados", plain.serialize(esLore.get(1)));
        assertTrue(mm.serialize(esLore.get(1)).contains("<aqua>Sopas y Estofados"));
    }

    @Test
    @DisplayName("buildResultLore handles unregistered categories, null messageService, and 8-arg constructor fallback")
    void testBuildResultLoreFallbacksForNullMessageServiceAndUnregisteredCategory() {
        RecipeCategoryRegistry categoryRegistry = new DefaultRecipeCategoryRegistry();
        RecipeDetailMenuController nullMsgController = new RecipeDetailMenuController(
                menuRegistry, processRegistry, stationRegistry, itemFactory, mockRenderer,
                null, null, 5, categoryRegistry
        );

        RecipeDefinition soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "simmer_soup"),
                List.of(),
                Key.of("minecraft", "beetroot_soup"),
                3,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        RecipeDefinition customCategoryRecipe = new RecipeDefinition(
                Key.of("croppy", "street_taco"),
                "Street Taco",
                Key.of("custom", "street_food"),
                Key.of("croppy", "grill"),
                Key.of("croppy", "grill_proc"),
                List.of(),
                Key.of("minecraft", "cooked_beef"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        );

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        MiniMessage mm = MiniMessage.miniMessage();

        List<Component> nullMsgSoupLore = nullMsgController.buildResultLore(mockPlayer, soupRecipe);
        assertEquals("Yields: 3x", plain.serialize(nullMsgSoupLore.get(0)));
        assertEquals("Category: Soups & Stews", plain.serialize(nullMsgSoupLore.get(1)));
        assertTrue(mm.serialize(nullMsgSoupLore.get(1)).contains("<aqua>Soups & Stews"));

        List<Component> customLore = nullMsgController.buildResultLore(mockPlayer, customCategoryRecipe);
        assertEquals("Yields: 1x", plain.serialize(customLore.get(0)));
        assertEquals("Category: street_food", plain.serialize(customLore.get(1)));

        // Legacy 8-argument constructor (null categoryRegistry)
        List<Component> legacyLore = controller.buildResultLore(mockPlayer, soupRecipe);
        assertEquals("Yields: 3x", plain.serialize(legacyLore.get(0)));
        assertEquals("Category: soups", plain.serialize(legacyLore.get(1)));
    }
}
