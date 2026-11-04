package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

/**
 * Listens for player right-clicks with the inspection tool to inspect targeted crop blocks.
 */
public class InspectionToolListener implements Listener {

    private final CroppyPlugin plugin;
    private final CropInspectorService inspectorService;
    private final PluginConfig pluginConfig;

    public InspectionToolListener(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.inspectorService = Objects.requireNonNull(inspectorService, "inspectorService cannot be null");
        this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig cannot be null");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!CropInspectorService.isInspectionTool(item, plugin)) {
            return;
        }

        // It is an inspection interaction: cancel block placement / interaction
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        BlockPosition pos = BlockPosition.of(
                clickedBlock.getWorld().getName(),
                clickedBlock.getX(),
                clickedBlock.getY(),
                clickedBlock.getZ()
        );

        Optional<Component> card = inspectorService.inspect(pos, player);
        if (card.isPresent()) {
            player.sendMessage(card.get());
        } else {
            if (plugin.getMessageService() != null) {
                plugin.getMessageService().sendPrefixedMessage(player, dev.darkblade.croppy.api.i18n.MessageKey.of("command.inspect.no-crop-found"));
            } else {
                player.sendMessage(pluginConfig.getMessage(plugin.getConfig(), "no-crop-found"));
            }
        }
    }
}
