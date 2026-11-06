package dev.darkblade.croppy.platform.paper.station.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.station.display.PacketStationDisplay;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Inbound PacketEvents listener intercepting player clicks on station virtual Interaction entities,
 * routing ATTACK to station dismantling and INTERACT to station cooking / GUI interactions.
 */
public class StationVirtualInteractionPacketListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final StationViewerTracker viewerTracker;
    private final StationBlockListener blockListener;
    private final StationInteractListener interactListener;
    private final Logger logger;
    private final dev.darkblade.croppy.api.protection.ProtectionPort protectionPort;

    public StationVirtualInteractionPacketListener(
            Plugin plugin,
            StationViewerTracker viewerTracker,
            StationBlockListener blockListener,
            StationInteractListener interactListener,
            Logger logger,
            dev.darkblade.croppy.api.protection.ProtectionPort protectionPort
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.viewerTracker = Objects.requireNonNull(viewerTracker, "viewerTracker cannot be null");
        this.blockListener = Objects.requireNonNull(blockListener, "blockListener cannot be null");
        this.interactListener = Objects.requireNonNull(interactListener, "interactListener cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.protectionPort = protectionPort;
    }

    public StationVirtualInteractionPacketListener(
            Plugin plugin,
            StationViewerTracker viewerTracker,
            StationBlockListener blockListener,
            StationInteractListener interactListener,
            Logger logger
    ) {
        this(plugin, viewerTracker, blockListener, interactListener, logger, null);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();

        Optional<PacketStationDisplay> displayOpt = viewerTracker.getDisplayByInteractionId(entityId);
        if (displayOpt.isEmpty()) {
            return;
        }

        // Cancel vanilla entity interaction packet processing
        event.setCancelled(true);

        Player player = (Player) event.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        WrapperPlayClientInteractEntity.InteractAction action = packet.getAction();
        InteractionHand hand = packet.getHand();

        // Avoid double-firing on off-hand for right-click interactions
        if (action != WrapperPlayClientInteractEntity.InteractAction.ATTACK && hand == InteractionHand.OFF_HAND) {
            return;
        }

        PacketStationDisplay display = displayOpt.get();
        StationInstance station = display.getStation();
        BlockPosition pos = display.getPosition();

        // Dispatch on Bukkit main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            if (protectionPort != null) {
                dev.darkblade.croppy.api.protection.ProtectionAction protAction =
                        (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK)
                                ? dev.darkblade.croppy.api.protection.ProtectionAction.BREAK
                                : dev.darkblade.croppy.api.protection.ProtectionAction.STATION_USE;
                if (!protectionPort.canInteract(player.getUniqueId(), pos, protAction)) {
                    return;
                }
            }

            if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                // Left-click attack -> dismantle station
                blockListener.dismantleStation(pos, true);
            } else {
                // Right-click interact -> cooking / GUI
                interactListener.interactWithStation(player, station);
            }
        });
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.BLOCK_CHANGE
                && event.getPacketType() != PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            return;
        }

        try {
            if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                handleBlockChange(event);
            } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                handleMultiBlockChange(event);
            }
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.FINE, "Error rewriting block change packet for station", ex);
        }
    }

    private void handleBlockChange(PacketSendEvent event) {
        WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
        Vector3i pos = packet.getBlockPosition();

        Player player = (Player) event.getPlayer();
        if (player == null) {
            event.setLastUsedWrapper(null);
            return;
        }

        BlockPosition blockPos = new BlockPosition(player.getWorld().getName(), pos.getX(), pos.getY(), pos.getZ());
        Optional<PacketStationDisplay> displayOpt = viewerTracker.getDisplay(blockPos);
        if (displayOpt.isEmpty()) {
            event.setLastUsedWrapper(null);
            return;
        }

        StationInstance station = displayOpt.get().getStation();
        boolean hideBaseBlock = station.getDefinition().getVisualDefinition()
                .map(StationVisualDefinition::hideBaseBlock)
                .orElse(false);

        if (hideBaseBlock) {
            BlockData barrierData = viewerTracker.resolveBarrierBlockData();
            if (barrierData != null) {
                int barrierGlobalId = SpigotConversionUtil.fromBukkitBlockData(barrierData).getGlobalId();
                packet.setBlockID(barrierGlobalId);
                event.markForReEncode(true);
            } else {
                event.setLastUsedWrapper(null);
            }
        } else {
            event.setLastUsedWrapper(null);
        }
    }

    private void handleMultiBlockChange(PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
        Player player = (Player) event.getPlayer();
        if (player == null) {
            event.setLastUsedWrapper(null);
            return;
        }

        WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks = packet.getBlocks();
        if (blocks == null || blocks.length == 0) {
            event.setLastUsedWrapper(null);
            return;
        }

        boolean modified = false;
        BlockData barrierData = null;
        Integer barrierGlobalId = null;

        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            BlockPosition blockPos = new BlockPosition(player.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            Optional<PacketStationDisplay> displayOpt = viewerTracker.getDisplay(blockPos);
            if (displayOpt.isPresent()) {
                StationInstance station = displayOpt.get().getStation();
                boolean hideBaseBlock = station.getDefinition().getVisualDefinition()
                        .map(StationVisualDefinition::hideBaseBlock)
                        .orElse(false);

                if (hideBaseBlock) {
                    if (barrierGlobalId == null) {
                        barrierData = viewerTracker.resolveBarrierBlockData();
                        if (barrierData != null) {
                            barrierGlobalId = SpigotConversionUtil.fromBukkitBlockData(barrierData).getGlobalId();
                        }
                    }
                    if (barrierGlobalId != null) {
                        block.setBlockId(barrierGlobalId);
                        modified = true;
                    }
                }
            }
        }

        if (modified) {
            event.markForReEncode(true);
        } else {
            event.setLastUsedWrapper(null);
        }
    }
}
