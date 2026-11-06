package dev.darkblade.croppy.integration.common;

/**
 * Common lifecycle contract for external platform integrations.
 */
public interface CroppyIntegration {

    /**
     * Returns the unique identifier for this integration (e.g. "worldguard", "papi", "vault").
     *
     * @return lowercase string identifier
     */
    String id();

    /**
     * Determines whether the external dependency is present and enabled in server configuration.
     *
     * @return true if available for activation
     */
    boolean isAvailable();

    /**
     * Early initialization hook called during server/plugin onLoad phase.
     */
    default void onLoad() {}

    /**
     * Activation hook called during server/plugin onEnable phase.
     */
    default void onEnable() {}

    /**
     * Teardown hook called during server/plugin onDisable phase.
     */
    default void onDisable() {}
}
