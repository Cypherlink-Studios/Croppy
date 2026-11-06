package dev.darkblade.croppy.integration.common;

import dev.darkblade.croppy.api.protection.ProtectionAction;
import dev.darkblade.croppy.api.world.BlockPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Common Integration & No-Op Adapter Tests")
class NoOpAdaptersTest {

    @Test
    @DisplayName("NoOpProtectionAdapter permits all actions unconditionally")
    void testNoOpProtectionAdapter() {
        NoOpProtectionAdapter adapter = NoOpProtectionAdapter.INSTANCE;
        UUID player = UUID.randomUUID();
        BlockPosition pos = new BlockPosition("world", 10, 64, 10);

        for (ProtectionAction action : ProtectionAction.values()) {
            assertTrue(adapter.canInteract(player, pos, action), "Should permit action: " + action);
        }
    }

    @Test
    @DisplayName("NoOpEconomyAdapter rejects all withdrawals and has checks")
    void testNoOpEconomyAdapter() {
        NoOpEconomyAdapter adapter = NoOpEconomyAdapter.INSTANCE;
        UUID player = UUID.randomUUID();

        assertFalse(adapter.has(player, 10.0));
        assertFalse(adapter.withdraw(player, 10.0));
        assertFalse(adapter.deposit(player, 10.0));
        assertEquals("$25.50", adapter.format(25.5));
    }

    @Test
    @DisplayName("CroppyIntegration default methods do not throw exceptions")
    void testCroppyIntegrationDefaults() {
        CroppyIntegration integration = new CroppyIntegration() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        assertEquals("test", integration.id());
        assertTrue(integration.isAvailable());
        assertDoesNotThrow(integration::onLoad);
        assertDoesNotThrow(integration::onEnable);
        assertDoesNotThrow(integration::onDisable);
    }
}
