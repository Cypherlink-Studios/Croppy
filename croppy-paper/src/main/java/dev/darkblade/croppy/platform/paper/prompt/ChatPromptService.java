package dev.darkblade.croppy.platform.paper.prompt;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service managing asynchronous player chat input prompts with bounds validation,
 * automatic expiration, cancellation, and main-thread callback execution.
 */
public final class ChatPromptService implements Listener {

    private final Plugin plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, PromptSession> activePrompts = new ConcurrentHashMap<>();

    public record PromptSession(
            UUID playerId,
            double minAllowed,
            double maxAllowed,
            Consumer<Double> onSuccessOnMainThread,
            Runnable onCancelOnMainThread,
            long expiryTimestampMillis,
            BukkitTask timeoutTask
    ) {}

    public ChatPromptService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Starts a temperature input prompt for the player.
     *
     * @param player     the player
     * @param minAllowed minimum allowed temperature
     * @param maxAllowed maximum allowed temperature
     * @param onSuccess  callback executed on main thread on valid input
     * @param onCancel   callback executed on main thread on cancel or timeout
     */
    public void startTemperaturePrompt(
            Player player,
            double minAllowed,
            double maxAllowed,
            Consumer<Double> onSuccess,
            Runnable onCancel
    ) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(onSuccess, "onSuccess cannot be null");

        UUID playerId = player.getUniqueId();
        cancelPrompt(playerId, false);

        player.closeInventory();

        String promptMsg = "<gold>[Croppy]</gold> <yellow>Enter target temperature (</yellow><white>"
                + minAllowed + "°C - " + maxAllowed + "°C</white><yellow>) in chat, or type </yellow><red>'cancel'</red><yellow>:</yellow>";
        player.sendMessage(miniMessage.deserialize(promptMsg));

        long expiryMillis = System.currentTimeMillis() + 30_000L;
        BukkitTask timeoutTask = null;
        if (plugin.isEnabled()) {
            timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PromptSession session = activePrompts.remove(playerId);
                if (session != null) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(miniMessage.deserialize("<gold>[Croppy]</gold> <gray>Temperature selection timed out.</gray>"));
                        try {
                            p.playSound(p.getLocation(), "block.note_block.bass", 1.0f, 0.8f);
                        } catch (Exception ignored) {}
                    }
                    if (onCancel != null) {
                        onCancel.run();
                    }
                }
            }, 600L); // 30 seconds = 600 ticks
        }

        activePrompts.put(playerId, new PromptSession(
                playerId, minAllowed, maxAllowed, onSuccess, onCancel, expiryMillis, timeoutTask
        ));
    }

    /**
     * Processes input string directly (useful for tests or programmatic invocation).
     *
     * @param player the player
     * @param input  raw text input
     * @return true if input was handled by an active prompt
     */
    public boolean processInput(Player player, String input) {
        if (player == null || input == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        PromptSession session = activePrompts.get(playerId);
        if (session == null) {
            return false;
        }

        if (System.currentTimeMillis() > session.expiryTimestampMillis()) {
            activePrompts.remove(playerId);
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }
            return false;
        }

        String trimmed = input.trim();
        if ("cancel".equalsIgnoreCase(trimmed) || "cancelar".equalsIgnoreCase(trimmed)) {
            activePrompts.remove(playerId);
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }
            player.sendMessage(miniMessage.deserialize("<gold>[Croppy]</gold> <gray>Temperature selection cancelled.</gray>"));
            try {
                player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 0.8f);
            } catch (Exception ignored) {}
            if (session.onCancelOnMainThread() != null) {
                scheduleOnMain(session.onCancelOnMainThread()::run);
            }
            return true;
        }

        try {
            double val = Double.parseDouble(trimmed);
            if (val < session.minAllowed() || val > session.maxAllowed()) {
                String errorMsg = "<gold>[Croppy]</gold> <red>Temperature must be between "
                        + session.minAllowed() + "°C and " + session.maxAllowed() + "°C.</red>";
                player.sendMessage(miniMessage.deserialize(errorMsg));
                return true;
            }

            activePrompts.remove(playerId);
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }

            String successMsg = "<gold>[Croppy]</gold> <green>Target temperature set to <aqua>" + val + "°C</aqua>!</green>";
            player.sendMessage(miniMessage.deserialize(successMsg));
            try {
                player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.2f);
            } catch (Exception ignored) {}

            scheduleOnMain(() -> session.onSuccessOnMainThread().accept(val));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(miniMessage.deserialize("<gold>[Croppy]</gold> <red>Invalid number. Type a valid number or 'cancel'.</red>"));
            return true;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!activePrompts.containsKey(playerId)) {
            return;
        }

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        processInput(player, text);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelPrompt(event.getPlayer().getUniqueId(), true);
    }

    /**
     * Cancels an active prompt for a player.
     *
     * @param playerId player UUID
     * @param notify   whether to execute cancel callback
     */
    public void cancelPrompt(UUID playerId, boolean notify) {
        if (playerId == null) {
            return;
        }
        PromptSession session = activePrompts.remove(playerId);
        if (session != null) {
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }
            if (notify && session.onCancelOnMainThread() != null) {
                scheduleOnMain(session.onCancelOnMainThread()::run);
            }
        }
    }

    public boolean hasActivePrompt(UUID playerId) {
        return playerId != null && activePrompts.containsKey(playerId);
    }

    private void scheduleOnMain(Runnable task) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
    }
}
