package dev.darkblade.croppy.integration.worldguard;

import dev.darkblade.croppy.api.protection.ProtectionAction;
import dev.darkblade.croppy.api.world.BlockPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WorldGuardProtectionAdapter Unit Tests")
class WorldGuardProtectionAdapterTest {

    @Test
    @DisplayName("canInteract validates non-null parameters")
    void testParameterValidation() {
        WorldGuardProtectionAdapter adapter = new WorldGuardProtectionAdapter(true);
        UUID player = UUID.randomUUID();
        BlockPosition pos = new BlockPosition("world", 0, 64, 0);

        assertThrows(NullPointerException.class, () -> adapter.canInteract(null, pos, ProtectionAction.HARVEST));
        assertThrows(NullPointerException.class, () -> adapter.canInteract(player, null, ProtectionAction.HARVEST));
        assertThrows(NullPointerException.class, () -> adapter.canInteract(player, pos, null));
    }

    @Test
    @DisplayName("canInteract permits actions when world is not loaded in mock environment")
    void testUnloadedWorldFallback() {
        WorldGuardProtectionAdapter adapter = new WorldGuardProtectionAdapter(true);
        UUID player = UUID.randomUUID();
        BlockPosition pos = new BlockPosition("unloaded_world", 10, 64, 10);

        assertTrue(adapter.canInteract(player, pos, ProtectionAction.PLANT));
    }
}
