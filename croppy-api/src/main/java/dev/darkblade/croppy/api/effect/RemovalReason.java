package dev.darkblade.croppy.api.effect;

/**
 * Reason why an active {@link CustomStatusEffect} was removed from a player.
 */
public enum RemovalReason {
    EXPIRED,
    REMOVED,
    MILK,
    DEATH,
    DISCONNECTED,
    ADDON_UNLOADED,
    CLEARED
}
