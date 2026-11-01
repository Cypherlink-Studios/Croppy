package dev.darkblade.croppy.core.discovery.method;

import dev.darkblade.croppy.api.discovery.DiscoveryContext;
import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryMethod;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Discovery method that unlocks or permits unlocking of advanced recipes only after
 * prerequisite recipes are already discovered by the player.
 */
public final class KnowledgeBasedDiscoveryMethod implements RecipeDiscoveryMethod {

    public static final Key KEY = Key.of("croppy", "knowledge_based");

    private final RecipeDiscoveryEngine engine;

    public KnowledgeBasedDiscoveryMethod(RecipeDiscoveryEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public DiscoveryResult evaluate(DiscoveryContext context, RecipeDefinition recipe) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");

        if (!recipe.enabledDiscoveryMethods().contains(KEY)) {
            return DiscoveryResult.INELIGIBLE;
        }

        String prereqStr = recipe.properties().get("prerequisites");
        if (prereqStr == null || prereqStr.isBlank()) {
            return DiscoveryResult.DISCOVERED;
        }

        List<String> prereqKeys = Arrays.stream(prereqStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String keyStr : prereqKeys) {
            Key prereqKey = Key.parse(keyStr);
            if (engine.getKnowledgeState(context.playerId(), prereqKey) != KnowledgeState.DISCOVERED) {
                return DiscoveryResult.INELIGIBLE;
            }
        }

        return DiscoveryResult.DISCOVERED;
    }
}
