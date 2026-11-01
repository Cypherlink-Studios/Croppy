package dev.darkblade.croppy.platform.paper.discovery;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.storage.RecipeKnowledgeStorage;
import dev.darkblade.croppy.core.discovery.DefaultRecipeDiscoveryEngine;
import dev.darkblade.croppy.core.discovery.method.ExperimentalDiscoveryMethod;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class StationExperimentationHandlerTest {

    private DefaultRecipeRegistry recipeRegistry;
    private DefaultRecipeDiscoveryEngine discoveryEngine;
    private TestRecipeKnowledgeStorage storage;
    private StationExperimentationHandler handler;

    private UUID playerId;
    private Player mockPlayer;
    private List<String> playerMessages;

    private StationInstance potStation;
    private StorageComponent storageComp;
    private RecipeDefinition soupRecipe;

    @BeforeEach
    void setUp() {
        recipeRegistry = new DefaultRecipeRegistry();
        discoveryEngine = new DefaultRecipeDiscoveryEngine();
        storage = new TestRecipeKnowledgeStorage();
        handler = new StationExperimentationHandler(recipeRegistry, discoveryEngine, storage, new DiscoveryFeedbackHelper());

        playerId = UUID.randomUUID();
        playerMessages = new ArrayList<>();

        World mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> null
        );
        Location loc = new Location(mockWorld, 0, 64, 0);

        mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getUniqueId".equals(name)) {
                        return playerId;
                    } else if ("getLocation".equals(name)) {
                        return loc;
                    } else if ("sendMessage".equals(name)) {
                        playerMessages.add(args[0].toString());
                        return null;
                    } else if ("playSound".equals(name) || "spawnParticle".equals(name) || "showTitle".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        Key potKey = Key.of("croppy", "cooking_pot");
        StationDefinition potDef = new StationDefinition(
                potKey, "Cooking Pot", "minecraft:cauldron",
                dev.darkblade.croppy.api.station.StationInteractionType.GUI,
                5, 3, null, false, List.of(), Map.of(), null
        );

        potStation = new DefaultStationInstance(potKey, new dev.darkblade.croppy.api.world.BlockPosition("world", 0, 64, 0), potDef);
        storageComp = new DefaultStorageComponent(5, 3);
        potStation.setComponent(StorageComponent.class, storageComp);

        soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                potKey,
                Key.of("croppy", "tomato_soup_p"),
                List.of(StoredItem.of(Key.of("croppy", "tomato"), 2)),
                Key.of("croppy", "tomato_soup"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(ExperimentalDiscoveryMethod.KEY),
                Map.of()
        );
        recipeRegistry.register(soupRecipe);
    }

    @Test
    @DisplayName("Fails and warns when station inputs are empty")
    void testEmptyStationInputs() {
        boolean discovered = handler.handleExperimentation(mockPlayer, potStation);
        assertFalse(discovered);
        assertFalse(playerMessages.isEmpty());
    }

    @Test
    @DisplayName("Fails when ingredients do not match any recipe")
    void testInvalidIngredients() {
        storageComp.addInput(StoredItem.of(Key.of("minecraft", "dirt"), 1));

        boolean discovered = handler.handleExperimentation(mockPlayer, potStation);
        assertFalse(discovered);
        // Ingredients remain completely untouched!
        assertEquals(1, storageComp.getInputs().size());
    }

    @Test
    @DisplayName("Successfully discovers matching recipe and preserves all ingredients in station")
    void testSuccessfulDiscoveryWithoutConsumingInputs() {
        storageComp.addInput(StoredItem.of(Key.of("croppy", "tomato"), 2));

        // Prior to experiment: UNKNOWN
        assertEquals(KnowledgeState.UNKNOWN, discoveryEngine.getKnowledgeState(playerId, soupRecipe.getKey()));

        boolean discovered = handler.handleExperimentation(mockPlayer, potStation);
        assertTrue(discovered);

        // State is now DISCOVERED
        assertEquals(KnowledgeState.DISCOVERED, discoveryEngine.getKnowledgeState(playerId, soupRecipe.getKey()));

        // Crucial verification: ingredients in station are NOT consumed!
        assertEquals(1, storageComp.getInputs().size());
        assertEquals(2, storageComp.getInputs().get(0).amount());

        // Storage was notified
        assertTrue(storage.saved.contains(soupRecipe.getKey()));

        // Second experiment returns false because already known
        boolean secondAttempt = handler.handleExperimentation(mockPlayer, potStation);
        assertFalse(secondAttempt);
    }

    private static class TestRecipeKnowledgeStorage implements RecipeKnowledgeStorage {
        final Set<Key> saved = new HashSet<>();

        @Override
        public CompletableFuture<Map<Key, dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge>> loadKnowledge(UUID playerId) {
            return CompletableFuture.completedFuture(Map.of());
        }

        @Override
        public CompletableFuture<Void> saveKnowledge(UUID playerId, dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge knowledge) {
            saved.add(knowledge.recipeKey());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> saveAllKnowledge(UUID playerId, Collection<dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge> knowledge) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> deleteKnowledge(UUID playerId, Key recipeKey) {
            saved.remove(recipeKey);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> clearKnowledge(UUID playerId) {
            saved.clear();
            return CompletableFuture.completedFuture(null);
        }
    }
}
