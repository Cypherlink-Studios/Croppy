package dev.darkblade.croppy.integration.worldguard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorldGuardIntegration Lifecycle Tests")
class WorldGuardIntegrationTest {

    @Test
    @DisplayName("WorldGuardIntegration reports correct id and respects disabled config")
    void testDisabledConfig() {
        Logger logger = Logger.getLogger("test");
        WorldGuardIntegration integration = new WorldGuardIntegration(logger, false, false);

        assertEquals("worldguard", integration.id());
        assertFalse(integration.isAvailable());
        assertNull(integration.getProtectionAdapter());

        assertDoesNotThrow(integration::onLoad);
        assertDoesNotThrow(integration::onEnable);
        assertDoesNotThrow(integration::onDisable);
    }
}
