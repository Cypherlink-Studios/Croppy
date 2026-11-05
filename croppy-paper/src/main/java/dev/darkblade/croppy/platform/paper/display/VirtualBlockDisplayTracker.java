package dev.darkblade.croppy.platform.paper.display;

import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks active virtual 3D block displays, entity ID mappings, and player viewing distances.
 */
public class VirtualBlockDisplayTracker {

    private final Server server;
    private final PacketEventsBlockRenderer renderer;
    private final ItemFactory itemFactory;
    private final Logger logger;

    private final int viewDistance = 48;
    private final int viewDistanceSq = viewDistance * viewDistance;

    private final Map<BlockPosition, VirtualBlockDisplay> activeDisplays = new ConcurrentHashMap<>();
    private final Map<Integer, BlockPosition> interactionEntityLookup = new ConcurrentHashMap<>();

    public VirtualBlockDisplayTracker(Server server, PacketEventsBlockRenderer renderer, ItemFactory itemFactory, Logger logger) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    public VirtualBlockDisplay getOrCreateDisplay(BlockPosition pos, float yaw, Model3DDefinition model) {
        return activeDisplays.computeIfAbsent(pos, p -> {
            VirtualBlockDisplay display = new VirtualBlockDisplay(p, yaw, model);
            interactionEntityLookup.put(display.getInteractionId(), p);
            return display;
        });
    }

    public Optional<VirtualBlockDisplay> getDisplay(BlockPosition pos) {
        return Optional.ofNullable(activeDisplays.get(pos));
    }

    public Optional<VirtualBlockDisplay> getDisplayByInteractionId(int interactionId) {
        BlockPosition pos = interactionEntityLookup.get(interactionId);
        if (pos == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeDisplays.get(pos));
    }

    public void removeDisplay(BlockPosition pos) {
        VirtualBlockDisplay display = activeDisplays.remove(pos);
        if (display != null) {
            interactionEntityLookup.remove(display.getInteractionId());

            World world = server.getWorld(pos.worldName());
            if (world != null) {
                for (UUID viewerId : display.getViewers()) {
                    Player player = server.getPlayer(viewerId);
                    if (player != null && player.isOnline()) {
                        renderer.destroyBlockDisplay(player, display);
                    }
                }
            }
            display.clearViewers();
        }
    }

    public void syncDisplay(VirtualBlockDisplay display, World world) {
        if (display == null || world == null) {
            return;
        }

        BlockPosition pos = display.getPosition();
        double centerX = pos.x() + 0.5;
        double centerY = pos.y() + 0.5;
        double centerZ = pos.z() + 0.5;

        ItemStack item = null;
        if (display.getModel() != null) {
            item = resolveItemStack(display.getModel().itemModel());
        }
        if (item == null || isAir(item)) {
            try {
                item = new ItemStack(Material.PAPER);
            } catch (Throwable ignored) {}
        }

        Collection<? extends Player> players = world.getPlayers();
        for (Player player : players) {
            UUID playerId = player.getUniqueId();
            double dx = player.getLocation().getX() - centerX;
            double dy = player.getLocation().getY() - centerY;
            double dz = player.getLocation().getZ() - centerZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            boolean inRange = distSq <= viewDistanceSq;
            boolean viewing = display.isViewing(playerId);

            if (inRange && !viewing) {
                display.addViewer(playerId);
                renderer.spawnBlockDisplay(player, display, item);
            } else if (inRange && viewing) {
                renderer.updateBlockDisplay(player, display, item, true);
            } else if (!inRange && viewing) {
                display.removeViewer(playerId);
                renderer.destroyBlockDisplay(player, display);
            }
        }
    }

    /**
     * Periodically evaluates player distances for all active virtual block displays,
     * dispatching spawn packets to approaching players and destroy packets to departing players.
     */
    public void tickViewers() {
        for (VirtualBlockDisplay display : activeDisplays.values()) {
            BlockPosition pos = display.getPosition();
            World world = server.getWorld(pos.worldName());
            if (world == null) {
                continue;
            }

            int chunkX = pos.x() >> 4;
            int chunkZ = pos.z() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            // Clean up viewers that left this world or disconnected
            for (UUID viewerId : display.getViewers()) {
                Player player = server.getPlayer(viewerId);
                if (player == null || !player.isOnline() || !world.equals(player.getWorld())) {
                    display.removeViewer(viewerId);
                    if (player != null && player.isOnline()) {
                        renderer.destroyBlockDisplay(player, display);
                    }
                }
            }

            double centerX = pos.x() + 0.5;
            double centerY = pos.y() + 0.5;
            double centerZ = pos.z() + 0.5;

            ItemStack item = null;
            Collection<? extends Player> players = world.getPlayers();
            for (Player player : players) {
                if (!player.isOnline()) {
                    continue;
                }
                UUID playerId = player.getUniqueId();
                double dx = player.getLocation().getX() - centerX;
                double dy = player.getLocation().getY() - centerY;
                double dz = player.getLocation().getZ() - centerZ;
                double distSq = dx * dx + dy * dy + dz * dz;

                boolean inRange = distSq <= viewDistanceSq;
                boolean viewing = display.isViewing(playerId);

                if (inRange && !viewing) {
                    if (item == null) {
                        if (display.getModel() != null) {
                            item = resolveItemStack(display.getModel().itemModel());
                        }
                        if (item == null || isAir(item)) {
                            try {
                                item = new ItemStack(Material.PAPER);
                            } catch (Throwable ignored) {}
                        }
                    }
                    display.addViewer(playerId);
                    renderer.spawnBlockDisplay(player, display, item);
                } else if (!inRange && viewing) {
                    display.removeViewer(playerId);
                    renderer.destroyBlockDisplay(player, display);
                }
            }
        }
    }

    public void onPlayerQuit(Player player) {
        clearPlayer(player);
    }

    public void clearPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        for (VirtualBlockDisplay display : activeDisplays.values()) {
            if (display.removeViewer(playerId)) {
                renderer.destroyBlockDisplay(player, display);
            }
        }
    }

    public void clearAll() {
        for (VirtualBlockDisplay display : activeDisplays.values()) {
            World world = server.getWorld(display.getPosition().worldName());
            if (world != null) {
                for (UUID viewerId : display.getViewers()) {
                    Player player = server.getPlayer(viewerId);
                    if (player != null && player.isOnline()) {
                        renderer.destroyBlockDisplay(player, display);
                    }
                }
            }
            display.clearViewers();
        }
        activeDisplays.clear();
        interactionEntityLookup.clear();
    }

    private ItemStack resolveItemStack(dev.darkblade.croppy.api.identifier.Key itemKey) {
        if (itemKey == null) return null;
        Optional<dev.darkblade.croppy.api.item.ItemDefinition> defOpt = itemFactory.getItemRegistry().get(itemKey);
        if (defOpt.isPresent()) {
            return itemFactory.createItemStack(defOpt.get(), 1);
        }
        try {
            ItemStack item = new ItemStack(Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setItemModel(new org.bukkit.NamespacedKey(itemKey.namespace(), itemKey.value()));
                item.setItemMeta(meta);
            }
            return item;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isAir(ItemStack item) {
        if (item == null) return true;
        try {
            Material type = item.getType();
            return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
        } catch (Throwable t) {
            return false;
        }
    }

    public Map<BlockPosition, VirtualBlockDisplay> getActiveDisplays() {
        return Collections.unmodifiableMap(activeDisplays);
    }
}
