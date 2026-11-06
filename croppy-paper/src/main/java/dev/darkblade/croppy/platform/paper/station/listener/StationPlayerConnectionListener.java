package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;

/**
 * Listens for player disconnects and world transitions to release virtual display tracking state.
 */
public class StationPlayerConnectionListener implements Listener {

    private final StationViewerTracker viewerTracker;
    private final VirtualBlockDisplayTracker blockTracker;

    public StationPlayerConnectionListener(StationViewerTracker viewerTracker, VirtualBlockDisplayTracker blockTracker) {
        this.viewerTracker = Objects.requireNonNull(viewerTracker, "viewerTracker cannot be null");
        this.blockTracker = blockTracker;
    }

    public StationPlayerConnectionListener(StationViewerTracker viewerTracker) {
        this(viewerTracker, null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (blockTracker != null) {
            blockTracker.tickViewers();
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
