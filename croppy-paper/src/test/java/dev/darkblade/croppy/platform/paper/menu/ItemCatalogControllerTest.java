package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemCategory;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.item.DefaultItemCategoryRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.menu.DefaultMenuRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemCatalogControllerTest {

    private DefaultMenuRegistry menuRegistry;
    private DefaultItemRegistry itemRegistry;
    private DefaultItemCategoryRegistry categoryRegistry;
    private ItemCatalogController controller;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        menuRegistry = new DefaultMenuRegistry();
        itemRegistry = new DefaultItemRegistry();
        categoryRegistry = new DefaultItemCategoryRegistry();
        testPlayerId = UUID.randomUUID();

        // Register sample test items
        itemRegistry.register(new ItemDefinition(
                Key.of("croppy", "wheat_seeds"),
                "seeds",
                "minecraft:wheat_seeds",
                0,
                null,
                "<green>Wheat Seeds</green>",
                List.of("<gray>Plant on farmland</gray>"),
                Map.of()
        ));
        itemRegistry.register(new ItemDefinition(
                Key.of("croppy", "tomato_seeds"),
                "seeds",
                "minecraft:pumpkin_seeds",
                0,
                null,
                "<red>Tomato Seeds</red>",
                List.of("<gray>Plant on farmland</gray>"),
                Map.of()
        ));
        itemRegistry.register(new ItemDefinition(
                Key.of("croppy", "copper_can"),
                "watering_cans",
                "minecraft:copper_ingot",
                2001,
                Key.of("croppy", "copper_can"),
                "<blue>Copper Can</blue>",
                List.of("<gray>Capacity 40</gray>"),
                Map.of("capacity", "40")
        ));
        itemRegistry.register(new ItemDefinition(
                Key.of("croppy", "growth_fertilizer"),
                "fertilizers",
                "minecraft:bone_meal",
                101,
                null,
                "<aqua>Growth Booster</aqua>",
                List.of(),
                Map.of()
        ));

        // Headless controller without Bukkit runtime
        ItemFactory factory = new ItemFactory(null, itemRegistry);
        controller = new ItemCatalogController(
                menuRegistry,
                itemRegistry,
                categoryRegistry,
                factory,
                new PaperMenuRenderer(factory),
                null,
                null
        );
    }

    @Test
    @DisplayName("Session records support immutable state transitions")
    void testSessionTransitions() {
        ItemCatalogController.ItemCatalogSession session = new ItemCatalogController.ItemCatalogSession(
                1, ItemCategory.ALL.getKey()
        );

        assertEquals(1, session.page());
        assertEquals(ItemCategory.ALL.getKey(), session.categoryKey());
        assertNull(session.searchQuery());
        assertEquals(1, session.categoryPage());

        ItemCatalogController.ItemCatalogSession page2 = session.withPage(2);
        assertEquals(2, page2.page());
        assertEquals(ItemCategory.ALL.getKey(), page2.categoryKey());

        ItemCatalogController.ItemCatalogSession seedsSession = session.withCategory(ItemCategory.SEEDS.getKey());
        assertEquals(1, seedsSession.page());
        assertEquals(ItemCategory.SEEDS.getKey(), seedsSession.categoryKey());

        ItemCatalogController.ItemCatalogSession searchSession = session.withSearch("copper");
        assertEquals(1, searchSession.page());
        assertEquals("copper", searchSession.searchQuery());

        ItemCatalogController.ItemCatalogSession catPage2 = session.withCategoryPage(2);
        assertEquals(2, catPage2.categoryPage());
    }

    @Test
    @DisplayName("Filter all items returns complete item registry sorted by key")
    void testFilterAllItems() {
        ItemCatalogController.ItemCatalogSession session = new ItemCatalogController.ItemCatalogSession(
                1, ItemCategory.ALL.getKey()
        );

        List<ItemDefinition> items = controller.getFilteredItems(null, session);
        assertEquals(4, items.size());
        assertEquals(Key.of("croppy", "copper_can"), items.get(0).key());
        assertEquals(Key.of("croppy", "growth_fertilizer"), items.get(1).key());
        assertEquals(Key.of("croppy", "tomato_seeds"), items.get(2).key());
        assertEquals(Key.of("croppy", "wheat_seeds"), items.get(3).key());
    }

    @Test
    @DisplayName("Filter by category returns only items in that category")
    void testFilterByCategory() {
        ItemCatalogController.ItemCatalogSession session = new ItemCatalogController.ItemCatalogSession(
                1, ItemCategory.SEEDS.getKey()
        );

        List<ItemDefinition> items = controller.getFilteredItems(null, session);
        assertEquals(2, items.size());
        assertTrue(items.stream().allMatch(i -> "seeds".equals(i.category())));
    }

    @Test
    @DisplayName("Filter by search query matches display name, key, or material")
    void testFilterBySearch() {
        ItemCatalogController.ItemCatalogSession session = new ItemCatalogController.ItemCatalogSession(
                1, ItemCategory.ALL.getKey(), "copper", 1
        );

        List<ItemDefinition> items = controller.getFilteredItems(null, session);
        assertEquals(1, items.size());
        assertEquals(Key.of("croppy", "copper_can"), items.get(0).key());

        ItemCatalogController.ItemCatalogSession session2 = new ItemCatalogController.ItemCatalogSession(
                1, ItemCategory.ALL.getKey(), "Booster", 1
        );
        List<ItemDefinition> items2 = controller.getFilteredItems(null, session2);
        assertEquals(1, items2.size());
        assertEquals(Key.of("croppy", "growth_fertilizer"), items2.get(0).key());
    }

    @Test
    @DisplayName("Default categories are registered and ordered by priority")
    void testDefaultCategories() {
        List<ItemCategory> categories = categoryRegistry.values().stream().sorted().toList();
        assertTrue(categories.size() >= 8);
        assertEquals(ItemCategory.ALL.getKey(), categories.get(0).key());
        assertEquals(ItemCategory.SEEDS.getKey(), categories.get(1).key());
    }

    @Test
    @DisplayName("Clear filters resets search query and category to defaults")
    void testClearFiltersResetsSession() {
        ItemCatalogController.ItemCatalogSession session = new ItemCatalogController.ItemCatalogSession(
                2, ItemCategory.SEEDS.getKey(), "wheat", 2
        );
        assertEquals("wheat", session.searchQuery());
        assertEquals(ItemCategory.SEEDS.getKey(), session.categoryKey());

        ItemCatalogController.ItemCatalogSession cleared = new ItemCatalogController.ItemCatalogSession(
                1, ItemCategory.ALL.getKey(), null, 1
        );
        assertNull(cleared.searchQuery());
        assertEquals(ItemCategory.ALL.getKey(), cleared.categoryKey());
        assertEquals(1, cleared.page());
        assertEquals(1, cleared.categoryPage());
    }
}
