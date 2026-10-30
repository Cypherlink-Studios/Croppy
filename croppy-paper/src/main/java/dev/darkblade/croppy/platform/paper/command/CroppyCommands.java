package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import dev.darkblade.croppy.platform.paper.watering.WateringCanTier;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;

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
    private final MiniMessage miniMessage;

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig,
            WateringCanService wateringCanService,
            FertilizerService fertilizerService,
            FertilizerRegistry fertilizerRegistry
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.inspectorService = Objects.requireNonNull(inspectorService, "inspectorService cannot be null");
        this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig cannot be null");
        this.wateringCanService = wateringCanService;
        this.fertilizerService = fertilizerService;
        this.fertilizerRegistry = fertilizerRegistry;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this(plugin, inspectorService, pluginConfig, null, null, null);
    }

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

    @Command("croppy tool")
    @Permission("croppy.admin")
    public void giveTool(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Only players can receive the inspection tool."));
            return;
        }

        ItemStack tool = CropInspectorService.createInspectionTool(plugin, pluginConfig);
        var leftover = player.getInventory().addItem(tool);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "tool-given"));
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
            @Argument("fertilizer") String fertilizerId
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
        Key fertKey = Key.parse(fertilizerId);
        crop.setFertilizerKey(fertKey);

        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0.5, 0.5, 0.5), 12, 0.2, 0.2, 0.2, 0.1);
        player.playSound(target.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.1f);
        player.sendMessage(miniMessage.deserialize("<green>¡Fertilizante <yellow>" + fertKey.asString() + "</yellow> aplicado al cultivo!</green>"));
    }

    @Command("croppy givecan <player> <tier>")
    @Permission("croppy.admin")
    public void giveWateringCan(
            CommandSourceStack source,
            @Argument("player") Player targetPlayer,
            @Argument("tier") String tierStr
    ) {
        CommandSender sender = source.getSender();
        if (wateringCanService == null) {
            sender.sendMessage(miniMessage.deserialize("<red>Watering can service not initialized."));
            return;
        }

        WateringCanTier tier = WateringCanTier.parse(tierStr);
        ItemStack can = wateringCanService.createWateringCan(tier);
        var leftover = targetPlayer.getInventory().addItem(can);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(rem -> targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), rem));
        }

        sender.sendMessage(miniMessage.deserialize("<green>Entregada " + tier.displayName() + " a " + targetPlayer.getName() + ".</green>"));
    }

    @Command("croppy givefertilizer <player> <fertilizer> [amount]")
    @Permission("croppy.admin")
    public void giveFertilizer(
            CommandSourceStack source,
            @Argument("player") Player targetPlayer,
            @Argument("fertilizer") String fertId,
            @Argument("amount") @Default("1") int amount
    ) {
        CommandSender sender = source.getSender();
        if (fertilizerService == null || fertilizerRegistry == null) {
            sender.sendMessage(miniMessage.deserialize("<red>Fertilizer service not initialized."));
            return;
        }

        Key key = Key.parse(fertId);
        Optional<Fertilizer> fertOpt = fertilizerRegistry.get(key);
        if (fertOpt.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize("<red>Fertilizante no encontrado: " + fertId));
            return;
        }

        Fertilizer fert = fertOpt.get();
        ItemStack item = fertilizerService.createFertilizerItem(fert);
        item.setAmount(Math.max(1, amount));

        var leftover = targetPlayer.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(rem -> targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), rem));
        }

        sender.sendMessage(miniMessage.deserialize("<green>Entregado " + amount + "x " + fert.name() + " a " + targetPlayer.getName() + ".</green>"));
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

    @Command("croppy give <player> <species> [amount]")
    @Permission("croppy.admin")
    public void giveCropItem(
            CommandSourceStack source,
            @Argument("player") Player targetPlayer,
            @Argument("species") Species species,
            @Argument("amount") @Default("1") int amount
    ) {
        CommandSender sender = source.getSender();
        if (amount <= 0) {
            sender.sendMessage(miniMessage.deserialize("<red>Amount must be greater than 0."));
            return;
        }

        ItemStack item = createSpeciesItem(species, amount);
        var leftover = targetPlayer.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(rem -> targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), rem));
        }

        sender.sendMessage(pluginConfig.getMessage(
                plugin.getConfig(),
                "give-success",
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("species", species.getName()),
                Placeholder.unparsed("player", targetPlayer.getName())
        ));
    }

    private ItemStack createSpeciesItem(Species species, int amount) {
        CropDisplay display = species.getDisplay();
        Material material = Material.WHEAT_SEEDS;
        if (display != null && display.seedItemId() != null) {
            String id = display.seedItemId().replace("minecraft:", "").toUpperCase(Locale.ROOT);
            Material matched = Material.matchMaterial(id);
            if (matched != null) {
                material = matched;
            }
        }

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize("<yellow>" + species.getName() + " Seeds</yellow>"));
            meta.lore(List.of(
                    miniMessage.deserialize("<gray>Custom Croppy agricultural seed.</gray>"),
                    miniMessage.deserialize("<dark_gray>Species: <gray>" + species.getKey().asString())
            ));
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "species"),
                    PersistentDataType.STRING,
                    species.getKey().asString()
            );
            item.setItemMeta(meta);
        }
        return item;
    }
}
