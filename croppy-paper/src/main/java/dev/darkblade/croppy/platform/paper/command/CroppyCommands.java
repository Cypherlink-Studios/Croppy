package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private final MiniMessage miniMessage;

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.inspectorService = Objects.requireNonNull(inspectorService, "inspectorService cannot be null");
        this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig cannot be null");
        this.miniMessage = MiniMessage.miniMessage();
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
