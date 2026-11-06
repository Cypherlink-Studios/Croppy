package dev.darkblade.croppy.integration.papi;

import dev.darkblade.croppy.core.CroppyCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CroppyPlaceholderExpansion Unit Tests")
class CroppyPlaceholderExpansionTest {

    @Test
    @DisplayName("Expansion metadata matches expected values")
    void testExpansionMetadata() {
        CroppyCore core = new CroppyCore(Logger.getLogger("test"));
        core.initialize();

        CroppyPlaceholderExpansion expansion = new CroppyPlaceholderExpansion(core, "0.5.4", 5);

        assertEquals("croppy", expansion.getIdentifier());
        assertEquals("DarkBladeDev", expansion.getAuthor());
        assertEquals("0.5.4", expansion.getVersion());
        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
    }

    @Test
    @DisplayName("Global placeholders resolve accurately")
    void testGlobalPlaceholders() {
        CroppyCore core = new CroppyCore(Logger.getLogger("test"));
        core.initialize();

        CroppyPlaceholderExpansion expansion = new CroppyPlaceholderExpansion(core, "0.5.4", 5);

        assertEquals("0.5.4", expansion.onRequest(null, "version"));
        assertEquals("0", expansion.onRequest(null, "total_active_crops"));
        assertEquals("0", expansion.onRequest(null, "total_active_stations"));
        assertNull(expansion.onRequest(null, "non_existent_placeholder"));
    }
}
