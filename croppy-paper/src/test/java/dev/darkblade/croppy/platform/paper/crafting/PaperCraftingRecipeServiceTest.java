package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.crafting.DefaultCraftingRecipeRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PaperCraftingRecipeServiceTest {

    private DefaultItemRegistry itemRegistry;
    private DefaultCraftingRecipeRegistry craftingRegistry;
    private ItemFactory itemFactory;
    private CraftingRecipeChoiceResolver choiceResolver;
    private PaperCraftingRecipeService service;

    private List<Recipe> addedRecipes;
    private List<NamespacedKey> removedKeys;
    private Server mockServer;

    @BeforeEach
    void setUp() {
        itemRegistry = new DefaultItemRegistry();
        craftingRegistry = new DefaultCraftingRecipeRegistry();
        addedRecipes = new ArrayList<>();
        removedKeys = new ArrayList<>();

        itemFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                return new MockTestItemStack(Material.CAULDRON, amount);
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                return new MockTestItemStack(material, amount);
            }
        };

        choiceResolver = new CraftingRecipeChoiceResolver(itemFactory);

        mockServer = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if ("addRecipe".equals(method.getName())) {
                        addedRecipes.add((Recipe) args[0]);
                        return true;
                    }
                    if ("removeRecipe".equals(method.getName())) {
                        removedKeys.add((NamespacedKey) args[0]);
                        return true;
                    }
                    return null;
                }
        );

        service = new PaperCraftingRecipeService(
                mockServer,
                craftingRegistry,
                itemFactory,
                choiceResolver,
                Logger.getLogger("test")
        );
    }

    @Test
    @DisplayName("Register shaped recipe creates ShapedRecipe in server and tracks key")
    void registerShapedRecipe() {
        ItemDefinition def = new ItemDefinition(
                Key.of("croppy", "cooking_pot"),
                "stations",
                "minecraft:cauldron",
                101,
                "Cooking Pot",
                List.of(),
                Map.of()
        );
        itemRegistry.register(def);

        CraftingRecipe recipe = CraftingRecipe.shaped(
                Key.of("croppy", "cooking_pot"),
                "croppy_stations",
                "misc",
                Key.of("croppy", "cooking_pot"),
                1,
                List.of("I I", "ICI", " I "),
                Map.of(
                        'I', new IngredientChoice("minecraft:iron_ingot"),
                        'C', new IngredientChoice("croppy:cooking_pot")
                )
        );
        craftingRegistry.register(recipe);

        int count = service.registerAll();
        assertEquals(1, count);
        assertEquals(1, addedRecipes.size());
        assertInstanceOf(ShapedRecipe.class, addedRecipes.get(0));

        ShapedRecipe shaped = (ShapedRecipe) addedRecipes.get(0);
        assertEquals(new NamespacedKey("croppy", "cooking_pot"), shaped.getKey());
        assertEquals("croppy_stations", shaped.getGroup());

        assertTrue(service.getRegisteredKeys().contains(new NamespacedKey("croppy", "cooking_pot")));
    }

    @Test
    @DisplayName("Register shapeless recipe creates ShapelessRecipe in server")
    void registerShapelessRecipe() {
        CraftingRecipe recipe = CraftingRecipe.shapeless(
                Key.of("croppy", "fertilizer_craft"),
                "croppy_fertilizers",
                "misc",
                Key.of("minecraft", "bone_meal"),
                2,
                List.of(
                        new IngredientChoice("minecraft:bone_meal", 1),
                        new IngredientChoice("minecraft:wheat", 2)
                )
        );
        craftingRegistry.register(recipe);

        int count = service.registerAll();
        assertEquals(1, count);
        assertEquals(1, addedRecipes.size());
        assertInstanceOf(ShapelessRecipe.class, addedRecipes.get(0));

        ShapelessRecipe shapeless = (ShapelessRecipe) addedRecipes.get(0);
        assertEquals(new NamespacedKey("croppy", "fertilizer_craft"), shapeless.getKey());
        assertEquals(3, shapeless.getChoiceList().size()); // 1 bone meal + 2 wheat
    }

    @Test
    @DisplayName("UnregisterAll removes all registered keys from server")
    void unregisterAll() {
        CraftingRecipe recipe = CraftingRecipe.shapeless(
                Key.of("croppy", "test_recipe"),
                null,
                null,
                Key.of("minecraft", "dirt"),
                1,
                List.of(new IngredientChoice("minecraft:dirt"))
        );
        craftingRegistry.register(recipe);
        service.registerAll();

        assertEquals(1, service.getRegisteredKeys().size());

        service.unregisterAll();
        assertEquals(0, service.getRegisteredKeys().size());
        assertTrue(removedKeys.contains(new NamespacedKey("croppy", "test_recipe")));
    }
}
