package dev.darkblade.croppy.api.addon;

import java.util.Collection;
import java.util.Optional;

/**
 * Port contract for managing addon lifecycle, state transitions, and discovery.
 */
public interface AddonManager {

    /**
     * Discovers all addons on disk.
     */
    void discoverAddons();

    /**
     * Retrieves an addon description by its unique ID.
     *
     * @param addonId the addon identifier
     * @return optional containing the description if present
     */
    Optional<AddonDescription> getAddon(String addonId);

    /**
     * Retrieves the current lifecycle state of an addon.
     *
     * @param addonId the addon identifier
     * @return the current state
     */
    AddonState getState(String addonId);

    /**
     * @return unmodifiable collection of all discovered addon descriptions
     */
    Collection<AddonDescription> getDiscoveredAddons();

    /**
     * Loads an addon into memory (parsing manifest and linking ClassLoader).
     *
     * @param addonId the addon identifier
     * @return true if loaded successfully
     */
    boolean loadAddon(String addonId);

    /**
     * Enables an addon (invoking onEnable and activating domain content).
     *
     * @param addonId the addon identifier
     * @return true if enabled successfully
     */
    boolean enableAddon(String addonId);

    /**
     * Disables an addon (invoking onDisable and deactivating resources).
     *
     * @param addonId the addon identifier
     * @return true if disabled successfully
     */
    boolean disableAddon(String addonId);

    /**
     * Unloads an addon (disabling if needed, tearing down ClassLoader and Metaspace references).
     *
     * @param addonId the addon identifier
     * @return true if unloaded successfully
     */
    boolean unloadAddon(String addonId);
}
