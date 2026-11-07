package dev.darkblade.croppy.api.resourcepack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract managing resource pack compilation, state retrieval, and player delivery.
 */
public interface ResourcePackService {

    /**
     * Asynchronously compiles the unified resource pack merging all active addons.
     *
     * @return a CompletableFuture yielding the build result
     */
    CompletableFuture<PackBuildResult> buildPackAsync();

    /**
     * Gets the most recently compiled resource pack result, if available.
     *
     * @return an Optional containing the active build result
     */
    Optional<PackBuildResult> getActivePack();

    /**
     * Dispatches the active companion resource pack prompt to a specific player.
     *
     * @param playerUniqueId the unique identifier of the target player
     * @return true if the dispatch request was successfully issued, false if player is offline or pack unavailable
     */
    boolean sendPack(UUID playerUniqueId);

    /**
     * Dispatches the active companion resource pack prompt to all online players.
     *
     * @return total number of players to whom the pack prompt was dispatched
     */
    int sendPackToAll();
}
