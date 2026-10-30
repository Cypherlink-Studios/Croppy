package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
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
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;

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
        this.miniMessage = MiniMessage.miniMessage();
    }

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this(plugin, inspectorService, pluginConfig, null, null, null, null, null);
    }

    // ---------------------------------------------------------------------------------------------
    // Dynamic Autocompletion Providers
    // ---------------------------------------------------------------------------------------------

    @Suggestions("item_categories")
    public List<String> suggestItemCategories(CommandContext<CommandSourceStack> context, CommandInput input) {
        if (itemRegistry == null) {
            return List.of();
        }
        return List.copyOf(itemRegistry.getCategories());
    }

    @Suggestions("category_items")
    public List<String> suggestCategoryItems(CommandContext<CommandSourceStack> context, CommandInput input) {
        if (itemRegistry == null) {
            return List.of();
        }
        String category = context.getOrDefault("category", null);
        if (category != null && !category.isBlank()) {
            return itemRegistry.getByCategory(category).stream()
                    .map(def -> def.key().asString())
                    .toList();
        }
        return itemRegistry.values().stream()
                .map(def -> def.key().asString())
                .toList();
    }

    // ---------------------------------------------------------------------------------------------
    // Unified Item Commands
    // ---------------------------------------------------------------------------------------------

    @Command("croppy item give <category> <item_id> [quantity]")
    @Permission("croppy.admin")
    public void giveItem(
            CommandSourceStack source,
            @Argument(value = "category", suggestions = "item_categories") String category,
            @Argument(value = "item_id", suggestions = "category_items") String itemId,
            @Argument("quantity") @Default("1") int quantity,
            @Flag("target") Player targetPlayer,
            @Flag("silent") boolean silent
    ) {
        CommandSender sender = source.getSender();
        Player recipient = targetPlayer;
        if (recipient == null) {
            if (sender instanceof Player p) {
                recipient = p;
            } else {
                sender.sendMessage(miniMessage.deserialize("<red>Debe especificar un jugador destinatario con --target <jugador> desde la consola.</red>"));
                return;
            }
        }

        if (itemRegistry == null || itemFactory == null) {
            sender.sendMessage(miniMessage.deserialize("<red>El sistema de ítems no está inicializado.</red>"));
            return;
        }

        Key key;
        try {
            key = itemId.contains(":") ? Key.parse(itemId) : Key.of("croppy", itemId);
        } catch (Exception e) {
            sender.sendMessage(miniMessage.deserialize("<red>Identificador de ítem inválido: " + itemId + "</red>"));
            return;
        }

        Optional<ItemDefinition> defOpt = itemRegistry.get(key);
        if (defOpt.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize("<red>Ítem no encontrado en el registro: " + key.asString() + "</red>"));
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

        if (!silent && !recipientFinal.equals(sender)) {
            recipientFinal.sendMessage(miniMessage.deserialize(
                    "<green>Has recibido <yellow>" + amount + "x</yellow> " + def.displayName() + ".</green>"
            ));
        }

        sender.sendMessage(miniMessage.deserialize(
                "<green>Entregado <yellow>" + amount + "x</yellow> " + def.displayName() + " a " + recipientFinal.getName() + ".</green>"
        ));
    }

    @Command("croppy item list [category]")
    @Permission("croppy.admin")
    public void listItemDefinitions(
            CommandSourceStack source,
            @Argument(value = "category", suggestions = "item_categories") @Default("") String category
    ) {
        CommandSender sender = source.getSender();
        if (itemRegistry == null) {
            sender.sendMessage(miniMessage.deserialize("<red>El registro de ítems no está inicializado.</red>"));
            return;
        }

        if (category != null && !category.isBlank()) {
            Collection<ItemDefinition> items = itemRegistry.getByCategory(category);
            if (items.isEmpty()) {
                sender.sendMessage(miniMessage.deserialize("<yellow>No hay ítems registrados en la categoría: " + category + "</yellow>"));
                return;
            }
            sender.sendMessage(miniMessage.deserialize("<gold><bold>=== Ítems en categoría: " + category + " (" + items.size() + ") ===</bold></gold>"));
            for (ItemDefinition def : items) {
                sender.sendMessage(miniMessage.deserialize(" <dark_gray>•</dark_gray> <yellow>" + def.key().asString() + "</yellow> - " + def.displayName()));
            }
        } else {
            Collection<ItemDefinition> all = itemRegistry.values();
            sender.sendMessage(miniMessage.deserialize("<gold><bold>=== Ítems Registrados (" + all.size() + ") ===</bold></gold>"));
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
    public void inspectRaycast(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Console must specify coordinates: /croppy inspect <x> <y> <z>"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<Component> card = inspectorService.inspect(pos);
        if (card.isPresent()) {
            player.sendMessage(card.get());
        } else {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
        }
    }

    @Command("croppy inspect <x> <y> <z>")
    @Permission("croppy.admin")
    public void inspectCoordinates(
            CommandSourceStack source,
            @Argument("x") int x,
            @Argument("y") int y,
            @Argument("z") int z
    ) {
        CommandSender sender = source.getSender();
        World world = (sender instanceof Player player) ? player.getWorld() : Bukkit.getWorlds().get(0);
        BlockPosition pos = BlockPosition.of(world.getName(), x, y, z);
        Optional<Component> card = inspectorService.inspect(pos);
        if (card.isPresent()) {
            sender.sendMessage(card.get());
        } else {
            sender.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
        }
    }

    @Command("croppy reload")
    @Permission("croppy.admin")
    public void reload(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        try {
            plugin.reloadPlugin();
            sender.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "reload-success"));
        } catch (Exception e) {
            sender.sendMessage(pluginConfig.getMessage(
                    plugin.getConfig(),
                    "reload-failed",
                    Placeholder.unparsed("error", e.getMessage() != null ? e.getMessage() : "Unknown error")
            ));
            plugin.getLogger().severe("Failed to reload Croppy: " + e.getMessage());
        }
    }

    @Command("croppy water")
    @Permission("croppy.admin")
    public void waterTarget(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Only players can execute targeting commands."));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            cropOpt = plugin.getCore().getCropManager().getCrop(BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY() + 1, target.getZ()));
        }

        if (cropOpt.isEmpty()) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
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
        player.sendMessage(miniMessage.deserialize("<aqua>¡Cultivo regado exitosamente para el día de hoy!</aqua>"));
    }

    @Command("croppy fertilize <fertilizer>")
    @Permission("croppy.admin")
    public void fertilizeTarget(
            CommandSourceStack source,
            @Argument(value = "fertilizer", suggestions = "category_items") String fertilizerId
    ) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Only players can execute targeting commands."));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            cropOpt = plugin.getCore().getCropManager().getCrop(BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY() + 1, target.getZ()));
        }

        if (cropOpt.isEmpty()) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        CropInstance crop = cropOpt.get();
        Key fertKey = fertilizerId.contains(":") ? Key.parse(fertilizerId) : Key.of("croppy", fertilizerId);
        crop.setFertilizerKey(fertKey);

        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0.5, 0.5, 0.5), 12, 0.2, 0.2, 0.2, 0.1);
        player.playSound(target.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.1f);
        player.sendMessage(miniMessage.deserialize("<green>¡Fertilizante <yellow>" + fertKey.asString() + "</yellow> aplicado al cultivo!</green>"));
    }

    @Command("croppy setstage <stageIndex>")
    @Permission("croppy.admin")
    public void setStage(
            CommandSourceStack source,
            @Argument("stageIndex") int stageIndex
    ) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Only players can execute targeting commands."));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = plugin.getCore().getSpeciesRegistry().get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<red>Species not found for this crop."));
            return;
        }

        Species species = speciesOpt.get();
        if (stageIndex < 0 || stageIndex >= species.getTotalStages()) {
            player.sendMessage(miniMessage.deserialize(
                    "<red>Invalid stage index " + stageIndex + ". Valid stages: 0 to " + (species.getTotalStages() - 1)
            ));
            return;
        }

        crop.setStageIndex(stageIndex);
        crop.setProgress(0.0);
        StageDefinition stage = species.getStage(stageIndex);
        plugin.getDisplaySynchronizer().synchronizeDisplay(crop, species, stage);

        player.sendMessage(pluginConfig.getMessage(
                plugin.getConfig(),
                "stage-updated",
                Placeholder.unparsed("stage", String.valueOf(stageIndex))
        ));
    }

    @Command("croppy setprogress <progress>")
    @Permission("croppy.admin")
    public void setProgress(
            CommandSourceStack source,
            @Argument("progress") double progress
    ) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Only players can execute targeting commands."));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        BlockPosition pos = BlockPosition.of(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = plugin.getCore().getCropManager().getCrop(pos);
        if (cropOpt.isEmpty()) {
            player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            return;
        }

        CropInstance crop = cropOpt.get();
        crop.setProgress(Math.max(0.0, progress));

        player.sendMessage(pluginConfig.getMessage(
                plugin.getConfig(),
                "progress-updated",
                Placeholder.unparsed("progress", String.format(Locale.ROOT, "%.1f", crop.getProgress()))
        ));
    }
}
