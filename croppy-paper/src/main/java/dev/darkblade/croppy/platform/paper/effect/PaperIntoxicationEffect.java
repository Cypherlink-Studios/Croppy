package dev.darkblade.croppy.platform.paper.effect;

import dev.darkblade.croppy.api.effect.CustomStatusEffect;
import dev.darkblade.croppy.api.effect.RemovalReason;
import dev.darkblade.croppy.api.effect.StackingPolicy;
import dev.darkblade.croppy.api.effect.StatusEffectContext;
import dev.darkblade.croppy.api.effect.StatusEffectStage;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Built-in staged alcohol intoxication status effect ({@code croppy:intoxication})
 * operating on a continuous {@code 0.0..100.0} scale with pulsed nausea, stage buffs/debuffs,
 * opt-in sway, and opt-in hangover transition.
 */
public class PaperIntoxicationEffect implements CustomStatusEffect {

    public static final Key KEY = Key.of("croppy", "intoxication");

    private final PluginConfig config;
    private final PaperMessageService messageService;
    private final Map<UUID, Double> peakIntensityByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastNauseaTickByPlayer = new ConcurrentHashMap<>();

    public PaperIntoxicationEffect(PluginConfig config, PaperMessageService messageService) {
        this.config = config;
        this.messageService = messageService;
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Intoxication";
    }

    @Override
    public StackingPolicy getStackingPolicy() {
        return StackingPolicy.STACK_INTENSITY;
    }

    @Override
    public double getMaxIntensity() {
        return 100.0;
    }

    @Override
    public double getDecayPerSecond() {
        return config != null ? config.getAlcoholDecayPerSecond() : 0.25;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public boolean isMilkCurable() {
        return true;
    }

    @Override
    public double getMilkReduction() {
        return config != null ? config.getAlcoholMilkReduction() : 0.5;
    }

    @Override
    public boolean isClearedOnDeath() {
        return true;
    }

    @Override
    public List<StatusEffectStage> getStages() {
        double tipsy = config != null ? config.getAlcoholTipsyThreshold() : 20.0;
        double drunk = config != null ? config.getAlcoholDrunkThreshold() : 45.0;
        double heavy = config != null ? config.getAlcoholHeavilyDrunkThreshold() : 70.0;
        double blackout = config != null ? config.getAlcoholBlackoutThreshold() : 90.0;
        return List.of(
                new StatusEffectStage("tipsy", "Tipsy", tipsy, 1),
                new StatusEffectStage("drunk", "Drunk", drunk, 2),
                new StatusEffectStage("heavily_drunk", "Heavily Drunk", heavy, 3),
                new StatusEffectStage("blackout", "Blackout", blackout, 4)
        );
    }

    @Override
    public void onApply(StatusEffectContext context) {
        double intensity = context.getActiveEffect().intensity();
        peakIntensityByPlayer.merge(context.getPlayerId(), intensity, Math::max);
    }

    @Override
    public void onStageChange(StatusEffectContext context, StatusEffectStage prev, StatusEffectStage next) {
        Player player = context.getPlatformHandle(Player.class).orElse(null);
        if (player == null || messageService == null) {
            return;
        }
        if (next == null) {
            if (prev != null) {
                player.sendMessage(messageService.render(player, MessageKey.of("effects.intoxication.sober")));
            }
            return;
        }
        String keySuffix = switch (next.id()) {
            case "tipsy" -> "stage-tipsy";
            case "drunk" -> "stage-drunk";
            case "heavily_drunk" -> "stage-heavily-drunk";
            case "blackout" -> "stage-blackout";
            default -> null;
        };
        if (keySuffix != null && (prev == null || next.priority() > prev.priority())) {
            player.sendMessage(messageService.render(player, MessageKey.of("effects.intoxication." + keySuffix)));
        }
    }

    @Override
    public void onTick(StatusEffectContext context) {
        double intensity = context.getActiveEffect().intensity();
        peakIntensityByPlayer.merge(context.getPlayerId(), intensity, Math::max);

        Player player = context.getPlatformHandle(Player.class).orElse(null);
        if (player == null) {
            return;
        }
        StatusEffectStage stage = context.getCurrentStage().orElse(null);
        if (stage == null) {
            return;
        }

        switch (stage.id()) {
            case "tipsy" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 45, 0, true, false, false));
                if (context.getTickCount() % 60L == 0L) {
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                            player.getLocation().add(0.0, 1.4, 0.0), 3, 0.3, 0.2, 0.3, 0.01);
                }
            }
            case "drunk" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 0, true, false, false));
                maybePulseNausea(player, context.getPlayerId(), context.getTickCount(), 1.0);
                maybeApplySway(player, 0.08);
            }
            case "heavily_drunk" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 45, 0, true, false, false));
                maybePulseNausea(player, context.getPlayerId(), context.getTickCount(), 0.7);
                if (context.getTickCount() % 160L == 0L) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, true, false, false));
                }
                maybeApplySway(player, 0.14);
            }
            case "blackout" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 45, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false, false));
                maybePulseNausea(player, context.getPlayerId(), context.getTickCount(), 0.5);
                if (player.getHealth() > 2.5) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 30, 0, true, false, false));
                }
                maybeApplySway(player, 0.20);
            }
            default -> {}
        }
    }

    @Override
    public void onExpire(StatusEffectContext context) {
        double peak = peakIntensityByPlayer.getOrDefault(context.getPlayerId(), 0.0);
        peakIntensityByPlayer.remove(context.getPlayerId());
        lastNauseaTickByPlayer.remove(context.getPlayerId());

        boolean hangoverEnabled = config == null || config.isAlcoholHangoverEnabled();
        double triggerThreshold = config != null ? config.getAlcoholHangoverTriggerThreshold() : 70.0;
        int durationSec = config != null ? config.getAlcoholHangoverDurationSeconds() : 60;

        if (hangoverEnabled && peak >= triggerThreshold) {
            try {
                context.getEffectService().apply(context.getPlayerId(), PaperHangoverEffect.KEY, durationSec * 20L, 0);
            } catch (IllegalArgumentException ignored) {
                // Hangover effect might be unregistered during shutdown/unload
            }
        }
    }

    @Override
    public void onRemove(StatusEffectContext context, RemovalReason reason) {
        if (reason == RemovalReason.DEATH || reason == RemovalReason.CLEARED || reason == RemovalReason.ADDON_UNLOADED) {
            peakIntensityByPlayer.remove(context.getPlayerId());
            lastNauseaTickByPlayer.remove(context.getPlayerId());
        } else if (reason == RemovalReason.REMOVED) {
            onExpire(context);
        } else {
            lastNauseaTickByPlayer.remove(context.getPlayerId());
        }
    }

    boolean shouldTriggerNauseaPulse(UUID playerId, long currentTick, double intervalMultiplier) {
        int baseSeconds = config != null ? config.getAlcoholNauseaPulseIntervalSeconds() : 12;
        long intervalTicks = Math.max(40L, Math.round(baseSeconds * 20.0 * intervalMultiplier));
        Long lastTick = lastNauseaTickByPlayer.get(playerId);
        if (lastTick == null || (currentTick - lastTick) >= intervalTicks) {
            lastNauseaTickByPlayer.put(playerId, currentTick);
            return true;
        }
        return false;
    }

    private void maybePulseNausea(Player player, UUID playerId, long currentTick, double intervalMultiplier) {
        if (shouldTriggerNauseaPulse(playerId, currentTick, intervalMultiplier)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 110, 0, true, false, false));
        }
    }

    private void maybeApplySway(Player player, double maxHorizontalVelocity) {
        if (config == null || !config.isAlcoholSwayEnabled() || !player.isOnGround()) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < 0.35) {
            double dx = (rng.nextDouble() - 0.5) * maxHorizontalVelocity;
            double dz = (rng.nextDouble() - 0.5) * maxHorizontalVelocity;
            player.setVelocity(player.getVelocity().add(new Vector(dx, 0.0, dz)));
        }
    }
}
