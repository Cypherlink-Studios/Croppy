package dev.darkblade.croppy.integration.common;

import dev.darkblade.croppy.api.protection.ProtectionAction;
import dev.darkblade.croppy.api.protection.ProtectionPort;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.UUID;

/**
 * Fallback pass-through protection adapter that permits all gameplay actions unconditionally.
 */
public final class NoOpProtectionAdapter implements ProtectionPort {

    public static final NoOpProtectionAdapter INSTANCE = new NoOpProtectionAdapter();

    @Override
    public boolean canInteract(UUID playerId, BlockPosition position, ProtectionAction action) {
        return true;
    }
}
