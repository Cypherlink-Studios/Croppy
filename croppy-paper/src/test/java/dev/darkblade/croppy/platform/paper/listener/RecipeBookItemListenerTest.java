package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.menu.CompendiumMenuController;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RecipeBookItemListenerTest {

    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestItemStack extends ItemStack {
        private ItemMeta meta;

        @Override
        public boolean hasItemMeta() {
            return meta != null;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }
    }

    private ItemFactory itemFactory;
    private AtomicInteger compendiumOpenCount;
    private CompendiumMenuController mockController;
    private RecipeBookItemListener listener;

    @BeforeEach
    void setUp() {
        itemFactory = new ItemFactory(null, new DefaultItemRegistry());
        compendiumOpenCount = new AtomicInteger(0);
        mockController = createSpyController(compendiumOpenCount);
        listener = new RecipeBookItemListener(itemFactory, mockController);
    }

    private ItemStack createMockItem(String itemId, String action) {
        Map<NamespacedKey, String> pdcMap = new HashMap<>();
        if (itemId != null) {
            pdcMap.put(new NamespacedKey("croppy", "item_id"), itemId);
        }
        if (action != null) {
            pdcMap.put(new NamespacedKey("croppy", "action"), action);
        }

        PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        return pdcMap.get(key);
                    } else if ("has".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        return pdcMap.containsKey(key);
                    }
                    return null;
                }
        );

        ItemMeta meta = (ItemMeta) Proxy.newProxyInstance(
                ItemMeta.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> {
                    if ("getPersistentDataContainer".equals(method.getName())) {
                        return pdc;
                    }
                    return null;
                }
        );

        try {
            TestItemStack stack = (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
            stack.meta = meta;
            return stack;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Verify isRecipeBook correctly identifies items with croppy:recipe_book ID")
    void testRecipeBookDetectedByItemId() {
        ItemStack item = createMockItem("croppy:recipe_book", null);
        assertTrue(listener.isRecipeBook(item));
        assertTrue(listener.isRecipeBook(item.getItemMeta()));
    }

    @Test
    @DisplayName("Verify isRecipeBook correctly identifies items with action=open_recipes")
    void testRecipeBookDetectedByActionPdc() {
        ItemStack item = createMockItem(null, "open_recipes");
        assertTrue(listener.isRecipeBook(item));
        assertTrue(listener.isRecipeBook(item.getItemMeta()));
    }

    @Test
    @DisplayName("Verify isRecipeBook rejects unrelated items or null")
    void testUnrelatedItemNotDetected() throws Exception {
        ItemStack otherItem = createMockItem("croppy:growth_fertilizer", null);
        assertFalse(listener.isRecipeBook(otherItem));
        assertFalse(listener.isRecipeBook((ItemStack) null));
        assertFalse(listener.isRecipeBook((ItemMeta) null));

        TestItemStack emptyStack = (TestItemStack) UNSAFE.allocateInstance(TestItemStack.class);
        assertFalse(listener.isRecipeBook(emptyStack));
    }

    @Test
    @DisplayName("Verify right clicking with recipe book cancels event and opens compendium")
    void testRightClickOpensCompendium() {
        ItemStack book = createMockItem("croppy:recipe_book", "open_recipes");
        Player mockPlayer = createMockPlayer(book, null);

        PlayerInteractEvent event = new PlayerInteractEvent(
                mockPlayer,
                Action.RIGHT_CLICK_AIR,
                book,
                null,
                null,
                EquipmentSlot.HAND
        );

        listener.onPlayerInteract(event);

        assertTrue(event.isCancelled(), "Event must be cancelled to prevent block/item interactions");
        assertEquals(1, compendiumOpenCount.get(), "Compendium controller openCompendium must be invoked once");
    }

    @Test
    @DisplayName("Verify right clicking entity with recipe book cancels event and opens compendium")
    void testRightClickEntityOpensCompendium() {
        ItemStack book = createMockItem("croppy:recipe_book", "open_recipes");
        Player mockPlayer = createMockPlayer(book, null);

        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(
                mockPlayer,
                mockPlayer, // Target entity proxy
                EquipmentSlot.HAND
        );

        listener.onPlayerInteractEntity(event);

        assertTrue(event.isCancelled(), "Entity interact event must be cancelled");
        assertEquals(1, compendiumOpenCount.get(), "Compendium controller openCompendium must be invoked once");
    }

    @Test
    @DisplayName("Verify off-hand interaction does not fire duplicate compendium open when main-hand already holds recipe book")
    void testOffHandIgnoredIfMainHandAlsoRecipeBook() {
        ItemStack mainBook = createMockItem("croppy:recipe_book", "open_recipes");
        ItemStack offBook = createMockItem("croppy:recipe_book", "open_recipes");
        Player mockPlayer = createMockPlayer(mainBook, offBook);

        // First event: main hand
        PlayerInteractEvent mainEvent = new PlayerInteractEvent(
                mockPlayer,
                Action.RIGHT_CLICK_AIR,
                mainBook,
                null,
                null,
                EquipmentSlot.HAND
        );
        listener.onPlayerInteract(mainEvent);
        assertEquals(1, compendiumOpenCount.get());

        // Second event: off hand while main hand has book
        PlayerInteractEvent offEvent = new PlayerInteractEvent(
                mockPlayer,
                Action.RIGHT_CLICK_AIR,
                offBook,
                null,
                null,
                EquipmentSlot.OFF_HAND
        );
        listener.onPlayerInteract(offEvent);
        assertEquals(1, compendiumOpenCount.get(), "Off-hand event must be ignored to prevent duplicate GUI openings");
    }

    private CompendiumMenuController createSpyController(AtomicInteger opens) {
        dev.darkblade.croppy.core.menu.DefaultMenuRegistry menuReg = new dev.darkblade.croppy.core.menu.DefaultMenuRegistry();
        dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry recipeReg = new dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry();
        dev.darkblade.croppy.core.discovery.DefaultRecipeDiscoveryEngine discoveryEngine = new dev.darkblade.croppy.core.discovery.DefaultRecipeDiscoveryEngine();
        dev.darkblade.croppy.platform.paper.menu.PaperMenuRenderer renderer = new dev.darkblade.croppy.platform.paper.menu.PaperMenuRenderer(itemFactory);

        return new CompendiumMenuController(menuReg, recipeReg, discoveryEngine, renderer, itemFactory) {
            @Override
            public void openCompendium(Player player, Key categoryKey, int page) {
                opens.incrementAndGet();
            }
        };
    }

    private Player createMockPlayer(ItemStack mainHand, ItemStack offHand) {
        World mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> null
        );
        Location loc = new Location(mockWorld, 0, 64, 0);

        PlayerInventory mockInventory = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getItemInMainHand".equals(name)) {
                        return mainHand;
                    } else if ("getItemInOffHand".equals(name)) {
                        return offHand;
                    }
                    return null;
                }
        );

        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getUniqueId".equals(name)) {
                        return UUID.randomUUID();
                    } else if ("getLocation".equals(name)) {
                        return loc;
                    } else if ("getInventory".equals(name)) {
                        return mockInventory;
                    } else if ("playSound".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );
    }
}
