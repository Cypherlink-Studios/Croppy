package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionAction;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.station.component.ThermalZone;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.core.interaction.InteractionRouter;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.rule.RuleExecution;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.menu.PaperMenuService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles player interactions with crafting stations via the declarative InteractionRouter
 * and RuleEngine pipelines, falling back to default physical/GUI handling.
 */
public final class StationInteractListener implements Listener {

    private final DefaultStationManager stationManager;
    private final ProcessEngine processEngine;
    private final ItemFactory itemFactory;
    private final PaperMenuService menuService;
    private final InteractionRouter interactionRouter;
    private final RuleEngine ruleEngine;
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey durabilityKey = new NamespacedKey("croppy", "durability");

    public StationInteractListener(
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            ItemFactory itemFactory,
            PaperMenuService menuService,
            InteractionRouter interactionRouter,
            RuleEngine ruleEngine
    ) {
        this(stationManager, processEngine, itemFactory, menuService, interactionRouter, ruleEngine, null);
    }

    public StationInteractListener(
            DefaultStationManager stationManager,
            ProcessEngine processEngine,
            ItemFactory itemFactory,
            PaperMenuService menuService,
            InteractionRouter interactionRouter,
            RuleEngine ruleEngine,
            PaperMessageService messageService
    ) {
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.processEngine = Objects.requireNonNull(processEngine, "processEngine cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.menuService = Objects.requireNonNull(menuService, "menuService cannot be null");
        this.interactionRouter = Objects.requireNonNull(interactionRouter, "interactionRouter cannot be null");
        this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine cannot be null");
        this.messageService = messageService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        BlockPosition pos = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        Optional<StationInstance> stationOpt = stationManager.getStation(pos);
        if (stationOpt.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        StationInstance station = stationOpt.get();
        Player player = event.getPlayer();

        ItemStack handItem = player.getInventory().getItemInMainHand();
        boolean emptyHand = (handItem == null || handItem.getType().isAir());
        Key handItemKey = resolveItemKey(handItem);
        String handMaterial = emptyHand ? null : handItem.getType().name().toLowerCase(Locale.ROOT);
        if (handMaterial != null && !handMaterial.contains(":")) {
            handMaterial = "minecraft:" + handMaterial;
        }

        // Build evaluation context
        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        double temp = thermalOpt.map(ThermalComponent::getTemperature).orElse(20.0);
        boolean isHeated = thermalOpt.map(ThermalComponent::isHeated).orElse(false);
        String thermalZone = thermalOpt.map(t -> t.getThermalZone().name()).orElse("COLD");

        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        boolean processFinished = processOpt.isPresent()
                && processOpt.get().hasActiveProcess()
                && processOpt.get().getActiveProcess().get().getState() == ProcessState.COMPLETED;

        EvaluationContext evalCtx = EvaluationContext.empty()
                .with("player", player)
                .with("player.is_sneaking", player.isSneaking())
                .with("station.temperature", temp)
                .with("station.is_heated", isHeated)
                .with("station.thermal_zone", thermalZone)
                .with("process.finished", processFinished)
                .with("hand.is_empty", emptyHand)
                .with("hand.item", handItemKey != null ? handItemKey.asString() : "");

        // 1. Evaluate RuleEngine triggers for STATION_INTERACT
        List<RuleExecution> ruleExecutions = ruleEngine.evaluateAndApply(RuleTrigger.STATION_INTERACT, station, evalCtx);
        for (RuleExecution exec : ruleExecutions) {
            executeRulePlatformActions(player, station, exec.appliedActions());
        }

        // 2. Query InteractionRouter for matching declarative definition
        InteractionTrigger trigger = player.isSneaking()
                ? InteractionTrigger.SNEAK_RIGHT_CLICK
                : InteractionTrigger.RIGHT_CLICK;

        Optional<InteractionDefinition> matchOpt = interactionRouter.match(
                station,
                trigger,
                handItemKey,
                handMaterial,
                emptyHand,
                evalCtx
        );

        if (matchOpt.isPresent()) {
            executeInteractionPipeline(player, station, matchOpt.get(), handItem, evalCtx);
            return;
        }

        // 3. Fallback to default station interaction style
        handleDefaultInteraction(player, station);
    }

    private void executeInteractionPipeline(
            Player player,
            StationInstance station,
            InteractionDefinition def,
            ItemStack handItem,
            EvaluationContext context
    ) {
        for (InteractionAction action : def.actions()) {
            String type = action.type().toUpperCase(Locale.ROOT);
            switch (type) {
                case "OPEN_GUI" -> menuService.openStationMenu(player, station);
                case "SHOW_TEMPERATURE" -> handleShowTemperature(player, station, handItem);
                case "INSERT_INPUT" -> handleInsertInput(player, station, handItem, context);
                case "SERVE_OUTPUT", "EXTRACT_OUTPUTS" -> handleServeOutput(player, station, handItem, action);
                case "ADVANCE_PROGRESS" -> handleAdvanceProgress(station, action);
                case "PLAY_SOUND" -> playConfiguredSound(player, action);
                case "EMIT_PARTICLES" -> emitConfiguredParticles(player, station, action);
            }
        }
    }

    private void handleShowTemperature(Player player, StationInstance station, ItemStack handItem) {
        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        double temp = thermalOpt.map(ThermalComponent::getTemperature).orElse(20.0);
        ThermalZone zone = thermalOpt.map(ThermalComponent::getThermalZone).orElse(ThermalZone.COLD);

        String color = switch (zone) {
            case COLD -> "#55ffff";
            case WARM -> "#55ff55";
            case SIMMER -> "#ffaa00";
            case BOIL -> "#ff7700";
            case SEAR -> "#ff3300";
            case SCORCH -> "#aa0000";
        };

        if (messageService != null) {
            messageService.sendActionBarWithReplacements(
                    player,
                    MessageKey.of("station.temperature"),
                    Map.of("{color}", color),
                    PaperMessageService.args(
                            "temperature", String.format(Locale.ROOT, "%.1f", temp),
                            "zone", zone.name()
                    )
            );
        } else {
            String msg = String.format("<gradient:#ffaa00:%s>Temperature:</gradient> <white>%.1f°C</white> <gray>[%s]</gray>", color, temp, zone.name());
            player.sendActionBar(miniMessage.deserialize(msg));
        }
        player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.2f);

        // Apply tool durability damage if applicable
        consumeThermometerDurability(player, handItem);
    }

    private void consumeThermometerDurability(Player player, ItemStack handItem) {
        if (handItem == null || handItem.getType().isAir()) {
            return;
        }

        ItemMeta meta = handItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Custom PDC durability
        if (meta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.INTEGER)) {
            int dur = meta.getPersistentDataContainer().get(durabilityKey, PersistentDataType.INTEGER);
            if (dur > 0) {
                dur--;
                if (dur <= 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                } else {
                    meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, dur);
                    handItem.setItemMeta(meta);
                }
            }
            return;
        }

        // Vanilla Damageable
        if (meta instanceof Damageable damageable) {
            int maxDur = handItem.getType().getMaxDurability();
            if (maxDur > 0) {
                int damage = damageable.getDamage() + 1;
                if (damage >= maxDur) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                } else {
                    damageable.setDamage(damage);
                    handItem.setItemMeta(damageable);
                }
            }
        }
    }

    private void handleInsertInput(Player player, StationInstance station, ItemStack handItem, EvaluationContext context) {
        if (handItem == null || handItem.getType().isAir()) {
            return;
        }

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        Key itemKey = resolveItemKey(handItem);
        boolean added = storageOpt.get().addInput(StoredItem.of(itemKey, 1));
        if (added) {
            handItem.subtract(1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);
            processEngine.tryStartProcess(station, context);
        }
    }

    private void handleServeOutput(Player player, StationInstance station, ItemStack handItem, InteractionAction action) {
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        List<StoredItem> outputs = storageOpt.get().extractOutputs();
        if (outputs.isEmpty()) {
            return;
        }

        boolean consumeHand = Boolean.TRUE.equals(action.parameters().get("consume_hand_item"));
        if (consumeHand && handItem != null && !handItem.getType().isAir()) {
            handItem.subtract(1);
        }

        for (StoredItem out : outputs) {
            player.getInventory().addItem(createBukkitItem(out));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }

    private void handleAdvanceProgress(StationInstance station, InteractionAction action) {
        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        if (processOpt.isEmpty() || !processOpt.get().hasActiveProcess()) {
            return;
        }

        long ticks = 20;
        Object rawTicks = action.parameters().get("ticks");
        if (rawTicks instanceof Number n) {
            ticks = n.longValue();
        }

        ProcessInstance pi = processOpt.get().getActiveProcess().get();
        pi.addPhaseElapsedTicks(ticks);
    }

    private void executeRulePlatformActions(Player player, StationInstance station, List<RuleAction> actions) {
        for (RuleAction action : actions) {
            String type = action.type().toUpperCase(Locale.ROOT);
            switch (type) {
                case "DAMAGE_PLAYER" -> {
                    double damage = 1.0;
                    Object d = action.parameters().get("damage");
                    if (d instanceof Number n) {
                        damage = n.doubleValue();
                    }
                    player.damage(damage);
                }
                case "PLAY_SOUND" -> playConfiguredSound(player, action);
                case "EMIT_PARTICLES" -> emitConfiguredParticles(player, station, action);
                case "SEND_MESSAGE" -> {
                    Object msg = action.parameters().get("message");
                    if (msg != null) {
                        player.sendMessage(miniMessage.deserialize(msg.toString()));
                    }
                }
            }
        }
    }

    private void playConfiguredSound(Player player, Object actionObj) {
        String soundName = null;
        float volume = 1.0f;
        float pitch = 1.0f;

        if (actionObj instanceof InteractionAction ia) {
            soundName = (String) ia.parameters().get("sound");
            if (ia.parameters().get("volume") instanceof Number v) volume = v.floatValue();
            if (ia.parameters().get("pitch") instanceof Number p) pitch = p.floatValue();
        } else if (actionObj instanceof RuleAction ra) {
            soundName = (String) ra.parameters().get("sound");
            if (ra.parameters().get("volume") instanceof Number v) volume = v.floatValue();
            if (ra.parameters().get("pitch") instanceof Number p) pitch = p.floatValue();
        }

        if (soundName != null) {
            try {
                NamespacedKey sKey = soundName.contains(":")
                        ? NamespacedKey.fromString(soundName.toLowerCase(Locale.ROOT))
                        : NamespacedKey.minecraft(soundName.toLowerCase(Locale.ROOT));
                if (sKey != null) {
                    Sound sound = org.bukkit.Registry.SOUNDS.get(sKey);
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void emitConfiguredParticles(Player player, StationInstance station, Object actionObj) {
        String particleName = null;
        int count = 5;

        if (actionObj instanceof InteractionAction ia) {
            particleName = (String) ia.parameters().get("particle");
            if (ia.parameters().get("count") instanceof Number c) count = c.intValue();
        } else if (actionObj instanceof RuleAction ra) {
            particleName = (String) ra.parameters().get("particle");
            if (ra.parameters().get("count") instanceof Number c) count = c.intValue();
        }

        if (particleName != null) {
            try {
                Particle p = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
                BlockPosition pos = station.getPosition();
                player.getWorld().spawnParticle(p, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, count, 0.2, 0.2, 0.2, 0.02);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void handleDefaultInteraction(Player player, StationInstance station) {
        StationDefinition def = station.getDefinition();
        boolean openGui = (def.interactionType() == StationInteractionType.GUI)
                || (def.interactionType() == StationInteractionType.HYBRID && !player.isSneaking());

        if (openGui) {
            menuService.openStationMenu(player, station);
            return;
        }

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }
        StorageComponent storage = storageOpt.get();

        // 1. Extract outputs
        List<StoredItem> outputs = storage.extractOutputs();
        if (!outputs.isEmpty()) {
            for (StoredItem out : outputs) {
                player.getInventory().addItem(createBukkitItem(out));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            return;
        }

        // 2. Manual interaction if awaiting
        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        if (processOpt.isPresent() && processOpt.get().hasActiveProcess()) {
            ProcessInstance pi = processOpt.get().getActiveProcess().get();
            if (pi.getState() == ProcessState.AWAITING_INPUT) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                Key itemKey = resolveItemKey(handItem);
                int amount = (handItem != null) ? handItem.getAmount() : 0;
                boolean advanced = processEngine.handleManualInteraction(station, itemKey, amount);
                if (advanced) {
                    handItem.subtract(1);
                    player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
                    return;
                }
            }
        }

        // 3. Insert input
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && !handItem.getType().isAir()) {
            Key itemKey = resolveItemKey(handItem);
            boolean added = storage.addInput(StoredItem.of(itemKey, 1));
            if (added) {
                handItem.subtract(1);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);
                EvaluationContext context = EvaluationContext.empty().with("player", player);
                processEngine.tryStartProcess(station, context);
            }
            return;
        }

        // 4. Feedback for empty hand interaction
        sendEmptyPhysicalFeedback(player, station);
    }

    private void sendEmptyPhysicalFeedback(Player player, StationInstance station) {
        StationDefinition def = station.getDefinition();
        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        double temp = thermalOpt.map(ThermalComponent::getTemperature).orElse(20.0);
        ThermalZone zone = thermalOpt.map(ThermalComponent::getThermalZone).orElse(ThermalZone.COLD);

        String color = switch (zone) {
            case COLD -> "#55ffff";
            case WARM -> "#55ff55";
            case SIMMER -> "#ffaa00";
            case BOIL -> "#ff7700";
            case SEAR -> "#ff3300";
            case SCORCH -> "#aa0000";
        };

        Optional<ProcessComponent> processOpt = station.getComponent(ProcessComponent.class);
        String statusText;
        if (processOpt.isPresent() && processOpt.get().hasActiveProcess()) {
            ProcessInstance pi = processOpt.get().getActiveProcess().get();
            statusText = "<yellow>" + pi.getState().name() + "</yellow>";
        } else {
            statusText = (messageService != null)
                    ? miniMessage.serialize(messageService.render(player, MessageKey.of("station.empty")))
                    : "<gray>Empty</gray>";
        }

        if (messageService != null) {
            if (thermalOpt.isPresent()) {
                messageService.sendActionBarWithReplacements(
                        player,
                        MessageKey.of("station.empty-feedback"),
                        Map.of("{color}", color),
                        PaperMessageService.args(
                                "name", miniMessage.deserialize(def.displayName()),
                                "status", miniMessage.deserialize(statusText),
                                "temperature", String.format(Locale.ROOT, "%.1f", temp)
                        )
                );
            } else {
                messageService.sendActionBar(
                        player,
                        MessageKey.of("station.empty-feedback-no-thermal"),
                        PaperMessageService.args(
                                "name", miniMessage.deserialize(def.displayName()),
                                "status", miniMessage.deserialize(statusText)
                        )
                );
            }
        } else {
            String msg;
            if (thermalOpt.isPresent()) {
                msg = String.format("%s <dark_gray>•</dark_gray> %s <dark_gray>•</dark_gray> <gradient:#ffaa00:%s>%.1f°C</gradient>",
                        def.displayName(), statusText, color, temp);
            } else {
                msg = String.format("%s <dark_gray>•</dark_gray> %s", def.displayName(), statusText);
            }
            player.sendActionBar(miniMessage.deserialize(msg));
        }
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.5f, 1.3f);
    }

    private Key resolveItemKey(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return itemFactory.getItemId(item)
                .orElseGet(() -> Key.of("minecraft", item.getType().name().toLowerCase(Locale.ROOT)));
    }

    private ItemStack createBukkitItem(StoredItem stored) {
        Optional<dev.darkblade.croppy.api.item.ItemDefinition> defOpt = itemFactory.getItemRegistry().get(stored.itemKey());
        if (defOpt.isPresent()) {
            return itemFactory.createItemStack(defOpt.get(), stored.amount());
        }
        return itemFactory.createItemStack(new dev.darkblade.croppy.api.item.ItemDefinition(
                stored.itemKey(),
                "misc",
                stored.itemKey().asString(),
                0,
                "<gray>" + stored.itemKey().value() + "</gray>",
                List.of(),
                java.util.Map.of()
        ), stored.amount());
    }
}
