package dev.darkblade.croppy.platform.paper.update.listener;

import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.update.SemanticVersion;
import dev.darkblade.croppy.api.update.UpdateCheckResult;
import dev.darkblade.croppy.api.update.UpdateService;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Bukkit listener that notifies operators with permission croppy.admin.update
 * upon server login if a newer release of Croppy is available.
 */
public class UpdateNotificationListener implements Listener {

    private final UpdateService updateService;
    private final PaperMessageService messageService;

    public UpdateNotificationListener(UpdateService updateService, PaperMessageService messageService) {
        this.updateService = Objects.requireNonNull(updateService, "updateService cannot be null");
        this.messageService = messageService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateService.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("croppy.admin.update")) {
            return;
        }

        // Use cached result if available to avoid firing remote requests on every join
        Optional<UpdateCheckResult> cached = updateService.getCachedResult();
        if (cached.isPresent()) {
            notifyIfAvailable(player, cached.get());
        } else {
            updateService.checkForUpdatesAsync(false).thenAccept(result -> notifyIfAvailable(player, result));
        }
    }

    private void notifyIfAvailable(Player player, UpdateCheckResult result) {
        if (result != null && result.updateAvailable() && player.isOnline()) {
            String latest = result.latestVersion().map(SemanticVersion::asString).orElse("?");
            String current = result.currentVersion().asString();

            if (messageService != null) {
                messageService.sendPrefixedMessage(
                        player,
                        MessageKey.of("command.update.login-notification"),
                        PaperMessageService.rawArgs("latest", latest, "current", current),
                        PaperMessageService.args("latest", latest, "current", current)
                );
            }
        }
    }
}
