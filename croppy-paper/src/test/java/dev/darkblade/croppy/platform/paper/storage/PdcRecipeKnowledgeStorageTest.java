package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PdcRecipeKnowledgeStorageTest {

    private UUID playerId;
    private NamespacedKey storageKey;
    private Map<NamespacedKey, Object> pdcMap;
    private PersistentDataContainer pdc;
    private Player mockPlayer;
    private Server mockServer;
    private PdcRecipeKnowledgeStorage storage;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        storageKey = new NamespacedKey("croppy", "recipe_knowledge");
        pdcMap = new HashMap<>();

        pdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        return pdcMap.get(key);
                    } else if ("set".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        Object val = args[2];
                        pdcMap.put(key, val);
                        return null;
                    } else if ("has".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        return pdcMap.containsKey(key);
                    } else if ("remove".equals(name)) {
                        NamespacedKey key = (NamespacedKey) args[0];
                        pdcMap.remove(key);
                        return null;
                    }
                    return null;
                }
        );

        mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getPersistentDataContainer".equals(method.getName())) {
                        return pdc;
                    } else if ("getUniqueId".equals(method.getName())) {
                        return playerId;
                    }
                    return null;
                }
        );

        mockServer = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if ("getPlayer".equals(method.getName())) {
                        UUID id = (UUID) args[0];
                        return id.equals(playerId) ? mockPlayer : null;
                    } else if ("isPrimaryThread".equals(method.getName())) {
                        return true;
                    }
                    return null;
                }
        );

        storage = new PdcRecipeKnowledgeStorage(mockServer, storageKey, Runnable::run);
    }

    @Test
    @DisplayName("Empty PDC returns empty map")
    void testLoadEmpty() throws Exception {
        Map<Key, PlayerRecipeKnowledge> map = storage.loadKnowledge(playerId).get();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("Save and load round trip through PDC")
    void testSaveAndLoad() throws Exception {
        Key recipeKey = Key.of("croppy", "tomato_soup");
        PlayerRecipeKnowledge knowledge = new PlayerRecipeKnowledge(
                playerId,
                recipeKey,
                KnowledgeState.DISCOVERED,
                Instant.now(),
                Key.of("croppy", "experimental"),
                Map.of("note", "tasty")
        );

        storage.saveKnowledge(playerId, knowledge).get();

        // Verify PDC holds bytes
        assertTrue(pdcMap.containsKey(storageKey));
        byte[] bytes = (byte[]) pdcMap.get(storageKey);
        assertTrue(bytes.length > 0);

        // Load back
        Map<Key, PlayerRecipeKnowledge> loaded = storage.loadKnowledge(playerId).get();
        assertEquals(1, loaded.size());
        PlayerRecipeKnowledge result = loaded.get(recipeKey);
        assertNotNull(result);
        assertEquals(KnowledgeState.DISCOVERED, result.state());
        assertEquals("tasty", result.metadata().get("note"));
    }

    @Test
    @DisplayName("Delete knowledge removes recipe entry")
    void testDeleteKnowledge() throws Exception {
        Key recipe1 = Key.of("croppy", "tomato_soup");
        Key recipe2 = Key.of("croppy", "bread");

        storage.saveKnowledge(playerId, PlayerRecipeKnowledge.discovered(playerId, recipe1, Key.of("croppy", "auto"))).get();
        storage.saveKnowledge(playerId, PlayerRecipeKnowledge.discovered(playerId, recipe2, Key.of("croppy", "auto"))).get();

        assertEquals(2, storage.loadKnowledge(playerId).get().size());

        storage.deleteKnowledge(playerId, recipe1).get();

        Map<Key, PlayerRecipeKnowledge> remaining = storage.loadKnowledge(playerId).get();
        assertEquals(1, remaining.size());
        assertFalse(remaining.containsKey(recipe1));
        assertTrue(remaining.containsKey(recipe2));
    }

    @Test
    @DisplayName("Clear knowledge clears all entries")
    void testClearKnowledge() throws Exception {
        Key recipe = Key.of("croppy", "tomato_soup");
        storage.saveKnowledge(playerId, PlayerRecipeKnowledge.discovered(playerId, recipe, Key.of("croppy", "auto"))).get();

        storage.clearKnowledge(playerId).get();
        assertTrue(storage.loadKnowledge(playerId).get().isEmpty());
    }
}
