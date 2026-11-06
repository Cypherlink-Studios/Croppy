package dev.darkblade.croppy.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.darkblade.croppy.api.protection.ProtectionAction;
import dev.darkblade.croppy.api.protection.ProtectionPort;
import dev.darkblade.croppy.api.world.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * WorldGuard 7.x protection adapter enforcing region rules across Bukkit and PacketEvents interactions.
 */
public class WorldGuardProtectionAdapter implements ProtectionPort {

    private final boolean useCustomFlags;

    public WorldGuardProtectionAdapter(boolean useCustomFlags) {
        this.useCustomFlags = useCustomFlags;
    }

    @Override
    public boolean canInteract(UUID playerId, BlockPosition position, ProtectionAction action) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(position, "position cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        org.bukkit.Server server = Bukkit.getServer();
        if (server == null) {
            return true;
        }

        World world = server.getWorld(position.worldName());
        if (world == null) {
            return true;
        }

        Location loc = new Location(world, position.x(), position.y(), position.z());
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(loc);

        Player player = Bukkit.getPlayer(playerId);
        LocalPlayer localPlayer = player != null
                ? WorldGuardPlugin.inst().wrapPlayer(player)
                : WorldGuardPlugin.inst().wrapOfflinePlayer(Bukkit.getOfflinePlayer(playerId));

        // Check region bypass permissions (e.g. worldguard.bypass.* or admin status)
        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, weWorld)) {
            return true;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        // 1. Evaluate custom Croppy flags if configured
        if (useCustomFlags) {
            StateFlag customFlag = resolveCustomFlag(action);
            if (customFlag != null) {
                StateFlag.State state = query.queryState(wgLoc, localPlayer, customFlag);
                if (state != null) {
                    return state == StateFlag.State.ALLOW;
                }
            }
        }

        // 2. Fallback to standard WorldGuard flags
        return switch (action) {
            case PLANT, HARVEST, BREAK -> query.testState(wgLoc, localPlayer, Flags.BUILD);
            case WATER, FERTILIZE, STATION_USE, STATION_FUEL -> query.testState(wgLoc, localPlayer, Flags.INTERACT);
        };
    }

    private StateFlag resolveCustomFlag(ProtectionAction action) {
        return switch (action) {
            case PLANT -> WorldGuardFlags.CROPPY_PLANT;
            case HARVEST -> WorldGuardFlags.CROPPY_HARVEST;
            case WATER -> WorldGuardFlags.CROPPY_WATER;
            case FERTILIZE -> WorldGuardFlags.CROPPY_FERTILIZE;
            case STATION_USE, STATION_FUEL -> WorldGuardFlags.CROPPY_COOK;
            case BREAK -> null; // Breaking always obeys BUILD/BREAK flags
        };
    }
}
