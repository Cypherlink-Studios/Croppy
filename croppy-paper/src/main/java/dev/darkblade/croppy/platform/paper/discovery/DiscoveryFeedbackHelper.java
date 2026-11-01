package dev.darkblade.croppy.platform.paper.discovery;

import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;

/**
 * Default implementation of {@link DiscoveryFeedbackProvider} delivering celebratory
 * sounds, particles, and MiniMessage titles upon recipe discovery.
 */
public final class DiscoveryFeedbackHelper implements DiscoveryFeedbackProvider {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PaperMessageService messageService;

    public DiscoveryFeedbackHelper(PaperMessageService messageService) {
        this.messageService = messageService;
    }

    public DiscoveryFeedbackHelper() {
        this(null);
    }

    @Override
    public void playDiscoveryFanfare(Player player, RecipeDefinition recipe) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(recipe, "recipe cannot be null");

        Location loc = player.getLocation();

        // 1. Audio feedback (string sound identifiers safe from registry bootstrap)
        try {
            player.playSound(loc, "ui.toast.challenge_complete", 1.0f, 1.2f);
            player.playSound(loc, "entity.player.levelup", 0.6f, 1.4f);
        } catch (Throwable ignored) {}

        // 2. Particle feedback
        try {
            player.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 0.1);
            player.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 25, 0.6, 0.6, 0.6, 0.2);
        } catch (Throwable ignored) {}

        // 3. Adventure Title
        Component titleMain = miniMessage.deserialize("<gold><bold>★ NEW RECIPE! ★</bold></gold>");
        Component subtitle = miniMessage.deserialize("<white>" + recipe.displayName() + "</white>");
        Title.Times times = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(2500), Duration.ofMillis(500));
        player.showTitle(Title.title(titleMain, subtitle, times));

        // 4. Chat notification
        Component chatMsg = miniMessage.deserialize("<green>You discovered <white>" + recipe.displayName() + "</white>! View your cookbook with <yellow>/croppy recipes</yellow>.</green>");
        player.sendMessage(chatMsg);
    }

    @Override
    public void playHintFeedback(Player player, String hint) {
        Objects.requireNonNull(player, "player cannot be null");
        Location loc = player.getLocation();

        try {
            player.playSound(loc, "block.enchantment_table.use", 0.8f, 1.2f);
            player.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3, 0.05);
        } catch (Throwable ignored) {}

        Component msg = miniMessage.deserialize("<aqua><bold>Culinary Clue:</bold></aqua> <gray>" + (hint != null ? hint : "You noticed something curious...") + "</gray>");
        player.sendMessage(msg);
    }
}
