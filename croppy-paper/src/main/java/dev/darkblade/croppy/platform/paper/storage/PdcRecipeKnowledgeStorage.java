package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import dev.darkblade.croppy.api.storage.RecipeKnowledgeStorage;
import dev.darkblade.croppy.core.storage.RecipeKnowledgeBinaryCodec;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Paper adapter implementation persisting player recipe knowledge into Paper's {@link PersistentDataContainer}.
 */
public final class PdcRecipeKnowledgeStorage implements RecipeKnowledgeStorage {

    private final Server server;
    private final NamespacedKey storageKey;
    private final Consumer<Runnable> syncExecutor;

    public PdcRecipeKnowledgeStorage(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        this.server = plugin.getServer();
        this.storageKey = new NamespacedKey(plugin, "recipe_knowledge");
        this.syncExecutor = task -> {
            if (server.isPrimaryThread()) {
                task.run();
            } else {
                server.getScheduler().runTask(plugin, task);
            }
        };
    }

    public PdcRecipeKnowledgeStorage(Server server, NamespacedKey storageKey, Consumer<Runnable> syncExecutor) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.storageKey = Objects.requireNonNull(storageKey, "storageKey cannot be null");
        this.syncExecutor = Objects.requireNonNull(syncExecutor, "syncExecutor cannot be null");
    }

    @Override
    public CompletableFuture<Map<Key, PlayerRecipeKnowledge>> loadKnowledge(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        CompletableFuture<Map<Key, PlayerRecipeKnowledge>> future = new CompletableFuture<>();

        syncExecutor.accept(() -> {
            Player player = server.getPlayer(playerId);
            if (player == null) {
                future.complete(Map.of());
                return;
            }

            PersistentDataContainer pdc = player.getPersistentDataContainer();
            byte[] bytes = pdc.get(storageKey, PersistentDataType.BYTE_ARRAY);
            if (bytes == null || bytes.length == 0) {
                future.complete(Map.of());
                return;
            }

            try {
                Map<Key, PlayerRecipeKnowledge> map = RecipeKnowledgeBinaryCodec.deserialize(bytes, playerId);
                future.complete(map);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> saveKnowledge(UUID playerId, PlayerRecipeKnowledge knowledge) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(knowledge, "knowledge cannot be null");

        return loadKnowledge(playerId).thenCompose(existing -> {
            Map<Key, PlayerRecipeKnowledge> updated = new HashMap<>(existing);
            updated.put(knowledge.recipeKey(), knowledge);
            return saveAllKnowledge(playerId, updated.values());
        });
    }

    @Override
    public CompletableFuture<Void> saveAllKnowledge(UUID playerId, Collection<PlayerRecipeKnowledge> knowledge) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        CompletableFuture<Void> future = new CompletableFuture<>();

        byte[] bytes = RecipeKnowledgeBinaryCodec.serialize(knowledge);

        syncExecutor.accept(() -> {
            Player player = server.getPlayer(playerId);
            if (player != null) {
                PersistentDataContainer pdc = player.getPersistentDataContainer();
                if (bytes.length > 0) {
                    pdc.set(storageKey, PersistentDataType.BYTE_ARRAY, bytes);
                } else {
                    pdc.remove(storageKey);
                }
            }
            future.complete(null);
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> deleteKnowledge(UUID playerId, Key recipeKey) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(recipeKey, "recipeKey cannot be null");

        return loadKnowledge(playerId).thenCompose(existing -> {
            if (!existing.containsKey(recipeKey)) {
                return CompletableFuture.completedFuture(null);
            }
            Map<Key, PlayerRecipeKnowledge> updated = new HashMap<>(existing);
            updated.remove(recipeKey);
            return saveAllKnowledge(playerId, updated.values());
        });
    }

    @Override
    public CompletableFuture<Void> clearKnowledge(UUID playerId) {
        return saveAllKnowledge(playerId, Collections.emptyList());
    }
}
