package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.core.crafting.DefaultCraftingRecipeRegistry;
import dev.darkblade.croppy.core.crafting.YamlCraftingRecipeLoader;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.item.YamlItemLoader;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class BundledCraftingRecipesIntegrityTest {

    private DefaultItemRegistry itemRegistry;
    private DefaultCraftingRecipeRegistry craftingRegistry;
    private ItemFactory itemFactory;
    private CraftingRecipeChoiceResolver choiceResolver;
    private PaperCraftingRecipeService recipeService;
    private List<Recipe> registeredBukkitRecipes;

    @BeforeEach
    void setUp() {
        itemRegistry = new DefaultItemRegistry();
        craftingRegistry = new DefaultCraftingRecipeRegistry();
        registeredBukkitRecipes = new ArrayList<>();

        // 1. Load bundled items from src/main/resources/items
        Path itemsDir = Paths.get("src", "main", "resources", "items");
        if (!Files.exists(itemsDir)) {
            itemsDir = Paths.get("croppy-paper", "src", "main", "resources", "items");
        }
        assertTrue(Files.isDirectory(itemsDir), "Items directory must exist");
        YamlItemLoader itemLoader = new YamlItemLoader();
        int loadedItems = itemLoader.loadAllCategories(itemsDir, Logger.getAnonymousLogger(), itemRegistry);
        assertTrue(loadedItems > 0, "Must load bundled items");

        itemFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(dev.darkblade.croppy.api.item.ItemDefinition definition, int amount) {
                String matKey = definition.materialKey();
                if (matKey != null && matKey.startsWith("minecraft:")) {
                    matKey = matKey.substring("minecraft:".length());
                }
                Material mat = (matKey != null) ? Material.matchMaterial(matKey) : null;
                return new MockTestItemStack(mat != null ? mat : Material.STONE, amount);
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                return new MockTestItemStack(material, amount);
            }
        };

        choiceResolver = new CraftingRecipeChoiceResolver(itemFactory);

        Server mockServer = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if ("addRecipe".equals(method.getName())) {
                        registeredBukkitRecipes.add((Recipe) args[0]);
                        return true;
                    }
                    if ("removeRecipe".equals(method.getName())) {
                        return true;
                    }
                    return null;
                }
        );

        recipeService = new PaperCraftingRecipeService(
                mockServer,
                craftingRegistry,
                itemFactory,
                choiceResolver,
                Logger.getLogger("test")
        );
    }

    @Test
    @DisplayName("Verify all bundled starter crafting recipes parse cleanly and register into Bukkit")
    void testBundledCraftingRecipesIntegrity() {
        Path craftingDir = Paths.get("src", "main", "resources", "crafting");
        if (!Files.exists(craftingDir)) {
            craftingDir = Paths.get("croppy-paper", "src", "main", "resources", "crafting");
        }
        assertTrue(Files.isDirectory(craftingDir), "Crafting directory must exist");

        YamlCraftingRecipeLoader loader = new YamlCraftingRecipeLoader();
        int loadedCount = loader.loadAll(craftingDir, craftingRegistry, Logger.getAnonymousLogger());

        assertTrue(loadedCount >= 8, "Must load at least 8 starter crafting recipes (found " + loadedCount + ")");

        // Check required starter pack items have recipes
        List<String> requiredResults = List.of(
                "croppy:cooking_pot",
                "croppy:stove",
                "croppy:skillet",
                "croppy:inspection_tool",
                "croppy:thermometer",
                "croppy:wooden_can",
                "croppy:copper_can",
                "croppy:iron_can"
        );

        for (String expectedResult : requiredResults) {
            Key resultKey = Key.parse(expectedResult);
            List<CraftingRecipe> matching = craftingRegistry.findByResult(resultKey);
            assertFalse(matching.isEmpty(), "Missing starter crafting recipe for result: " + expectedResult);
        }

        // Validate all custom ingredient references point to valid registered items
        for (CraftingRecipe recipe : craftingRegistry.values()) {
            List<IngredientChoice> allIngredients = new ArrayList<>();
            allIngredients.addAll(recipe.shapedIngredients().values());
            allIngredients.addAll(recipe.shapelessIngredients());

            for (IngredientChoice ing : allIngredients) {
                if (!ing.isTag() && ing.identifier().startsWith("croppy:")) {
                    Key itemKey = Key.parse(ing.identifier());
                    assertTrue(
                            itemRegistry.get(itemKey).isPresent(),
                            "Recipe '" + recipe.getKey() + "' references unknown custom item: " + ing.identifier()
                    );
                }
            }
        }

        // Register all into Bukkit
        int registeredCount = recipeService.registerAll();
        assertEquals(loadedCount, registeredCount, "All loaded crafting recipes must register cleanly without errors");
        assertEquals(loadedCount, registeredBukkitRecipes.size());
    }
}
