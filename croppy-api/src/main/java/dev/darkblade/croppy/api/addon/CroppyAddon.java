package dev.darkblade.croppy.api.addon;

/**
 * Lifecycle contract for Java code addons extending Croppy.
 */
public interface CroppyAddon {

    /**
     * Called when the addon is enabled.
     *
     * @param context the scoped addon context
     */
    void onEnable(AddonContext context);

    /**
     * Called when the addon is disabled or unloaded.
     * Implementations should release any internal custom resources.
     *
     * @param context the scoped addon context
     */
    default void onDisable(AddonContext context) {}
}
