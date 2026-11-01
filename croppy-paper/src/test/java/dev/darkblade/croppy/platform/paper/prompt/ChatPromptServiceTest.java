package dev.darkblade.croppy.platform.paper.prompt;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChatPromptServiceTest {

    private Plugin createMockPlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("isEnabled".equals(method.getName())) {
                        return false;
                    }
                    if ("getName".equals(method.getName())) {
                        return "Croppy";
                    }
                    return null;
                }
        );
    }

    private Player createMockPlayer(UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return uuid;
                    }
                    if ("getLocation".equals(method.getName())) {
                        return new Location(null, 0, 0, 0);
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Valid temperature input resolves prompt and triggers success callback")
    void testSuccessfulNumericInput() {
        ChatPromptService service = new ChatPromptService(createMockPlugin());
        UUID playerId = UUID.randomUUID();
        Player player = createMockPlayer(playerId);

        AtomicReference<Double> capturedTemp = new AtomicReference<>();
        service.startTemperaturePrompt(player, 20.0, 350.0, capturedTemp::set, () -> {});

        assertTrue(service.hasActivePrompt(playerId));

        boolean handled = service.processInput(player, "185.5");
        assertTrue(handled);
        assertEquals(185.5, capturedTemp.get(), 0.001);
        assertFalse(service.hasActivePrompt(playerId));
    }

    @Test
    @DisplayName("Out of range temperature rejects input and keeps prompt active")
    void testOutOfRangeInput() {
        ChatPromptService service = new ChatPromptService(createMockPlugin());
        UUID playerId = UUID.randomUUID();
        Player player = createMockPlayer(playerId);

        AtomicReference<Double> capturedTemp = new AtomicReference<>();
        service.startTemperaturePrompt(player, 20.0, 300.0, capturedTemp::set, () -> {});

        boolean handled = service.processInput(player, "400.0");
        assertTrue(handled);
        assertNull(capturedTemp.get());
        assertTrue(service.hasActivePrompt(playerId));

        // Valid retry
        service.processInput(player, "250.0");
        assertEquals(250.0, capturedTemp.get(), 0.001);
        assertFalse(service.hasActivePrompt(playerId));
    }

    @Test
    @DisplayName("Cancel keyword aborts prompt and executes cancel callback")
    void testCancelInput() {
        ChatPromptService service = new ChatPromptService(createMockPlugin());
        UUID playerId = UUID.randomUUID();
        Player player = createMockPlayer(playerId);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        service.startTemperaturePrompt(player, 20.0, 300.0, t -> {}, () -> cancelled.set(true));

        assertTrue(service.hasActivePrompt(playerId));

        boolean handled = service.processInput(player, "cancel");
        assertTrue(handled);
        assertTrue(cancelled.get());
        assertFalse(service.hasActivePrompt(playerId));
    }

    @Test
    @DisplayName("Non-numeric text rejects input and keeps prompt open")
    void testInvalidFormat() {
        ChatPromptService service = new ChatPromptService(createMockPlugin());
        UUID playerId = UUID.randomUUID();
        Player player = createMockPlayer(playerId);

        AtomicReference<Double> capturedTemp = new AtomicReference<>();
        service.startTemperaturePrompt(player, 20.0, 300.0, capturedTemp::set, () -> {});

        boolean handled = service.processInput(player, "invalid_degrees");
        assertTrue(handled);
        assertNull(capturedTemp.get());
        assertTrue(service.hasActivePrompt(playerId));
    }
}
