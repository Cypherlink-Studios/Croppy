package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;

/**
 * Listens for player connections, world transitions, and chunk delivery events
 * to manage virtual display tracking state and mask base blocks for viewing clients.
 */
public class StationPlayerConnectionListener implements Listener {

    private final StationViewerTracker viewerTracker;
    private final VirtualBlockDisplayTracker blockTracker;
    private final DefaultStationManager stationManager;

    public StationPlayerConnectionListener(
            StationViewerTracker viewerTracker,
            VirtualBlockDisplayTracker blockTracker,
            DefaultStationManager stationManager
    ) {
        this.viewerTracker = Objects.requireNonNull(viewerTracker, "viewerTracker cannot be null");
        this.blockTracker = blockTracker;
        this.stationManager = stationManager;
    }

    public StationPlayerConnectionListener(StationViewerTracker viewerTracker, VirtualBlockDisplayTracker blockTracker) {
        this(viewerTracker, blockTracker, null);
    }

    public StationPlayerConnectionListener(StationViewerTracker viewerTracker) {
        this(viewerTracker, null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (blockTracker != null) {
            blockTracker.tickViewers();
        }
        if (viewerTracker != null && stationManager != null) {
            viewerTracker.tickPlayer(event.getPlayer(), stationManager);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        if (viewerTracker != null && stationManager != null) {
            viewerTracker.maskStationsInChunk(event.getPlayer(), event.getChunk(), stationManager);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        viewerTracker.onPlayerQuit(event.getPlayer());
        if (blockTracker != null) {
            blockTracker.onPlayerQuit(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null && (event.getFrom().getWorld() != event.getTo().getWorld()
                || event.getFrom().distanceSquared(event.getTo()) > 256.0)) {
            viewerTracker.onPlayerQuit(event.getPlayer());
            if (blockTracker != null) {
                blockTracker.onPlayerQuit(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        viewerTracker.onPlayerQuit(event.getPlayer());
        if (blockTracker != null) {
            blockTracker.onPlayerQuit(event.getPlayer());
        }
    }
}
