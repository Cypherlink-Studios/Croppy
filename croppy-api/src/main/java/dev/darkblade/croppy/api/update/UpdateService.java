package dev.darkblade.croppy.api.update;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service port interface for checking and staging plugin updates.
 */
public interface UpdateService {

    /**
     * Checks for available updates asynchronously.
     * Uses in-memory caching if a recent check was performed within the cache TTL,
     * unless forceCheck is set to true.
     *
     * @param forceCheck whether to bypass cache and query remote releases directly
     * @return a CompletableFuture yielding the check result
     */
    CompletableFuture<UpdateCheckResult> checkForUpdatesAsync(boolean forceCheck);

    /**
     * Checks for available updates asynchronously using default caching behavior.
     *
     * @return a CompletableFuture yielding the check result
     */
    default CompletableFuture<UpdateCheckResult> checkForUpdatesAsync() {
        return checkForUpdatesAsync(false);
    }

    /**
     * Applies/stages the available update asynchronously into the platform's update directory.
     *
     * @return a CompletableFuture yielding the update application outcome
     */
    CompletableFuture<UpdateApplyResult> applyUpdateAsync();

    /**
     * Gets the most recent cached update check result, if one has been performed.
     *
     * @return an Optional containing the cached result
     */
    Optional<UpdateCheckResult> getCachedResult();

    /**
     * Gets the current configured update channel.
     *
     * @return the active UpdateChannel
     */
    UpdateChannel getChannel();

    /**
     * Checks if the update subsystem is currently enabled in configuration.
     *
     * @return true if enabled
     */
    boolean isEnabled();
}
