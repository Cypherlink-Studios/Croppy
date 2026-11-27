package dev.darkblade.croppy.platform.paper.effect;

import dev.darkblade.croppy.api.effect.CustomStatusEffect;
import dev.darkblade.croppy.api.effect.RemovalReason;
import dev.darkblade.croppy.api.effect.StackingPolicy;
import dev.darkblade.croppy.api.effect.StatusEffectContext;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Built-in hangover status effect ({@code croppy:hangover}) triggered after heavy
 * intoxication subsides, removable via milk or {@code croppy:reduce_status}.
 */
public class PaperHangoverEffect implements CustomStatusEffect {

    public static final Key KEY = Key.of("croppy", "hangover");

    private final PaperMessageService messageService;

    public PaperHangoverEffect(PaperMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Hangover";
    }

    @Override
    public StackingPolicy getStackingPolicy() {
        return StackingPolicy.REPLACE;
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
    public boolean isClearedOnDeath() {
        return true;
    }

    @Override
    public void onApply(StatusEffectContext context) {
        Player player = context.getPlatformHandle(Player.class).orElse(null);
        if (player != null && messageService != null) {
            player.sendMessage(messageService.render(player, MessageKey.of("effects.hangover.applied")));
        }
    }

    @Override
    public void onTick(StatusEffectContext context) {
        Player player = context.getPlatformHandle(Player.class).orElse(null);
        if (player == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 45, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 45, 0, true, false, false));
    }

    @Override
    public void onExpire(StatusEffectContext context) {
        sendRelieved(context);
    }

    @Override
    public void onRemove(StatusEffectContext context, RemovalReason reason) {
        if (reason == RemovalReason.MILK || reason == RemovalReason.REMOVED || reason == RemovalReason.CLEARED) {
            sendRelieved(context);
        }
    }

    private void sendRelieved(StatusEffectContext context) {
        Player player = context.getPlatformHandle(Player.class).orElse(null);
        if (player != null && messageService != null) {
            player.sendMessage(messageService.render(player, MessageKey.of("effects.hangover.relieved")));
        }
    }
}
