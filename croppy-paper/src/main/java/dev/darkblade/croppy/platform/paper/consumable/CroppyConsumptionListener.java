package dev.darkblade.croppy.platform.paper.consumable;

import dev.darkblade.croppy.api.consumable.ConsumeExecutionContext;
import dev.darkblade.croppy.api.effect.PlayerEffectService;
import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.item.ConsumableProperties;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.core.consumable.ConsumptionEvaluator;
import dev.darkblade.croppy.core.consumable.SimpleConsumeExecutionContext;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-authoritative consumption and status-effect lifecycle listener.
 */
public class CroppyConsumptionListener implements Listener {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final ConsumptionEvaluator evaluator;
    private final PlayerEffectService effectService;
    private final CroppyLogger logger;

    public CroppyConsumptionListener(
            ItemFactory itemFactory,
            ItemRegistry itemRegistry,
            ConsumptionEvaluator evaluator,
            PlayerEffectService effectService,
            CroppyLogger logger
    ) {
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry cannot be null");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator cannot be null");
        this.effectService = Objects.requireNonNull(effectService, "effectService cannot be null");
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        Player player = event.getPlayer();

        if (item.getType() == Material.MILK_BUCKET && itemFactory.getItemId(item).isEmpty()) {
            effectService.handleMilkConsume(player.getUniqueId());
            return;
        }

        Optional<ItemDefinition> defOpt = itemFactory.getItemId(item).flatMap(itemRegistry::get);
        if (defOpt.isEmpty()) {
            return;
        }
        ItemDefinition def = defOpt.get();
        Optional<ConsumableProperties> propsOpt = def.getConsumableProperties();
        if (propsOpt.isEmpty()) {
            return;
        }

        StoredItem stored = itemFactory.toStoredItem(item).orElse(null);
        int quality = stored != null ? Math.max(0, Math.min(3, stored.quality())) : 0;

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("item.id", def.key().asString());
        vars.put("item.category", def.category());
        vars.put("item.quality", quality);
        vars.put("player.name", player.getName());
        vars.put("player.health", player.getHealth());
        vars.put("player.food", player.getFoodLevel());
        if (stored != null && !stored.properties().isEmpty()) {
            for (Map.Entry<String, Object> entry : stored.properties().entrySet()) {
                vars.put("item.prop." + entry.getKey(), entry.getValue());
            }
        }

        ConsumeExecutionContext execCtx = new SimpleConsumeExecutionContext(
                player.getUniqueId(),
                player.getName(),
                def,
                stored,
                quality,
                EvaluationContext.of(vars),
                player
        );

        try {
            evaluator.execute(propsOpt.get(), execCtx);
        } catch (Exception e) {
            logger.error(LogChannel.CORE,
                    "Failed executing consumption effects for " + def.key() + " on player " + player.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        effectService.handlePlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        effectService.handlePlayerQuit(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        effectService.handlePlayerDeath(event.getEntity().getUniqueId());
    }
}
