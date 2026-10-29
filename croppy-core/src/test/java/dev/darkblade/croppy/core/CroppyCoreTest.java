package dev.darkblade.croppy.core;

import dev.darkblade.croppy.api.registry.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CroppyCoreTest {

    private CroppyCore core;

    private static class DummyService implements Service {
        final String status = "OK";
    }

    @BeforeEach
    void setUp() {
        core = new CroppyCore();
    }

    @Test
    @DisplayName("Lifecycle initialize and shutdown transitions")
    void testLifecycleTransitions() {
        assertFalse(core.isActive(), "Core should initially be inactive");

        core.initialize();
        assertTrue(core.isActive(), "Core should be active after initialize");

        assertThrows(IllegalStateException.class, () -> core.initialize(), "Double initialize must throw IllegalStateException");

        core.shutdown();
        assertFalse(core.isActive(), "Core should be inactive after shutdown");
    }

    @Test
    @DisplayName("Service registration and retrieval within container")
    void testServiceContainer() {
        core.initialize();
        DummyService service = new DummyService();
        core.registerService(DummyService.class, service);

        Optional<DummyService> retrieved = core.getService(DummyService.class);
        assertTrue(retrieved.isPresent());
        assertEquals("OK", retrieved.get().status);

        core.shutdown();
        assertTrue(core.getService(DummyService.class).isEmpty(), "Services should be cleared on shutdown");
    }
}
