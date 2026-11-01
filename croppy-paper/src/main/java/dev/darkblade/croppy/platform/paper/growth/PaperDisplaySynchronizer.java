package dev.darkblade.croppy.platform.paper.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Paper implementation of DisplaySynchronizer.
 * Synchronizes physical world block states and station Display Entities with spatial transforms.
 */
public class PaperDisplaySynchronizer implements DisplaySynchronizer {

    public static final NamespacedKey STATION_DISPLAY_KEY = new NamespacedKey("croppy", "station_display");

    private final Server server;
    private final Logger logger;

    public PaperDisplaySynchronizer(Server server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    public PaperDisplaySynchronizer(Server server) {
        this(server, server.getLogger());
    }

    @Override
    public void synchronizeDisplay(CropInstance crop, Species species, StageDefinition stage) {
        Objects.requireNonNull(crop, "crop must not be null");
        Objects.requireNonNull(species, "species must not be null");
        Objects.requireNonNull(stage, "stage must not be null");

        BlockPosition pos = crop.getPosition();
        World world = server.getWorld(pos.worldName());
        if (world == null) {
            return;
        }

        int chunkX = pos.x() >> 4;
        int chunkZ = pos.z() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        CropDisplay display = species.getDisplay();

        if ("vanilla".equalsIgnoreCase(display.type())) {
            synchronizeVanillaDisplay(block, display, stage);
        }
    }

    /**
     * Synchronizes the visual ItemDisplay entity for a crafting station or cookware machine.
     *
     * @param station     the station instance
     * @param world       the Bukkit world
     * @param itemFactory factory constructing ItemStacks from keys
     */
    /**
     * Legacy synchronization for station displays.
     * Prunes legacy world-persisted ItemDisplay entities to prevent ghost entities.
     * Virtual display projection is handled by {@link dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker}.
     *
     * @param station     the station instance
     * @param world       the Bukkit world
     * @param itemFactory factory constructing ItemStacks from keys
     */
    public void synchronizeStationDisplay(StationInstance station, World world, ItemFactory itemFactory) {
        Objects.requireNonNull(station, "station cannot be null");
        if (world == null) {
            return;
        }

        BlockPosition pos = station.getPosition();
        Location loc = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);

        // Prune any legacy world-persisted ItemDisplay entities
        Collection<Entity> nearby = world.getNearbyEntities(loc, 1.0, 1.5, 1.0,
                e -> e instanceof ItemDisplay && e.getPersistentDataContainer().has(STATION_DISPLAY_KEY, PersistentDataType.BYTE));
        for (Entity e : nearby) {
            e.remove();
        }

        station.getComponent(DisplayComponent.class).ifPresent(d -> d.setDirty(false));
    }

    private void synchronizeVanillaDisplay(Block block, CropDisplay display, StageDefinition stage) {
        Material expectedMaterial = resolveMaterial(display.blockId());
        if (expectedMaterial == null) {
            return;
        }

        if (block.getType() != expectedMaterial) {
            block.setType(expectedMaterial, false);
        }

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            int targetAge = Math.min(stage.blockAge(), ageable.getMaximumAge());
            ageable.setAge(targetAge);
            block.setBlockData(ageable, false);
        }
    }

    private Material resolveMaterial(String id) {
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key != null) {
            Material material = Registry.MATERIAL.get(key);
            if (material != null) {
                return material;
            }
        }
        return Material.matchMaterial(id);
    }
}
