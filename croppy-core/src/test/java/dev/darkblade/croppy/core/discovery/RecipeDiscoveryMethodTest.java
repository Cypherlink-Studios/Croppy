package dev.darkblade.croppy.core.discovery;

import dev.darkblade.croppy.api.discovery.DiscoveryContext;
import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.HintDefinition;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.core.discovery.method.AutomaticDiscoveryMethod;
import dev.darkblade.croppy.core.discovery.method.ExperimentalDiscoveryMethod;
import dev.darkblade.croppy.core.discovery.method.KnowledgeBasedDiscoveryMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeDiscoveryMethodTest {

    private UUID playerId;
    private DefaultRecipeDiscoveryEngine engine;
    private AutomaticDiscoveryMethod automaticMethod;
    private ExperimentalDiscoveryMethod experimentalMethod;
    private KnowledgeBasedDiscoveryMethod knowledgeMethod;

    private RecipeDefinition breadRecipe;
    private RecipeDefinition cakeRecipe;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        engine = new DefaultRecipeDiscoveryEngine();
        automaticMethod = new AutomaticDiscoveryMethod();
        experimentalMethod = new ExperimentalDiscoveryMethod();
        knowledgeMethod = new KnowledgeBasedDiscoveryMethod(engine);

        breadRecipe = new RecipeDefinition(
                Key.of("croppy", "bread"),
                "Bread",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "oven"),
                Key.of("croppy", "bread_p"),
                List.of(StoredItem.of(Key.of("croppy", "flour"), 3)),
                Key.of("croppy", "bread"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(AutomaticDiscoveryMethod.KEY, ExperimentalDiscoveryMethod.KEY),
                Map.of()
        );

        cakeRecipe = new RecipeDefinition(
                Key.of("croppy", "artisan_cake"),
                "Artisan Cake",
                RecipeCategory.BAKERY.getKey(),
                Key.of("croppy", "oven"),
                Key.of("croppy", "cake_p"),
                List.of(StoredItem.of(Key.of("croppy", "flour"), 2)),
                Key.of("croppy", "cake"),
                1,
                HintDefinition.DEFAULT,
                false,
                List.of(KnowledgeBasedDiscoveryMethod.KEY),
                Map.of("prerequisites", "croppy:bread")
        );
    }

    @Test
    @DisplayName("Automatic discovery unlocks when input item is obtained")
    void testAutomaticDiscovery() {
        // Obtaining flour triggers automatic discovery for bread
        DiscoveryContext context = DiscoveryContext.of(playerId, Key.of("croppy", "flour"));
        DiscoveryResult result = automaticMethod.evaluate(context, breadRecipe);
        assertEquals(DiscoveryResult.Status.DISCOVERED, result.status());

        // Obtaining dirt does not trigger
        DiscoveryContext unrelatedContext = DiscoveryContext.of(playerId, Key.of("minecraft", "dirt"));
        DiscoveryResult unrelatedResult = automaticMethod.evaluate(unrelatedContext, breadRecipe);
        assertEquals(DiscoveryResult.Status.INELIGIBLE, unrelatedResult.status());
    }

    @Test
    @DisplayName("Experimental discovery unlocks on valid station and matching inputs")
    void testExperimentalDiscovery() {
        List<StoredItem> inputs = List.of(StoredItem.of(Key.of("croppy", "flour"), 3));
        DiscoveryContext context = DiscoveryContext.experimental(playerId, Key.of("croppy", "oven"), inputs);

        DiscoveryResult result = experimentalMethod.evaluate(context, breadRecipe);
        assertEquals(DiscoveryResult.Status.DISCOVERED, result.status());

        // Wrong station (e.g. cooking_pot instead of oven)
        DiscoveryContext wrongStationContext = DiscoveryContext.experimental(playerId, Key.of("croppy", "cooking_pot"), inputs);
        assertEquals(DiscoveryResult.Status.INELIGIBLE, experimentalMethod.evaluate(wrongStationContext, breadRecipe).status());
    }

    @Test
    @DisplayName("Knowledge-based discovery requires prerequisite recipes to be discovered first")
    void testKnowledgeBasedDiscovery() {
        DiscoveryContext context = DiscoveryContext.of(playerId, null);

        // Initially, bread is NOT discovered, so cake is INELIGIBLE
        DiscoveryResult initialResult = knowledgeMethod.evaluate(context, cakeRecipe);
        assertEquals(DiscoveryResult.Status.INELIGIBLE, initialResult.status());

        // Now discover prerequisite bread
        engine.discoverRecipe(playerId, breadRecipe, AutomaticDiscoveryMethod.KEY);

        // Cake should now evaluate to DISCOVERED
        DiscoveryResult unlockedResult = knowledgeMethod.evaluate(context, cakeRecipe);
        assertEquals(DiscoveryResult.Status.DISCOVERED, unlockedResult.status());
    }
}
