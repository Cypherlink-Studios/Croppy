package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Cloud 2.0 command controller handling all /croppy administrative subcommands.
 */
public class CroppyCommands {

    private final CroppyPlugin plugin;
    private final CropInspectorService inspectorService;
    private final PluginConfig pluginConfig;
    private final WateringCanService wateringCanService;
    private final FertilizerService fertilizerService;
    private final FertilizerRegistry fertilizerRegistry;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage;

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig,
            WateringCanService wateringCanService,
            FertilizerService fertilizerService,
            FertilizerRegistry fertilizerRegistry,
            ItemFactory itemFactory,
            ItemRegistry itemRegistry
    ) {
        this.plugin = plugin;
        this.inspectorService = Objects.requireNonNull(inspectorService, "inspectorService cannot be null");
        this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig cannot be null");
        this.wateringCanService = wateringCanService;
        this.fertilizerService = fertilizerService;
        this.fertilizerRegistry = fertilizerRegistry;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        if (plugin != null && plugin.getMessageService() != null) {
            this.messageService = plugin.getMessageService();
        } else {
            dev.darkblade.croppy.core.i18n.DefaultMessageCatalog catalog = new dev.darkblade.croppy.core.i18n.DefaultMessageCatalog(dev.darkblade.croppy.api.i18n.LocaleRef.EN_US, dev.darkblade.croppy.api.i18n.LocaleRef.EN_US);
            dev.darkblade.croppy.core.i18n.YamlLocaleLoader loader = new dev.darkblade.croppy.core.i18n.YamlLocaleLoader();
            try {
                try (var enIn = CroppyCommands.class.getResourceAsStream("/locales/en-US.yml")) {
                    if (enIn != null) {
                        catalog.registerLocale(dev.darkblade.croppy.api.i18n.LocaleRef.EN_US, loader.load(enIn, "en-US.yml"));
                    }
                }
                try (var esIn = CroppyCommands.class.getResourceAsStream("/locales/es-ES.yml")) {
                    if (esIn != null) {
                        catalog.registerLocale(dev.darkblade.croppy.api.i18n.LocaleRef.ES_ES, loader.load(esIn, "es-ES.yml"));
                    }
                }
            } catch (Exception ignored) {}
            this.messageService = new PaperMessageService(catalog, null);
        }
        this.miniMessage = MiniMessage.miniMessage();
    }

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this(plugin, inspectorService, pluginConfig, null, null, null, null, null);
    }

    private void sendPrefixed(CommandSender sender, MessageKey key, Object... args) {
        if (messageService != null) {
            messageService.sendPrefixedMessage(sender, key, PaperMessageService.args(args));
        } else {
            sender.sendMessage(miniMessage.deserialize("<gold>[Croppy]</gold> <red>" + key.key() + "</red>"));
        }
    }

    private void sendRaw(CommandSender sender, MessageKey key, Object... args) {
        if (messageService != null) {
            messageService.sendMessage(sender, key, PaperMessageService.args(args));
        } else {
            sender.sendMessage(miniMessage.deserialize(key.key()));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Dynamic Autocompletion Providers
    // ---------------------------------------------------------------------------------------------

    @Suggestions("item_categories")
    public List<String> suggestItemCategories(CommandContext<Source> context, CommandInput input) {
        if (itemRegistry == null) {
            return List.of();
        }
        return List.copyOf(itemRegistry.getCategories());
    }

    @Suggestions("category_items")
    public List<String> suggestCategoryItems(CommandContext<Source> context, CommandInput input) {
        if (itemRegistry == null) {
            return List.of();
        }
        String category = (context != null) ? context.getOrDefault("category", null) : null;
        if (category != null && !category.isBlank()) {
            return itemRegistry.getByCategory(category).stream()
                    .map(def -> def.key().asString())
                    .toList();
        }
        return itemRegistry.values().stream()
                .map(def -> def.key().asString())
                .toList();
    }

    @Suggestions("fertilizer_items")
    public List<String> suggestFertilizerItems(CommandContext<Source> context, CommandInput input) {
        if (fertilizerRegistry != null && !fertilizerRegistry.values().isEmpty()) {
            return fertilizerRegistry.values().stream()
                    .map(f -> f.getKey().asString())
                    .toList();
        }
        if (itemRegistry != null) {
            return itemRegistry.getByCategory("fertilizers").stream()
                    .map(def -> def.key().asString())
                    .toList();
        }
        return List.of();
    }

    // ---------------------------------------------------------------------------------------------
    // Unified Item Commands
    // ---------------------------------------------------------------------------------------------

    @Command("croppy item give <category> <item_id> [quantity]")
    @Permission("croppy.admin")
    public void giveItem(
            Source source,
            @Argument(value = "category", suggestions = "item_categories") String category,
            @Argument(value = "item_id", suggestions = "category_items") Key itemId,
            @Argument("quantity") @Default("1") int quantity,
            @Flag("target") Player targetPlayer,
            @Flag("silent") boolean silent
    ) {
        CommandSender sender = source.source();
        Player recipient = targetPlayer;
        if (recipient == null) {
            if (sender instanceof Player p) {
                recipient = p;
            } else {
                sendPrefixed(sender, MessageKey.of("command.give.sender-required"));
                return;
            }
        }

        if (itemRegistry == null || itemFactory == null) {
            sendPrefixed(sender, MessageKey.of("command.give.registry-not-initialized"));
            return;
        }

        Optional<ItemDefinition> defOpt = itemRegistry.get(itemId);
        if (defOpt.isEmpty()) {
            sendPrefixed(sender, MessageKey.of("command.give.item-not-found"), "item", itemId.asString());
            return;
        }

        ItemDefinition def = defOpt.get();
        int amount = Math.max(1, quantity);
        ItemStack itemStack = itemFactory.createItemStack(def, amount);

        final Player recipientFinal = recipient;
        var leftover = recipientFinal.getInventory().addItem(itemStack);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(rem -> recipientFinal.getWorld().dropItemNaturally(recipientFinal.getLocation(), rem));
        }

        Component recipientItemDisplay = (messageService != null)
                ? messageService.resolveItemDisplayName(recipientFinal, def)
                : miniMessage.deserialize(def.displayName());

        if (!silent && !recipientFinal.equals(sender)) {
            sendPrefixed(recipientFinal, MessageKey.of("command.give.received"), "amount", amount, "item", recipientItemDisplay);
        }

        Component senderItemDisplay = (messageService != null)
                ? messageService.resolveItemDisplayName(sender, def)
                : miniMessage.deserialize(def.displayName());

        sendPrefixed(sender, MessageKey.of("command.give.given"), "amount", amount, "item", senderItemDisplay, "player", recipientFinal.getName());
    }

    @Command("croppy item list [category]")
    @Permission("croppy.admin")
    public void listItemDefinitions(
            Source source,
            @Argument(value = "category", suggestions = "item_categories") @Nullable String category
    ) {
        CommandSender sender = source.source();
        if (itemRegistry == null) {
            sendPrefixed(sender, MessageKey.of("command.give.registry-not-initialized"));
            return;
        }

        if (category != null && !category.isBlank()) {
            Collection<ItemDefinition> items = itemRegistry.getByCategory(category);
            if (items.isEmpty()) {
                sendPrefixed(sender, MessageKey.of("command.list.empty-category"), "category", category);
                return;
            }
            sendRaw(sender, MessageKey.of("command.list.category-header"), "category", category, "count", items.size());
            for (ItemDefinition def : items) {
                sender.sendMessage(miniMessage.deserialize(" <dark_gray>•</dark_gray> <yellow>" + def.key().asString() + "</yellow> - " + def.displayName()));
            }
        } else {
            Collection<ItemDefinition> all = itemRegistry.values();
            sendRaw(sender, MessageKey.of("command.list.all-header"), "count", all.size());
            for (String cat : itemRegistry.getCategories()) {
                Collection<ItemDefinition> inCat = itemRegistry.getByCategory(cat);
                sender.sendMessage(miniMessage.deserialize("<aqua>[" + cat + "]</aqua> <gray>(" + inCat.size() + "):</gray>"));
                for (ItemDefinition def : inCat) {
                    sender.sendMessage(miniMessage.deserialize("   <dark_gray>•</dark_gray> <yellow>" + def.key().asString() + "</yellow> - " + def.displayName()));
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Core Crop & Inspection Commands
    // ---------------------------------------------------------------------------------------------

    @Command("croppy inspect")
    @Permission("croppy.admin")
    public void inspectRaycast(Source source) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.inspect.console-needs-coords"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<Component> card = inspectorService.inspect(pos, player);
        if (card.isPresent()) {
            player.sendMessage(card.get());
        } else {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
        }
    }

    @Command("croppy inspect <x> <y> <z>")
    @Permission("croppy.admin")
    public void inspectCoordinates(
            Source source,
            @Argument("x") int x,
            @Argument("y") int y,
            @Argument("z") int z
    ) {
        CommandSender sender = source.source();
        World world = (sender instanceof Player player)
                ? player.getWorld()
                : (source.stack().getLocation().getWorld() != null ? source.stack().getLocation().getWorld() : Bukkit.getWorlds().get(0));
        BlockPosition pos = BlockPosition.of(world.getName(), x, y, z);
        Optional<Component> card = inspectorService.inspect(pos, sender);
        if (card.isPresent()) {
            sender.sendMessage(card.get());
        } else {
            sendPrefixed(sender, MessageKey.of("command.inspect.no-crop-found"));
        }
    }

    @Command("croppy reload")
    @Permission("croppy.admin")
    public void reload(Source source) {
        CommandSender sender = source.source();
        try {
            plugin.reloadPlugin();
            sendPrefixed(sender, MessageKey.of("general.reload-success"));
        } catch (Exception e) {
            sendPrefixed(sender, MessageKey.of("general.reload-failed"), "error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            plugin.getLogger().severe("Failed to reload Croppy: " + e.getMessage());
        }
    }

    @Command("croppy water")
    @Permission("croppy.admin")
    public void waterTarget(Source source) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            cropOpt = plugin.getCore().getCropManager().getCrop(BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY() + 1, target.getZ()));
        }

        if (cropOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        long currentDay = target.getWorld().getFullTime() / 24000L;
        CropInstance crop = cropOpt.get();
        crop.setLastWateredDay(currentDay);

        Block blockBelow = target.getWorld().getBlockAt(crop.getPosition().x(), crop.getPosition().y() - 1, crop.getPosition().z());
        if (blockBelow.getType() == Material.FARMLAND && blockBelow.getBlockData() instanceof Farmland farmland) {
            farmland.setMoisture(7);
            blockBelow.setBlockData(farmland, false);
        }

        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0.5, 0.5, 0.5), 15, 0.2, 0.2, 0.2, 0.1);
        player.playSound(target.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.8f, 1.2f);
        sendPrefixed(player, MessageKey.of("command.water.success"));
    }

    @Command("croppy fertilize <fertilizer>")
    @Permission("croppy.admin")
    public void fertilizeTarget(
            Source source,
            @Argument(value = "fertilizer", suggestions = "fertilizer_items") Key fertilizerKey
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            cropOpt = plugin.getCore().getCropManager().getCrop(BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY() + 1, target.getZ()));
        }

        if (cropOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropInstance crop = cropOpt.get();
        crop.setFertilizerKey(fertilizerKey);

        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0.5, 0.5, 0.5), 12, 0.2, 0.2, 0.2, 0.1);
        player.playSound(target.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.1f);
        sendPrefixed(player, MessageKey.of("command.fertilize.success"), "fertilizer", fertilizerKey.asString());
    }

    @Command("croppy setstage <stageIndex>")
    @Permission("croppy.admin")
    public void setStage(
            Source source,
            @Argument("stageIndex") int stageIndex
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = plugin.getCore().getSpeciesRegistry().get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.state.species-not-found"));
            return;
        }

        Species species = speciesOpt.get();
        if (stageIndex < 0 || stageIndex >= species.getTotalStages()) {
            sendPrefixed(player, MessageKey.of("command.state.invalid-stage"), "stage", stageIndex, "max", species.getTotalStages() - 1);
            return;
        }

        crop.setStageIndex(stageIndex);
        crop.setProgress(0.0);
        StageDefinition stage = species.getStage(stageIndex);
        plugin.getDisplaySynchronizer().synchronizeDisplay(crop, species, stage);

        sendPrefixed(player, MessageKey.of("command.state.stage-updated"), "stage", stageIndex);
    }

    @Command("croppy setprogress <progress>")
    @Permission("croppy.admin")
    public void setProgress(
            Source source,
            @Argument("progress") double progress
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropInstance crop = cropOpt.get();
        crop.setProgress(Math.max(0.0, progress));

        sendPrefixed(player, MessageKey.of("command.state.progress-updated"), "progress", String.format(Locale.ROOT, "%.1f", crop.getProgress()));
    }
}
