package dev.darkblade.croppy.core.discovery;

import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRecipeDiscoveryEngineTest {

    private DefaultRecipeDiscoveryEngine engine;
    private RecipeDefinition soupRecipe;
    private UUID playerId;
    private Key methodKey;

    @BeforeEach
    void setUp() {
        engine = new DefaultRecipeDiscoveryEngine();
        playerId = UUID.randomUUID();
        methodKey = Key.of("croppy", "experimental");

        soupRecipe = new RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "tomato_soup_p"),
                List.of(StoredItem.of(Key.of("croppy", "tomato"), 2)),
                Key.of("croppy", "tomato_soup"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(methodKey),
                Map.of()
        );
    }

    @Test
    @DisplayName("Defaults to UNKNOWN knowledge state")
    void testInitialUnknown() {
        assertEquals(KnowledgeState.UNKNOWN, engine.getKnowledgeState(playerId, soupRecipe.getKey()));
        assertTrue(engine.getKnowledge(playerId, soupRecipe.getKey()).isEmpty());
        assertTrue(engine.getAllKnowledge(playerId).isEmpty());
    }

    @Test
    @DisplayName("Granting discovery transitions state and fires listener")
    void testDiscoverRecipe() {
        AtomicInteger listenerCalls = new AtomicInteger(0);
        engine.setOnKnowledgeChanged((pId, knowledge) -> {
            assertEquals(playerId, pId);
            assertEquals(soupRecipe.getKey(), knowledge.recipeKey());
            assertEquals(KnowledgeState.DISCOVERED, knowledge.state());
            listenerCalls.incrementAndGet();
        });

        DiscoveryResult result = engine.discoverRecipe(playerId, soupRecipe, methodKey);
        assertEquals(DiscoveryResult.Status.DISCOVERED, result.status());
        assertEquals(1, listenerCalls.get());
        assertEquals(KnowledgeState.DISCOVERED, engine.getKnowledgeState(playerId, soupRecipe.getKey()));

        // Idempotent second call
        DiscoveryResult duplicate = engine.discoverRecipe(playerId, soupRecipe, methodKey);
        assertEquals(DiscoveryResult.Status.ALREADY_KNOWN, duplicate.status());
        assertEquals(1, listenerCalls.get(), "Listener must not be called again on duplicate grant");
    }

    @Test
    @DisplayName("Granting hint transitions to HINTED and does not downgrade DISCOVERED")
    void testHintTransitions() {
        DiscoveryResult hintResult = engine.grantHint(playerId, soupRecipe, methodKey, "Smells like tomato");
        assertEquals(DiscoveryResult.Status.HINTED, hintResult.status());
        assertEquals(KnowledgeState.HINTED, engine.getKnowledgeState(playerId, soupRecipe.getKey()));

        // Promoting HINTED to DISCOVERED
        DiscoveryResult discoverResult = engine.discoverRecipe(playerId, soupRecipe, methodKey);
        assertEquals(DiscoveryResult.Status.DISCOVERED, discoverResult.status());
        assertEquals(KnowledgeState.DISCOVERED, engine.getKnowledgeState(playerId, soupRecipe.getKey()));

        // Hinting an already DISCOVERED recipe returns ALREADY_KNOWN without downgrade
        DiscoveryResult redundantHint = engine.grantHint(playerId, soupRecipe, methodKey, "Another clue");
        assertEquals(DiscoveryResult.Status.ALREADY_KNOWN, redundantHint.status());
        assertEquals(KnowledgeState.DISCOVERED, engine.getKnowledgeState(playerId, soupRecipe.getKey()));
    }

    @Test
    @DisplayName("Revoke removes knowledge and fires revoke listener")
    void testRevokeRecipe() {
        engine.discoverRecipe(playerId, soupRecipe, methodKey);
        assertEquals(KnowledgeState.DISCOVERED, engine.getKnowledgeState(playerId, soupRecipe.getKey()));

        AtomicInteger revokeCalls = new AtomicInteger(0);
        engine.setOnKnowledgeRevoked((pId, rKey) -> {
            assertEquals(playerId, pId);
            assertEquals(soupRecipe.getKey(), rKey);
            revokeCalls.incrementAndGet();
        });

        boolean revoked = engine.revokeRecipe(playerId, soupRecipe.getKey());
        assertTrue(revoked);
        assertEquals(1, revokeCalls.get());
        assertEquals(KnowledgeState.UNKNOWN, engine.getKnowledgeState(playerId, soupRecipe.getKey()));

        // Revoking already unknown recipe returns false
        assertFalse(engine.revokeRecipe(playerId, soupRecipe.getKey()));
        assertEquals(1, revokeCalls.get());
    }

    @Test
    @DisplayName("Lifecycle load and unload manage player cache properly")
    void testLoadAndUnload() {
        PlayerRecipeKnowledge knowledge = PlayerRecipeKnowledge.discovered(playerId, soupRecipe.getKey(), methodKey);
        engine.loadPlayerKnowledge(playerId, Map.of(soupRecipe.getKey(), knowledge));

        assertEquals(KnowledgeState.DISCOVERED, engine.getKnowledgeState(playerId, soupRecipe.getKey()));
        assertEquals(1, engine.getAllKnowledge(playerId).size());

        engine.unloadPlayerKnowledge(playerId);
        assertEquals(KnowledgeState.UNKNOWN, engine.getKnowledgeState(playerId, soupRecipe.getKey()));
    }
}
