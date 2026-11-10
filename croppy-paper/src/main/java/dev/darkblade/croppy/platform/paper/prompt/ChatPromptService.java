package dev.darkblade.croppy.platform.paper.prompt;

import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
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

    private static final String DEFAULT_PREFIX = "<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>";

    private final Plugin plugin;
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage;
    private final Map<UUID, PromptSession> activePrompts = new ConcurrentHashMap<>();

    public enum PromptType {
        TEMPERATURE,
        TEXT_SEARCH,
        ITEM_SEARCH
    }

    public record PromptSession(
            UUID playerId,
            PromptType type,
            double minAllowed,
            double maxAllowed,
            Consumer<Double> onSuccessOnMainThread,
            Consumer<String> onTextSuccess,
            Runnable onCancelOnMainThread,
            long expiryTimestampMillis,
            BukkitTask timeoutTask
    ) {
        public PromptSession(
                UUID playerId,
                double minAllowed,
                double maxAllowed,
                Consumer<Double> onSuccessOnMainThread,
                Runnable onCancelOnMainThread,
                long expiryTimestampMillis,
                BukkitTask timeoutTask
        ) {
            this(playerId, PromptType.TEMPERATURE, minAllowed, maxAllowed, onSuccessOnMainThread, null, onCancelOnMainThread, expiryTimestampMillis, timeoutTask);
        }
    }

    public ChatPromptService(Plugin plugin, PaperMessageService messageService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.messageService = messageService;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public ChatPromptService(Plugin plugin) {
        this(plugin, null);
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

        if (messageService != null) {
            messageService.sendPrefixedMessage(player, MessageKey.of("prompt.temperature-prompt"), PaperMessageService.args("min", minAllowed, "max", maxAllowed));
        } else {
            String promptMsg = DEFAULT_PREFIX + "<yellow>Enter target temperature (</yellow><white>"
                    + minAllowed + "°C - " + maxAllowed + "°C</white><yellow>) in chat, or type </yellow><red>'cancel'</red><yellow>:</yellow>";
            player.sendMessage(miniMessage.deserialize(promptMsg));
        }

        long expiryMillis = System.currentTimeMillis() + 30_000L;
        BukkitTask timeoutTask = null;
        if (plugin.isEnabled()) {
            timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PromptSession session = activePrompts.remove(playerId);
                if (session != null) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && p.isOnline()) {
                        if (messageService != null) {
                            messageService.sendPrefixedMessage(p, MessageKey.of("prompt.temperature-timeout"));
                        } else {
                            p.sendMessage(miniMessage.deserialize(DEFAULT_PREFIX + "<gray>Temperature selection timed out.</gray>"));
                        }
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
                playerId, PromptType.TEMPERATURE, minAllowed, maxAllowed, onSuccess, null, onCancel, expiryMillis, timeoutTask
        ));
    }

    /**
     * Starts a recipe search text prompt for the player.
     *
     * @param player        the player
     * @param onSearchQuery callback executed on main thread on valid search query (null or empty string clears query)
     * @param onCancel      callback executed on main thread on cancel or timeout
     */
    public void startRecipeSearchPrompt(
            Player player,
            Consumer<String> onSearchQuery,
            Runnable onCancel
    ) {
        startSearchPromptInternal(player, PromptType.TEXT_SEARCH, "prompt.search-prompt", "/croppy recipes clear", "recipe", "prompt.search-timeout", onSearchQuery, onCancel);
    }

    /**
     * Starts an item catalog search text prompt for the player.
     *
     * @param player        the player
     * @param onSearchQuery callback executed on main thread on valid search query (null or empty string clears query)
     * @param onCancel      callback executed on main thread on cancel or timeout
     */
    public void startItemSearchPrompt(
            Player player,
            Consumer<String> onSearchQuery,
            Runnable onCancel
    ) {
        startSearchPromptInternal(player, PromptType.ITEM_SEARCH, "prompt.item-search-prompt", "/croppy items clear", "item", "prompt.item-search-timeout", onSearchQuery, onCancel);
    }

    /**
     * Starts a general search text prompt for the player (defaults to recipe search).
     *
     * @param player        the player
     * @param onSearchQuery callback executed on main thread on valid search query (null or empty string clears query)
     * @param onCancel      callback executed on main thread on cancel or timeout
     */
    public void startSearchPrompt(
            Player player,
            Consumer<String> onSearchQuery,
            Runnable onCancel
    ) {
        startRecipeSearchPrompt(player, onSearchQuery, onCancel);
    }

    private void startSearchPromptInternal(
            Player player,
            PromptType type,
            String messageKey,
            String clearCommand,
            String itemOrRecipe,
            String timeoutKey,
            Consumer<String> onSearchQuery,
            Runnable onCancel
    ) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(onSearchQuery, "onSearchQuery cannot be null");

        UUID playerId = player.getUniqueId();
        cancelPrompt(playerId, false);

        player.closeInventory();

        if (messageService != null) {
            messageService.sendPrefixedMessage(player, MessageKey.of(messageKey));
        } else {
            String promptMsg = DEFAULT_PREFIX + "<yellow>Enter " + itemOrRecipe + " search query in chat, or type </yellow><click:suggest_command:cancel><hover:show_text:'<gray>Click to cancel'><red>'cancel'</red></hover></click><yellow> to abort (or </yellow><click:run_command:'" + clearCommand + "'><hover:show_text:'<gray>Click to clear filter'><aqua>'clear'</aqua></hover></click><yellow> to reset):</yellow>";
            player.sendMessage(miniMessage.deserialize(promptMsg));
        }

        long expiryMillis = System.currentTimeMillis() + 30_000L;
        BukkitTask timeoutTask = null;
        if (plugin.isEnabled()) {
            timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PromptSession session = activePrompts.remove(playerId);
                if (session != null) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && p.isOnline()) {
                        if (messageService != null) {
                            messageService.sendPrefixedMessage(p, MessageKey.of(timeoutKey));
                        } else {
                            p.sendMessage(miniMessage.deserialize(DEFAULT_PREFIX + "<gray>" + Character.toUpperCase(itemOrRecipe.charAt(0)) + itemOrRecipe.substring(1) + " search timed out.</gray>"));
                        }
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
                playerId, type, 0.0, 0.0, null, onSearchQuery, onCancel, expiryMillis, timeoutTask
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

        if (session.type() == PromptType.TEXT_SEARCH || session.type() == PromptType.ITEM_SEARCH) {
            if ("cancel".equalsIgnoreCase(trimmed) || "cancelar".equalsIgnoreCase(trimmed)) {
                activePrompts.remove(playerId);
                if (session.timeoutTask() != null) {
                    session.timeoutTask().cancel();
                }
                String cancelKey = (session.type() == PromptType.ITEM_SEARCH) ? "prompt.item-search-cancelled" : "prompt.search-cancelled";
                String fallbackMsg = (session.type() == PromptType.ITEM_SEARCH) ? "<gray>Item search cancelled.</gray>" : "<gray>Recipe search cancelled.</gray>";
                if (messageService != null) {
                    messageService.sendPrefixedMessage(player, MessageKey.of(cancelKey));
                } else {
                    player.sendMessage(miniMessage.deserialize(DEFAULT_PREFIX + fallbackMsg));
                }
                try {
                    player.playSound(player.getLocation(), "block.note_block.bass", 1.0f, 0.8f);
                } catch (Exception ignored) {}
                if (session.onCancelOnMainThread() != null) {
                    scheduleOnMain(session.onCancelOnMainThread()::run);
                }
                return true;
            }

            activePrompts.remove(playerId);
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }

            if ("clear".equalsIgnoreCase(trimmed) || "limpiar".equalsIgnoreCase(trimmed) || "reset".equalsIgnoreCase(trimmed)) {
                try {
                    player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.2f);
                } catch (Exception ignored) {}
                if (session.onTextSuccess() != null) {
                    scheduleOnMain(() -> session.onTextSuccess().accept(null));
                }
                return true;
            }

            try {
                player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.2f);
            } catch (Exception ignored) {}
            if (session.onTextSuccess() != null) {
                scheduleOnMain(() -> session.onTextSuccess().accept(trimmed));
            }
            return true;
        }
        if ("cancel".equalsIgnoreCase(trimmed) || "cancelar".equalsIgnoreCase(trimmed)) {
            activePrompts.remove(playerId);
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, MessageKey.of("prompt.temperature-cancelled"));
            } else {
                player.sendMessage(miniMessage.deserialize(DEFAULT_PREFIX + "<gray>Temperature selection cancelled.</gray>"));
            }
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
                if (messageService != null) {
                    messageService.sendPrefixedMessage(player, MessageKey.of("prompt.temperature-bounds"), PaperMessageService.args("min", session.minAllowed(), "max", session.maxAllowed()));
                } else {
                    String errorMsg = DEFAULT_PREFIX + "<red>Temperature must be between "
                            + session.minAllowed() + "°C and " + session.maxAllowed() + "°C.</red>";
                    player.sendMessage(miniMessage.deserialize(errorMsg));
                }
                return true;
            }

            activePrompts.remove(playerId);
            if (session.timeoutTask() != null) {
                session.timeoutTask().cancel();
            }

            if (messageService != null) {
                messageService.sendPrefixedMessage(player, MessageKey.of("prompt.temperature-success"), PaperMessageService.arg("val", val));
            } else {
                String successMsg = DEFAULT_PREFIX + "<green>Target temperature set to <aqua>" + val + "°C</aqua>!</green>";
                player.sendMessage(miniMessage.deserialize(successMsg));
            }
            try {
                player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.2f);
            } catch (Exception ignored) {}

            scheduleOnMain(() -> session.onSuccessOnMainThread().accept(val));
            return true;
        } catch (NumberFormatException e) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(player, MessageKey.of("prompt.invalid-number"));
            } else {
                player.sendMessage(miniMessage.deserialize(DEFAULT_PREFIX + "<red>Invalid number. Type a valid number or 'cancel'.</red>"));
            }
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
