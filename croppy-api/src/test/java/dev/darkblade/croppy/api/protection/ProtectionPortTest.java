package dev.darkblade.croppy.api.protection;

import dev.darkblade.croppy.api.world.BlockPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProtectionPort & ProtectionAction Unit Tests")
class ProtectionPortTest {

    @Test
    @DisplayName("ProtectionAction enum values are well-defined")
    void testProtectionActionEnum() {
        assertEquals(7, ProtectionAction.values().length);
        assertNotNull(ProtectionAction.valueOf("PLANT"));
        assertNotNull(ProtectionAction.valueOf("HARVEST"));
        assertNotNull(ProtectionAction.valueOf("BREAK"));
        assertNotNull(ProtectionAction.valueOf("WATER"));
        assertNotNull(ProtectionAction.valueOf("FERTILIZE"));
        assertNotNull(ProtectionAction.valueOf("STATION_USE"));
        assertNotNull(ProtectionAction.valueOf("STATION_FUEL"));
    }

    @Test
    @DisplayName("ProtectionPort contract can be implemented and evaluated with pure types")
    void testProtectionPortContract() {
        ProtectionPort allowingPort = (playerId, position, action) -> true;
        ProtectionPort denyingPort = (playerId, position, action) -> false;

        UUID playerId = UUID.randomUUID();
        BlockPosition position = new BlockPosition("world", 100, 64, -200);

        assertTrue(allowingPort.canInteract(playerId, position, ProtectionAction.HARVEST));
        assertFalse(denyingPort.canInteract(playerId, position, ProtectionAction.PLANT));
    }
}
