package dev.darkblade.croppy.platform.paper.update.listener;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.update.SemanticVersion;
import dev.darkblade.croppy.api.update.UpdateApplyResult;
import dev.darkblade.croppy.api.update.UpdateChannel;
import dev.darkblade.croppy.api.update.UpdateCheckResult;
import dev.darkblade.croppy.api.update.UpdateService;
import dev.darkblade.croppy.core.i18n.DefaultMessageCatalog;
import dev.darkblade.croppy.core.i18n.YamlLocaleLoader;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateNotificationListenerTest {

    private PaperMessageService messageService;
    private List<String> playerMessages;

    @BeforeEach
    void setUp() throws Exception {
        playerMessages = new ArrayList<>();
        DefaultMessageCatalog catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);
        YamlLocaleLoader loader = new YamlLocaleLoader();
        try (InputStream in = getClass().getResourceAsStream("/locales/en-US.yml")) {
            if (in != null) {
                catalog.registerLocale(LocaleRef.EN_US, loader.load(in, "en-US.yml"));
            }
        }
        messageService = new PaperMessageService(catalog, null);
    }

    private Player createMockPlayer(boolean hasPermission, boolean online) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("hasPermission")) {
                        return hasPermission;
                    }
                    if (method.getName().equals("isOnline")) {
                        return online;
                    }
                    if (method.getName().equals("locale")) {
                        return java.util.Locale.US;
                    }
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component comp) {
                            playerMessages.add(PlainTextComponentSerializer.plainText().serialize(comp));
                        } else if (args[0] != null) {
                            playerMessages.add(args[0].toString());
                        }
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Should notify admin player on join when update is available")
    void testNotifyAdminOnJoin() {
        UpdateService updateService = new UpdateService() {
            @Override
            public CompletableFuture<UpdateCheckResult> checkForUpdatesAsync(boolean forceCheck) {
                return CompletableFuture.completedFuture(
                        UpdateCheckResult.available(
                                SemanticVersion.of(0, 5, 13),
                                SemanticVersion.of(0, 5, 14),
                                "v0.5.14",
                                "https://github.com",
                                "http://example.com/jar",
                                "http://example.com/checksum",
                                "2026-11-08"
                        )
                );
            }

            @Override
            public CompletableFuture<UpdateApplyResult> applyUpdateAsync() {
                return CompletableFuture.completedFuture(UpdateApplyResult.alreadyUpToDate("none"));
            }

            @Override
            public Optional<UpdateCheckResult> getCachedResult() {
                return Optional.of(UpdateCheckResult.available(
                        SemanticVersion.of(0, 5, 13),
                        SemanticVersion.of(0, 5, 14),
                        "v0.5.14",
                        "https://github.com",
                        "http://example.com/jar",
                        "http://example.com/checksum",
                        "2026-11-08"
                ));
            }

            @Override
            public UpdateChannel getChannel() {
                return UpdateChannel.STABLE;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        UpdateNotificationListener listener = new UpdateNotificationListener(updateService, messageService);
        Player admin = createMockPlayer(true, true);
        listener.onPlayerJoin(new PlayerJoinEvent(admin, (Component) null));

        assertTrue(playerMessages.stream().anyMatch(m -> m.contains("0.5.14")));
    }

    @Test
    @DisplayName("Should not notify player without permission")
    void testDoNotNotifyNonAdmin() {
        UpdateService updateService = new UpdateService() {
            @Override
            public CompletableFuture<UpdateCheckResult> checkForUpdatesAsync(boolean forceCheck) {
                return CompletableFuture.completedFuture(UpdateCheckResult.upToDate(SemanticVersion.of(0, 5, 13)));
            }

            @Override
            public CompletableFuture<UpdateApplyResult> applyUpdateAsync() {
                return CompletableFuture.completedFuture(UpdateApplyResult.alreadyUpToDate("none"));
            }

            @Override
            public Optional<UpdateCheckResult> getCachedResult() {
                return Optional.of(UpdateCheckResult.available(
                        SemanticVersion.of(0, 5, 13),
                        SemanticVersion.of(0, 5, 14),
                        "v0.5.14",
                        "https://github.com",
                        "http://example.com/jar",
                        "http://example.com/checksum",
                        "2026-11-08"
                ));
            }

            @Override
            public UpdateChannel getChannel() {
                return UpdateChannel.STABLE;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        UpdateNotificationListener listener = new UpdateNotificationListener(updateService, messageService);
        Player normalPlayer = createMockPlayer(false, true);
        listener.onPlayerJoin(new PlayerJoinEvent(normalPlayer, (Component) null));

        assertTrue(playerMessages.isEmpty());
    }
}
