package dev.darkblade.croppy.integration.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IntegrationManager Unit Tests")
class IntegrationManagerTest {

    @Test
    @DisplayName("IntegrationManager registers and dispatches lifecycle events properly")
    void testLifecycleDispatch() {
        Logger logger = Logger.getLogger("test");
        IntegrationManager manager = new IntegrationManager(logger);

        AtomicBoolean loaded = new AtomicBoolean(false);
        AtomicBoolean enabled = new AtomicBoolean(false);
        AtomicBoolean disabled = new AtomicBoolean(false);

        CroppyIntegration testIntegration = new CroppyIntegration() {
            @Override
            public String id() {
                return "test-plugin";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public void onLoad() {
                loaded.set(true);
            }

            @Override
            public void onEnable() {
                enabled.set(true);
            }

            @Override
            public void onDisable() {
                disabled.set(true);
            }
        };

        manager.register(testIntegration);
        assertTrue(manager.getIntegration("test-plugin").isPresent());
        assertEquals(1, manager.getAllIntegrations().size());
        assertFalse(manager.isActive("test-plugin"));

        manager.onLoad();
        assertTrue(loaded.get());

        manager.onEnable();
        assertTrue(enabled.get());
        assertTrue(manager.isActive("test-plugin"));

        manager.onDisable();
        assertTrue(disabled.get());
        assertFalse(manager.isActive("test-plugin"));
    }

    @Test
    @DisplayName("Unavailable integrations are not loaded or enabled")
    void testUnavailableIntegration() {
        Logger logger = Logger.getLogger("test");
        IntegrationManager manager = new IntegrationManager(logger);

        AtomicBoolean enabled = new AtomicBoolean(false);

        CroppyIntegration unavailable = new CroppyIntegration() {
            @Override
            public String id() {
                return "unavailable";
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public void onEnable() {
                enabled.set(true);
            }
        };

        manager.register(unavailable);
        manager.onLoad();
        manager.onEnable();

        assertFalse(enabled.get());
        assertFalse(manager.isActive("unavailable"));
    }
}
