package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.storage.StationStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles block placement and destruction of crafting stations, managing spatial persistence and drops.
 */
public final class StationBlockListener implements Listener {

    private final DefaultStationManager stationManager;
    private final StationRegistry stationRegistry;
    private final StationStorage stationStorage;
    private final ItemFactory itemFactory;

    public StationBlockListener(DefaultStationManager stationManager, StationRegistry stationRegistry, StationStorage stationStorage, ItemFactory itemFactory) {
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.stationRegistry = Objects.requireNonNull(stationRegistry, "stationRegistry cannot be null");
        this.stationStorage = Objects.requireNonNull(stationStorage, "stationStorage cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Optional<Key> keyOpt = itemFactory.getItemId(item);
        if (keyOpt.isEmpty()) {
            return;
        }

        Optional<StationDefinition> defOpt = stationRegistry.get(keyOpt.get());
        if (defOpt.isEmpty()) {
            return;
        }

        StationDefinition def = defOpt.get();
        Block block = event.getBlockPlaced();
        BlockPosition pos = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        DefaultStationInstance station = new DefaultStationInstance(def.key(), pos, def);
        station.setComponent(StorageComponent.class, new DefaultStorageComponent(def.inputCapacity(), def.outputCapacity()));
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent());
        if (def.hasDisplay()) {
            station.setComponent(DisplayComponent.class, new DefaultDisplayComponent());
        }

        stationManager.addStation(station);
        stationStorage.saveStation(station);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockPosition pos = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

        Optional<StationInstance> stationOpt = stationManager.removeStation(pos);
        if (stationOpt.isEmpty()) {
            return;
        }

        StationInstance station = stationOpt.get();
        stationStorage.deleteStation(pos);

        // Drop stored items into the world
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isPresent()) {
            StorageComponent storage = storageOpt.get();
            World world = block.getWorld();
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

            for (StoredItem in : storage.getInputs()) {
                world.dropItemNaturally(dropLoc, createBukkitItem(in));
            }
            for (StoredItem out : storage.getOutputs()) {
                world.dropItemNaturally(dropLoc, createBukkitItem(out));
            }
        }
    }

    private ItemStack createBukkitItem(StoredItem stored) {
        return itemFactory.createItemStack(new dev.darkblade.croppy.api.item.ItemDefinition(
                stored.itemKey(),
                "misc",
                stored.itemKey().asString(),
                0,
                "<gray>" + stored.itemKey().value() + "</gray>",
                List.of(),
                java.util.Map.of()
        ), stored.amount());
    }
}
