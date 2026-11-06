package dev.darkblade.croppy.api.protection;

import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.UUID;

/**
 * Port contract for querying spatial land claim and region protection permissions
 * across agricultural and culinary interactions.
 */
public interface ProtectionPort {

    /**
     * Evaluates whether a player is permitted to perform a gameplay action at a specific spatial position.
     *
     * @param playerId the unique identifier of the player attempting the action (cannot be null)
     * @param position the spatial coordinate of the target block (cannot be null)
     * @param action   the action type being attempted (cannot be null)
     * @return true if permitted by protection policies, false if denied
     */
    boolean canInteract(UUID playerId, BlockPosition position, ProtectionAction action);
}
