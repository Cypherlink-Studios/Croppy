package dev.darkblade.croppy.integration.papi;

import dev.darkblade.croppy.core.CroppyCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PapiIntegration Lifecycle Tests")
class PapiIntegrationTest {

    @Test
    @DisplayName("PapiIntegration respects disabled config and returns correct id")
    void testDisabledConfig() {
        Logger logger = Logger.getLogger("test");
        CroppyCore core = new CroppyCore(logger);
        PapiIntegration integration = new PapiIntegration(logger, core, "0.5.4", false, 5);

        assertEquals("papi", integration.id());
        assertFalse(integration.isAvailable());
        assertNull(integration.getExpansion());

        assertDoesNotThrow(integration::onLoad);
        assertDoesNotThrow(integration::onEnable);
        assertDoesNotThrow(integration::onDisable);
    }
}
