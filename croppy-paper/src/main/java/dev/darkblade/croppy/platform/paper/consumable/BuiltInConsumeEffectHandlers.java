package dev.darkblade.croppy.platform.paper.consumable;

import dev.darkblade.croppy.api.consumable.ConsumeEffectHandler;
import dev.darkblade.croppy.api.consumable.ConsumeEffectRegistry;
import dev.darkblade.croppy.api.consumable.ConsumeExecutionContext;
import dev.darkblade.croppy.api.effect.PlayerEffectService;
import dev.darkblade.croppy.api.effect.RemovalReason;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.QualityScaling;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.core.process.YamlProcessLoader;
import dev.darkblade.croppy.platform.paper.effect.PaperIntoxicationEffect;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Registers Croppy's built-in {@link ConsumeEffectHandler} implementations for Paper.
 */
public final class BuiltInConsumeEffectHandlers {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private BuiltInConsumeEffectHandlers() {}

    public static void registerAll(
            ConsumeEffectRegistry registry,
            PlayerEffectService effectService,
            CroppyLogger logger
    ) {
        Objects.requireNonNull(registry, "registry cannot be null");
        Objects.requireNonNull(effectService, "effectService cannot be null");

        register(registry, "potion", (ctx, params) -> applyPotion(ctx, params, logger));
        register(registry, "remove_potion", (ctx, params) -> removePotion(ctx, params));
        register(registry, "clear_effects", (ctx, params) -> clearEffects(ctx, params, effectService));
        register(registry, "heal", BuiltInConsumeEffectHandlers::heal);
        register(registry, "damage", BuiltInConsumeEffectHandlers::damage);
        register(registry, "oxygen", BuiltInConsumeEffectHandlers::oxygen);
        register(registry, "sound", BuiltInConsumeEffectHandlers::playSound);
        register(registry, "particle", BuiltInConsumeEffectHandlers::spawnParticle);
        register(registry, "message", BuiltInConsumeEffectHandlers::sendMessage);
        register(registry, "teleport_randomly", BuiltInConsumeEffectHandlers::teleportRandomly);
        register(registry, "apply_status", (ctx, params) -> applyStatus(ctx, params, effectService));
        register(registry, "reduce_status", (ctx, params) -> reduceStatus(ctx, params, effectService));
        register(registry, "remove_status", (ctx, params) -> removeStatus(ctx, params, effectService));
        register(registry, "alcohol", (ctx, params) -> applyAlcohol(ctx, params, effectService));
    }

    private static void register(ConsumeEffectRegistry registry, String id, HandlerFn fn) {
        Key key = Key.of("croppy", id);
        if (registry.contains(key)) {
            registry.unregister(key);
        }
        registry.register(new ConsumeEffectHandler() {
            @Override
            public Key getKey() {
                return key;
            }

            @Override
            public void apply(ConsumeExecutionContext context, Map<String, Object> parameters) {
                fn.execute(context, parameters != null ? parameters : Map.of());
            }
        });
    }

    @FunctionalInterface
    private interface HandlerFn {
        void execute(ConsumeExecutionContext context, Map<String, Object> params);
    }

    private static void applyPotion(ConsumeExecutionContext ctx, Map<String, Object> params, CroppyLogger logger) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;

        String rawEffect = getString(params, "effect", null);
        if (rawEffect == null || rawEffect.isBlank()) return;

        PotionEffectType type = resolvePotionType(rawEffect);
        if (type == null) {
            if (logger != null) {
                logger.warn(LogChannel.CORE, "Unknown potion effect '" + rawEffect + "' on item " + ctx.getItemDefinition().key());
            }
            return;
        }

        long baseTicks = YamlProcessLoader.parseDurationTicks(params.get("duration"), 200L);
        long scaledTicks = scaleDuration(ctx, baseTicks);
        int amplifier = Math.max(0, getInt(params, "amplifier", 0));
        boolean ambient = getBoolean(params, "ambient", false);
        boolean particles = getBoolean(params, "particles", true);
        boolean icon = getBoolean(params, "icon", true);

        player.addPotionEffect(new PotionEffect(type, (int) Math.min(Integer.MAX_VALUE, scaledTicks), amplifier, ambient, particles, icon));
    }

    private static void removePotion(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;

        String single = getString(params, "effect", null);
        if (single != null) {
            PotionEffectType type = resolvePotionType(single);
            if (type != null) player.removePotionEffect(type);
        }
        Object rawList = params.get("effects");
        if (rawList instanceof Collection<?> list) {
            for (Object entry : list) {
                if (entry != null) {
                    PotionEffectType type = resolvePotionType(entry.toString());
                    if (type != null) player.removePotionEffect(type);
                }
            }
        }
    }

    private static void clearEffects(ConsumeExecutionContext ctx, Map<String, Object> params, PlayerEffectService effectService) {
        boolean clearVanilla = getBoolean(params, "vanilla", true);
        boolean clearCustom = getBoolean(params, "custom", false);
        if (clearVanilla) {
            ctx.getPlatformHandle(Player.class).ifPresent(player -> {
                for (PotionEffect active : List.copyOf(player.getActivePotionEffects())) {
                    player.removePotionEffect(active.getType());
                }
            });
        }
        if (clearCustom) {
            effectService.clearPlayer(ctx.getPlayerId(), RemovalReason.CLEARED);
        }
    }

    private static void heal(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        double amount = Math.max(0.0, getDouble(params, "amount", 2.0));
        if (amount <= 0.0) return;

        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
        player.setHealth(Math.min(maxHealth, player.getHealth() + amount));
    }

    private static void damage(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        double amount = Math.max(0.0, getDouble(params, "amount", 1.0));
        if (amount > 0.0) {
            player.damage(amount);
        }
    }

    private static void oxygen(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        int amount = getInt(params, "amount", 60);
        int next = Math.max(0, Math.min(player.getMaximumAir(), player.getRemainingAir() + amount));
        player.setRemainingAir(next);
    }

    private static void playSound(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        String rawSound = getString(params, "sound", null);
        if (rawSound == null || rawSound.isBlank()) return;
        float volume = (float) getDouble(params, "volume", 1.0);
        float pitch = (float) getDouble(params, "pitch", 1.0);
        String normalized = rawSound.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        player.playSound(player.getLocation(), normalized, SoundCategory.PLAYERS, volume, pitch);
    }

    private static void spawnParticle(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        String raw = getString(params, "particle", "HEART");
        String enumName = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        Particle particle;
        try {
            particle = Particle.valueOf(enumName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }
        int count = Math.max(1, getInt(params, "count", 8));
        double offset = Math.max(0.0, getDouble(params, "offset", 0.35));
        double extra = Math.max(0.0, getDouble(params, "extra", 0.05));
        player.getWorld().spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0), count, offset, offset, offset, extra);
    }

    private static void sendMessage(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        String raw = getString(params, "message", null);
        if (raw == null || raw.isBlank()) return;
        Component component = MM.deserialize(raw.replace("<player>", player.getName()));
        String target = getString(params, "target", "CHAT").toUpperCase(Locale.ROOT);
        if ("ACTIONBAR".equals(target)) {
            player.sendActionBar(component);
        } else {
            player.sendMessage(component);
        }
    }

    private static void teleportRandomly(ConsumeExecutionContext ctx, Map<String, Object> params) {
        Player player = ctx.getPlatformHandle(Player.class).orElse(null);
        if (player == null) return;
        double diameter = Math.max(2.0, getDouble(params, "diameter", getDouble(params, "radius", 16.0)));
        int attempts = Math.max(1, getInt(params, "attempts", 16));
        Location origin = player.getLocation();
        World world = origin.getWorld();
        if (world == null) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < attempts; i++) {
            double x = origin.getX() + (rng.nextDouble() - 0.5) * diameter;
            double y = Math.max(world.getMinHeight() + 1, Math.min(world.getMaxHeight() - 2,
                    origin.getY() + (rng.nextInt(16) - 8)));
            double z = origin.getZ() + (rng.nextDouble() - 0.5) * diameter;
            Location candidate = new Location(world, x, y, z, origin.getYaw(), origin.getPitch());
            Block feet = candidate.getBlock();
            Block head = feet.getRelative(0, 1, 0);
            Block ground = feet.getRelative(0, -1, 0);
            if (!feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid()) {
                if (player.teleport(candidate)) {
                    world.playSound(candidate, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyStatus(ConsumeExecutionContext ctx, Map<String, Object> params, PlayerEffectService service) {
        String rawStatus = getString(params, "status", null);
        if (rawStatus == null || rawStatus.isBlank()) return;
        Key statusKey = parseKey(rawStatus, "croppy");
        long baseTicks = YamlProcessLoader.parseDurationTicks(params.get("duration"), 200L);
        long scaledTicks = scaleDuration(ctx, baseTicks);
        int amplifier = Math.max(0, getInt(params, "amplifier", 0));
        double intensity = Math.max(0.0, getDouble(params, "intensity", 0.0));
        Map<String, Object> data = new LinkedHashMap<>();
        if (params.get("data") instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        service.apply(ctx.getPlayerId(), statusKey, scaledTicks, amplifier, intensity, data);
    }

    private static void reduceStatus(ConsumeExecutionContext ctx, Map<String, Object> params, PlayerEffectService service) {
        String rawStatus = getString(params, "status", null);
        if (rawStatus == null || rawStatus.isBlank()) return;
        Key statusKey = parseKey(rawStatus, "croppy");
        double intensityDelta = Math.max(0.0, getDouble(params, "intensity", 0.0));
        long ticksDelta = params.containsKey("duration")
                ? YamlProcessLoader.parseDurationTicks(params.get("duration"), 0L)
                : 0L;
        service.reduce(ctx.getPlayerId(), statusKey, intensityDelta, ticksDelta);
    }

    private static void removeStatus(ConsumeExecutionContext ctx, Map<String, Object> params, PlayerEffectService service) {
        String rawStatus = getString(params, "status", null);
        if (rawStatus == null || rawStatus.isBlank()) return;
        service.remove(ctx.getPlayerId(), parseKey(rawStatus, "croppy"), RemovalReason.REMOVED);
    }

    private static void applyAlcohol(ConsumeExecutionContext ctx, Map<String, Object> params, PlayerEffectService service) {
        double amountOverride = getDouble(params, "amount", -1.0);
        double intensityDelta;
        if (amountOverride > 0.0) {
            intensityDelta = amountOverride;
        } else {
            double abv = Math.max(0.0, getDouble(params, "abv", 5.0));
            double volumeMl = Math.max(1.0, getDouble(params, "volume-ml", getDouble(params, "volume_ml", 250.0)));
            double factor = Math.max(0.0, getDouble(params, "factor", 1.0));
            intensityDelta = abv * (volumeMl / 250.0) * factor * 2.0;
        }
        if (intensityDelta > 0.0) {
            service.apply(ctx.getPlayerId(), PaperIntoxicationEffect.KEY, 0L, 0, intensityDelta, Map.of());
        }
    }

    private static long scaleDuration(ConsumeExecutionContext ctx, long baseTicks) {
        return ctx.getItemDefinition().getConsumableProperties()
                .flatMap(p -> p.getFood())
                .map(fp -> fp.qualityScaling().scaleDurationTicks(baseTicks, ctx.getQuality()))
                .orElse(baseTicks);
    }

    static PotionEffectType resolvePotionType(String raw) {
        Key key = parseKey(raw, "minecraft");
        NamespacedKey nk = new NamespacedKey(key.namespace(), key.value().toLowerCase(Locale.ROOT));
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(nk);
    }

    private static Key parseKey(String raw, String defaultNs) {
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains(":") ? Key.parse(trimmed) : Key.of(defaultNs, trimmed);
    }

    private static String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : def;
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static double getDouble(Map<String, Object> map, String key, double def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val != null) {
            try {
                return Double.parseDouble(val.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val != null) {
            String s = val.toString().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(s)) return true;
            if ("false".equals(s)) return false;
        }
        return def;
    }
}
