package dev.darkblade.croppy.integration.vault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VaultIntegration Lifecycle Tests")
class VaultIntegrationTest {

    @Test
    @DisplayName("VaultIntegration respects disabled config and returns correct id")
    void testDisabledConfig() {
        Logger logger = Logger.getLogger("test");
        VaultIntegration integration = new VaultIntegration(logger, false);

        assertEquals("vault", integration.id());
        assertFalse(integration.isAvailable());
        assertNull(integration.getEconomyAdapter());

        assertDoesNotThrow(integration::onLoad);
        assertDoesNotThrow(integration::onEnable);
        assertDoesNotThrow(integration::onDisable);
    }
}
