package dev.darkblade.croppy.platform.paper.effect;

import dev.darkblade.croppy.api.effect.ActiveStatusEffect;
import dev.darkblade.croppy.api.effect.PlayerEffectService;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Opt-in chat slurring listener triggered when a player's alcohol intoxication
 * reaches the drunk threshold and {@code consumables.alcohol.chat-slurring} is enabled.
 */
public class AlcoholChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final PlayerEffectService effectService;
    private final PluginConfig config;

    public AlcoholChatListener(PlayerEffectService effectService, PluginConfig config) {
        this.effectService = Objects.requireNonNull(effectService, "effectService cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!config.isAlcoholChatSlurring()) {
            return;
        }
        Optional<ActiveStatusEffect> activeOpt = effectService.getActiveEffect(
                event.getPlayer().getUniqueId(), PaperIntoxicationEffect.KEY);
        if (activeOpt.isEmpty()) {
            return;
        }
        double intensity = activeOpt.get().intensity();
        if (intensity < config.getAlcoholDrunkThreshold()) {
            return;
        }

        String plain = PLAIN.serialize(event.message());
        String slurred = slurMessage(plain, intensity, ThreadLocalRandom.current());
        if (!slurred.equals(plain)) {
            event.message(Component.text(slurred));
        }
    }

    public static String slurMessage(String message, double intensity, Random rng) {
        if (message == null || message.isEmpty() || rng == null) {
            return message;
        }
        double probability = Math.min(0.65, Math.max(0.15, (intensity - 35.0) / 100.0));
        StringBuilder out = new StringBuilder(message.length() + 8);
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            char lower = Character.toLowerCase(c);
            if ((lower == 's' || lower == 'z') && rng.nextDouble() < probability) {
                out.append(Character.isUpperCase(c) ? "Sh" : "sh");
            } else if ((lower == 'a' || lower == 'o' || lower == 'e') && rng.nextDouble() < probability * 0.35) {
                out.append(c).append(lower);
            } else {
                out.append(c);
            }
        }
        if (intensity >= 70.0 && rng.nextDouble() < 0.35) {
            out.append(" *hic*");
        }
        return out.toString();
    }
}
