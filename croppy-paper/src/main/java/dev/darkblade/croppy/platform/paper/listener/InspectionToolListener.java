package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.service.CropTargetResolver;
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
 * Listens for player right-clicks with the inspection tool to inspect targeted crop blocks
 * and virtual displays with line-of-sight raytracing.
 */
public class InspectionToolListener implements Listener {

    private final CroppyPlugin plugin;
    private final CropInspectorService inspectorService;
    private final CropTargetResolver targetResolver;

    public InspectionToolListener(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            CropTargetResolver targetResolver
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.inspectorService = Objects.requireNonNull(inspectorService, "inspectorService cannot be null");
        this.targetResolver = targetResolver;
    }

    public InspectionToolListener(
            CroppyPlugin plugin,
            CropInspectorService inspectorService
    ) {
        this(plugin, inspectorService, (CropTargetResolver) null);
    }

    public InspectionToolListener(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this(plugin, inspectorService, (CropTargetResolver) null);
    }

    public CropTargetResolver getTargetResolver() {
        return targetResolver;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        ItemStack item = event.getItem();
        if (!CropInspectorService.isInspectionTool(item, plugin)) {
            return;
        }

        // It is an inspection interaction: cancel block placement / interaction immediately at LOWEST priority
        event.setCancelled(true);

        Player player = event.getPlayer();
        BlockPosition targetPos = null;

        // 1. Resolve targeted crop via CropTargetResolver (raytraces virtual 3D displays, physical blocks, and supporting farmland)
        if (targetResolver != null) {
            Optional<CropTargetResolver.TargetResult> targetOpt = targetResolver.resolveTarget(player);
            if (targetOpt.isPresent()) {
                targetPos = targetOpt.get().crop().getPosition();
            }
        }

        // 2. Direct clicked block and supporting soil fallback (for direct block clicks or when targetResolver misses)
        if (targetPos == null && event.getClickedBlock() != null) {
            Block clickedBlock = event.getClickedBlock();
            BlockPosition clickedPos = BlockPosition.of(
                    clickedBlock.getWorld().getName(),
                    clickedBlock.getX(),
                    clickedBlock.getY(),
                    clickedBlock.getZ()
            );
            if (inspectorService.inspect(clickedPos, player).isPresent()) {
                targetPos = clickedPos;
            } else {
                BlockPosition abovePos = BlockPosition.of(
                        clickedBlock.getWorld().getName(),
                        clickedBlock.getX(),
                        clickedBlock.getY() + 1,
                        clickedBlock.getZ()
                );
                if (inspectorService.inspect(abovePos, player).isPresent()) {
                    targetPos = abovePos;
                }
            }
        }

        // 3. Inspect resolved crop or send localized not-found feedback
        if (targetPos != null) {
            Optional<Component> card = inspectorService.inspect(targetPos, player);
            if (card.isPresent()) {
                player.sendMessage(card.get());
                return;
            }
        }

        if (plugin.getMessageService() != null) {
            plugin.getMessageService().sendPrefixedMessage(player, dev.darkblade.croppy.api.i18n.MessageKey.of("command.inspect.no-crop-found"));
        }
    }
}
