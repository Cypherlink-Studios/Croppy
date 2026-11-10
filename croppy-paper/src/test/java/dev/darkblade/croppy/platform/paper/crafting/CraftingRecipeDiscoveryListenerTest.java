package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.api.crafting.CraftingRecipe;
import dev.darkblade.croppy.api.crafting.IngredientChoice;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.crafting.DefaultCraftingRecipeRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeDiscoveryListenerTest {

    private DefaultItemRegistry itemRegistry;
    private DefaultCraftingRecipeRegistry craftingRegistry;
    private PaperCraftingRecipeService recipeService;
    private PluginConfig pluginConfig;
    private CraftingRecipeDiscoveryListener listener;

    private List<Collection<NamespacedKey>> discoveredBatches;

    @BeforeEach
    void setUp() {
        itemRegistry = new DefaultItemRegistry();
        craftingRegistry = new DefaultCraftingRecipeRegistry();
        discoveredBatches = new ArrayList<>();

        ItemFactory itemFactory = new ItemFactory(null, null, itemRegistry) {
            @Override
            public ItemStack createItemStack(ItemDefinition definition, int amount) {
                return new MockTestItemStack(Material.DIRT, amount);
            }

            @Override
            public ItemStack createRawVanillaItemStack(Material material, int amount) {
                return new MockTestItemStack(material, amount);
            }
        };
        CraftingRecipeChoiceResolver choiceResolver = new CraftingRecipeChoiceResolver(itemFactory);

        Server mockServer = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if ("addRecipe".equals(method.getName()) || "removeRecipe".equals(method.getName())) {
                        return true;
                    }
                    return null;
                }
        );

        recipeService = new PaperCraftingRecipeService(mockServer, craftingRegistry, itemFactory, choiceResolver, null);

        // Pre-register one recipe in the service
        CraftingRecipe recipe = CraftingRecipe.shapeless(
                Key.of("croppy", "test"),
                null,
                null,
                Key.of("minecraft", "dirt"),
                1,
                List.of(new IngredientChoice("minecraft:dirt"))
        );
        craftingRegistry.register(recipe);
        recipeService.registerAll();

        pluginConfig = new PluginConfig();
        listener = new CraftingRecipeDiscoveryListener(recipeService, pluginConfig);
    }

    @Test
    @DisplayName("PlayerJoin grants recipe discovery when auto-discover is enabled")
    void discoverOnJoinWhenEnabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("crafting.auto-discover-recipes", true);
        pluginConfig.load(config);

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("discoverRecipes".equals(method.getName())) {
                        discoveredBatches.add(new ArrayList<>((Collection<NamespacedKey>) args[0]));
                        return 1;
                    }
                    return null;
                }
        );

        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, (Component) null);
        listener.onPlayerJoin(event);

        assertEquals(1, discoveredBatches.size());
        assertTrue(discoveredBatches.get(0).contains(new NamespacedKey("croppy", "test")));
    }

    @Test
    @DisplayName("PlayerJoin ignores recipe discovery when auto-discover is disabled")
    void ignoreOnJoinWhenDisabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("crafting.auto-discover-recipes", false);
        pluginConfig.load(config);

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("discoverRecipes".equals(method.getName())) {
                        discoveredBatches.add(new ArrayList<>((Collection<NamespacedKey>) args[0]));
                        return 1;
                    }
                    return null;
                }
        );

        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, (Component) null);
        listener.onPlayerJoin(event);

        assertTrue(discoveredBatches.isEmpty());
    }
}
