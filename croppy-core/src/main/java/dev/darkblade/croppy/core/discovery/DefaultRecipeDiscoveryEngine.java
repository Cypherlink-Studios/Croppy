package dev.darkblade.croppy.core.discovery;

import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryMethod;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Default domain implementation of {@link RecipeDiscoveryEngine} managing in-memory player knowledge,
 * thread-safe caching, and idempotent discovery state transitions.
 */
public final class DefaultRecipeDiscoveryEngine implements RecipeDiscoveryEngine {

    private final Map<Key, RecipeDiscoveryMethod> methods = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Key, PlayerRecipeKnowledge>> playerKnowledgeCache = new ConcurrentHashMap<>();

    private BiConsumer<UUID, PlayerRecipeKnowledge> onKnowledgeChanged;
    private BiConsumer<UUID, Key> onKnowledgeRevoked;

    public DefaultRecipeDiscoveryEngine() {
    }

    /**
     * Sets a callback listener invoked whenever a player's recipe knowledge transitions to HINTED or DISCOVERED.
     *
     * @param onKnowledgeChanged listener
     */
    public void setOnKnowledgeChanged(BiConsumer<UUID, PlayerRecipeKnowledge> onKnowledgeChanged) {
        this.onKnowledgeChanged = onKnowledgeChanged;
    }

    /**
     * Sets a callback listener invoked whenever a player's recipe knowledge is revoked.
     *
     * @param onKnowledgeRevoked listener
     */
    public void setOnKnowledgeRevoked(BiConsumer<UUID, Key> onKnowledgeRevoked) {
        this.onKnowledgeRevoked = onKnowledgeRevoked;
    }

    @Override
    public void registerMethod(RecipeDiscoveryMethod method) {
        Objects.requireNonNull(method, "method cannot be null");
        methods.put(method.getKey(), method);
    }

    @Override
    public Optional<RecipeDiscoveryMethod> getMethod(Key methodKey) {
        if (methodKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(methods.get(methodKey));
    }

    @Override
    public Collection<RecipeDiscoveryMethod> getMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }

    @Override
    public KnowledgeState getKnowledgeState(UUID playerId, Key recipeKey) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipeKey, "recipeKey cannot be null");

        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.get(playerId);
        if (playerMap == null) {
            return KnowledgeState.UNKNOWN;
        }

        PlayerRecipeKnowledge knowledge = playerMap.get(recipeKey);
        return knowledge != null ? knowledge.state() : KnowledgeState.UNKNOWN;
    }

    @Override
    public Optional<PlayerRecipeKnowledge> getKnowledge(UUID playerId, Key recipeKey) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipeKey, "recipeKey cannot be null");

        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.get(playerId);
        if (playerMap == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(playerMap.get(recipeKey));
    }

    @Override
    public Map<Key, PlayerRecipeKnowledge> getAllKnowledge(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.get(playerId);
        if (playerMap == null) {
            return Map.of();
        }
        return Map.copyOf(playerMap);
    }

    @Override
    public void loadPlayerKnowledge(UUID playerId, Map<Key, PlayerRecipeKnowledge> knowledge) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        if (knowledge != null) {
            playerMap.putAll(knowledge);
        }
    }

    @Override
    public void unloadPlayerKnowledge(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        playerKnowledgeCache.remove(playerId);
    }

    @Override
    public DiscoveryResult discoverRecipe(UUID playerId, RecipeDefinition recipe, Key discoveryMethodKey) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");

        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        PlayerRecipeKnowledge existing = playerMap.get(recipe.key());

        if (existing != null && existing.state() == KnowledgeState.DISCOVERED) {
            return DiscoveryResult.ALREADY_KNOWN;
        }

        PlayerRecipeKnowledge discovered = PlayerRecipeKnowledge.discovered(playerId, recipe.key(), discoveryMethodKey);
        playerMap.put(recipe.key(), discovered);

        if (onKnowledgeChanged != null) {
            onKnowledgeChanged.accept(playerId, discovered);
        }

        return DiscoveryResult.DISCOVERED;
    }

    @Override
    public DiscoveryResult grantHint(UUID playerId, RecipeDefinition recipe, Key discoveryMethodKey, String message) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");

        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        PlayerRecipeKnowledge existing = playerMap.get(recipe.key());

        if (existing != null && existing.state() == KnowledgeState.DISCOVERED) {
            return DiscoveryResult.ALREADY_KNOWN;
        }

        Map<String, String> meta = (message != null && !message.isBlank())
                ? Map.of("hint_message", message)
                : Map.of();

        PlayerRecipeKnowledge hinted = PlayerRecipeKnowledge.hinted(playerId, recipe.key(), discoveryMethodKey, meta);
        playerMap.put(recipe.key(), hinted);

        if (onKnowledgeChanged != null) {
            onKnowledgeChanged.accept(playerId, hinted);
        }

        return DiscoveryResult.hinted(message);
    }

    @Override
    public boolean revokeRecipe(UUID playerId, Key recipeKey) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipeKey, "recipeKey cannot be null");

        Map<Key, PlayerRecipeKnowledge> playerMap = playerKnowledgeCache.get(playerId);
        if (playerMap == null) {
            return false;
        }

        PlayerRecipeKnowledge removed = playerMap.remove(recipeKey);
        if (removed != null) {
            if (onKnowledgeRevoked != null) {
                onKnowledgeRevoked.accept(playerId, recipeKey);
            }
            return true;
        }

        return false;
    }
}
