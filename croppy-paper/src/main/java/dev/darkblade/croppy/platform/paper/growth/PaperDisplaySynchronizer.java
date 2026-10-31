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
    public void synchronizeStationDisplay(StationInstance station, World world, ItemFactory itemFactory) {
        Objects.requireNonNull(station, "station cannot be null");
        if (world == null || itemFactory == null) {
            return;
        }

        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isEmpty()) {
            return;
        }

        BlockPosition pos = station.getPosition();
        String stationType = station.getStationKey().value().toLowerCase();
        double yOffset = 1.1; // Default floating icon height
        float pitchAngle = 0.0f;
        Vector3f scale = new Vector3f(0.6f, 0.6f, 0.6f);

        if (stationType.contains("pot")) {
            // Liquid/contents inside pot interior
            yOffset = 0.35;
            scale = new Vector3f(0.55f, 0.15f, 0.55f);
        } else if (stationType.contains("skillet") || stationType.contains("pan")) {
            // Flat on the surface of the iron pressure plate
            yOffset = 0.08;
            pitchAngle = (float) Math.toRadians(90.0);
            scale = new Vector3f(0.45f, 0.45f, 0.45f);
        }

        Location loc = new Location(world, pos.x() + 0.5, pos.y() + yOffset, pos.z() + 0.5);
        DisplayComponent display = displayOpt.get();
        Optional<Key> itemKeyOpt = display.getDisplayedItem();

        Collection<Entity> nearby = world.getNearbyEntities(loc, 0.6, 0.8, 0.6,
                e -> e instanceof ItemDisplay && e.getPersistentDataContainer().has(STATION_DISPLAY_KEY, PersistentDataType.BYTE));

        if (itemKeyOpt.isEmpty()) {
            // No item to display: remove any existing displays
            for (Entity e : nearby) {
                e.remove();
            }
            display.setDirty(false);
            return;
        }

        Key itemKey = itemKeyOpt.get();
        ItemStack stack = itemFactory.createItemStack(new ItemDefinition(
                itemKey,
                "misc",
                itemKey.asString(),
                0,
                null,
                List.of(),
                java.util.Map.of()
        ), 1);

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(pitchAngle, 1, 0, 0),
                scale,
                new AxisAngle4f(0, 0, 1, 0)
        );

        if (!nearby.isEmpty()) {
            for (Entity e : nearby) {
                if (e instanceof ItemDisplay id) {
                    id.setItemStack(stack);
                    id.setTransformation(transformation);
                }
            }
        } else {
            world.spawn(loc, ItemDisplay.class, id -> {
                id.setItemStack(stack);
                id.setTransformation(transformation);
                id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                id.getPersistentDataContainer().set(STATION_DISPLAY_KEY, PersistentDataType.BYTE, (byte) 1);
            });
        }

        display.setDirty(false);
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
