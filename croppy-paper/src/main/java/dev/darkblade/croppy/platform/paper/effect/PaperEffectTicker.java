package dev.darkblade.croppy.platform.paper.effect;

import dev.darkblade.croppy.api.effect.ActiveStatusEffect;
import dev.darkblade.croppy.api.effect.CustomStatusEffect;
import dev.darkblade.croppy.api.effect.PlayerEffectService;
import dev.darkblade.croppy.api.effect.StatusEffectRegistry;
import dev.darkblade.croppy.api.effect.StatusEffectStage;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main-thread scheduler task driving {@link PlayerEffectService#tick(long)}
 * and optional status HUD summaries.
 */
public class PaperEffectTicker implements Runnable {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final PlayerEffectService effectService;
    private final StatusEffectRegistry statusRegistry;
    private final CroppyLogger logger;
    private volatile long intervalTicks;
    private volatile boolean hudEnabled;
    private BukkitTask task;

    public PaperEffectTicker(
            Plugin plugin,
            PlayerEffectService effectService,
            StatusEffectRegistry statusRegistry,
            long intervalTicks,
            boolean hudEnabled,
            CroppyLogger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.effectService = Objects.requireNonNull(effectService, "effectService cannot be null");
        this.statusRegistry = Objects.requireNonNull(statusRegistry, "statusRegistry cannot be null");
        this.intervalTicks = Math.max(1L, intervalTicks);
        this.hudEnabled = hudEnabled;
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    public void start() {
        stop();
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public void setIntervalTicks(long intervalTicks) {
        long clamped = Math.max(1L, intervalTicks);
        if (this.intervalTicks != clamped) {
            this.intervalTicks = clamped;
            if (this.task != null) {
                start();
            }
        }
    }

    @Override
    public void run() {
        try {
            effectService.tick(intervalTicks);
            if (hudEnabled) {
                renderHud();
            }
        } catch (Throwable t) {
            logger.warn(LogChannel.CORE, "Error ticking player status effects: " + t.getMessage());
        }
    }

    private void renderHud() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Collection<ActiveStatusEffect> active = effectService.getActiveEffects(player.getUniqueId());
            if (active.isEmpty()) {
                continue;
            }
            String summary = active.stream()
                    .map(eff -> formatEntry(eff))
                    .collect(Collectors.joining(" <dark_gray>|</dark_gray> "));
            player.sendActionBar(MM.deserialize(summary));
        }
    }

    private String formatEntry(ActiveStatusEffect eff) {
        Optional<CustomStatusEffect> defOpt = statusRegistry.get(eff.effectKey());
        String name = defOpt.map(CustomStatusEffect::getDisplayName).orElse(eff.effectKey().value());
        Optional<StatusEffectStage> stageOpt = defOpt.flatMap(d -> d.resolveStage(eff.intensity()));
        if (stageOpt.isPresent()) {
            return "<gold>" + name + "</gold> <gray>(" + stageOpt.get().displayName() + ")</gray>";
        }
        return "<aqua>" + name + "</aqua> <gray>" + eff.remainingSeconds() + "s</gray>";
    }
}
