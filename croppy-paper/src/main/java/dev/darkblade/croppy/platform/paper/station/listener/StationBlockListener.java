package dev.darkblade.croppy.platform.paper.station.listener;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.storage.StationStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.species.RotationCalculator;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.component.DefaultDisplayComponent;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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
    private final dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker;
    private final dev.darkblade.croppy.api.protection.ProtectionPort protectionPort;
    private final CroppyLogger logger;

    public StationBlockListener(
            DefaultStationManager stationManager,
            StationRegistry stationRegistry,
            StationStorage stationStorage,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker,
            dev.darkblade.croppy.api.protection.ProtectionPort protectionPort
    ) {
        this(stationManager, stationRegistry, stationStorage, itemFactory, viewerTracker, protectionPort,
                NoOpCroppyLogger.INSTANCE);
    }

    public StationBlockListener(
            DefaultStationManager stationManager,
            StationRegistry stationRegistry,
            StationStorage stationStorage,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker,
            dev.darkblade.croppy.api.protection.ProtectionPort protectionPort,
            CroppyLogger logger
    ) {
        this.stationManager = Objects.requireNonNull(stationManager, "stationManager cannot be null");
        this.stationRegistry = Objects.requireNonNull(stationRegistry, "stationRegistry cannot be null");
        this.stationStorage = Objects.requireNonNull(stationStorage, "stationStorage cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.viewerTracker = viewerTracker;
        this.protectionPort = protectionPort;
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    public StationBlockListener(
            DefaultStationManager stationManager,
            StationRegistry stationRegistry,
            StationStorage stationStorage,
            ItemFactory itemFactory,
            dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker viewerTracker
    ) {
        this(stationManager, stationRegistry, stationStorage, itemFactory, viewerTracker, null);
    }

    public StationBlockListener(DefaultStationManager stationManager, StationRegistry stationRegistry, StationStorage stationStorage, ItemFactory itemFactory) {
        this(stationManager, stationRegistry, stationStorage, itemFactory, null, null);
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

        if (protectionPort != null && event.getPlayer() != null &&
                !protectionPort.canInteract(event.getPlayer().getUniqueId(), pos, dev.darkblade.croppy.api.protection.ProtectionAction.PLANT)) {
            event.setCancelled(true);
            return;
        }

        float playerYaw = event.getPlayer().getLocation().getYaw();
        float effectiveYaw = RotationCalculator.calculate(playerYaw + 180.0f, def.rotationMode());

        DefaultStationInstance station = new DefaultStationInstance(def.key(), pos, def, effectiveYaw);
        station.setComponent(StorageComponent.class, new DefaultStorageComponent(def.inputCapacity(), def.outputCapacity()));
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent());
        if (def.hasDisplay()) {
            station.setComponent(DisplayComponent.class, new DefaultDisplayComponent());
        }

        // Apply custom head texture to in-world skull block if applicable
        if (block.getState() instanceof Skull skull) {
            itemFactory.getItemRegistry().get(def.key()).ifPresent(itemDef -> {
                itemDef.getSkullTexture().ifPresent(rawTex -> {
                    com.destroystokyo.paper.profile.PlayerProfile profile = itemFactory.createProfileFromTexture(rawTex);
                    if (profile != null) {
                        skull.setPlayerProfile(profile);
                        skull.update(true, false);
                    }
                });
            });
        }

        boolean hasThermal = Boolean.parseBoolean(def.properties().getOrDefault("thermal", "false"))
                || Boolean.parseBoolean(def.properties().getOrDefault("has_thermal", "false"));
        if (hasThermal) {
            station.setComponent(ThermalComponent.class, new DefaultThermalComponent());
        }

        boolean isHeatSource = Boolean.parseBoolean(def.properties().getOrDefault("heat_source", "false"))
                || Boolean.parseBoolean(def.properties().getOrDefault("is_heat_source", "false"));
        if (isHeatSource) {
            double minTemp = parseDoubleOrDefault(def.properties().get("min_temperature"), DefaultHeatSourceComponent.DEFAULT_MIN_TEMP);
            double maxTemp = parseDoubleOrDefault(def.properties().get("max_temperature"), DefaultHeatSourceComponent.DEFAULT_MAX_TEMP);
            double heatingRate = parseDoubleOrDefault(def.properties().get("heating_rate"), DefaultHeatSourceComponent.DEFAULT_HEATING_RATE);
            double coolingRate = parseDoubleOrDefault(def.properties().get("cooling_rate"), DefaultHeatSourceComponent.DEFAULT_COOLING_RATE);
            double fuelMult = parseDoubleOrDefault(def.properties().get("fuel_burn_multiplier"), DefaultHeatSourceComponent.DEFAULT_FUEL_MULTIPLIER);

            station.setComponent(HeatSourceComponent.class, new DefaultHeatSourceComponent(
                    false, minTemp, DefaultHeatSourceComponent.DEFAULT_AMBIENT_TEMPERATURE, 0L, 0L,
                    minTemp, maxTemp, heatingRate, coolingRate, fuelMult
            ));
        }

        stationManager.addStation(station);
        stationStorage.saveStation(station);

        boolean hideBaseBlock = def.getVisualDefinition()
                .map(dev.darkblade.croppy.api.station.visual.StationVisualDefinition::hideBaseBlock)
                .orElse(false);
        if (hideBaseBlock) {
            try {
                event.getPlayer().sendBlockChange(block.getLocation(), org.bukkit.Material.BARRIER.createBlockData());
            } catch (Exception ex) {
                logger.trace(LogChannel.STATION, () -> "Failed to send hidden station barrier block change to "
                        + event.getPlayer().getName(), ex);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockPosition pos = new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (stationManager.getStation(pos).isEmpty()) {
            return;
        }
        if (protectionPort != null && event.getPlayer() != null &&
                !protectionPort.canInteract(event.getPlayer().getUniqueId(), pos, dev.darkblade.croppy.api.protection.ProtectionAction.BREAK)) {
            event.setCancelled(true);
            return;
        }
        dismantleStation(pos, false);
    }

    /**
     * Dismantles a station, unregistering it, destroying visual entities, playing break effects,
     * resetting the block if requested, and dropping the station item and stored contents.
     *
     * @param pos    station block position
     * @param setAir whether to set the block at this position to AIR
     */
    public void dismantleStation(BlockPosition pos, boolean setAir) {
        Optional<StationInstance> stationOpt = stationManager.removeStation(pos);
        if (stationOpt.isEmpty()) {
            return;
        }

        StationInstance station = stationOpt.get();
        stationStorage.deleteStation(pos);

        if (viewerTracker != null) {
            viewerTracker.destroyStation(pos);
        }

        World world = null;
        try {
            world = (org.bukkit.Bukkit.getServer() != null) ? org.bukkit.Bukkit.getWorld(pos.worldName()) : null;
        } catch (Exception ex) {
            logger.trace(LogChannel.STATION,
                    () -> "Unable to resolve world '" + pos.worldName() + "' during station break", ex);
        }
        if (world == null) {
            return;
        }

        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

        world.playEffect(dropLoc, org.bukkit.Effect.STEP_SOUND, block.getType());

        if (setAir && !block.getType().isAir()) {
            block.setType(org.bukkit.Material.AIR);
        }

        // Drop the station item itself
        ItemStack stationItem = createStationItem(station);
        if (stationItem != null && !stationItem.getType().isAir()) {
            world.dropItemNaturally(dropLoc, stationItem);
        }

        // Drop stored items into the world
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isPresent()) {
            StorageComponent storage = storageOpt.get();
            for (StoredItem in : storage.getInputs()) {
                world.dropItemNaturally(dropLoc, createBukkitItem(in));
            }
            for (StoredItem out : storage.getOutputs()) {
                world.dropItemNaturally(dropLoc, createBukkitItem(out));
            }
        }
    }

    public void dismantleStation(BlockPosition pos) {
        dismantleStation(pos, true);
    }

    public ItemStack createStationItem(StationInstance station) {
        Key stationKey = station.getStationKey();
        Optional<dev.darkblade.croppy.api.item.ItemDefinition> defOpt = itemFactory.getItemRegistry().get(stationKey);
        if (defOpt.isPresent()) {
            return itemFactory.createItemStack(defOpt.get(), 1);
        }
        String baseBlock = station.getDefinition().baseBlockMaterial();
        String matName = baseBlock.contains(":") ? baseBlock.substring(baseBlock.indexOf(':') + 1) : baseBlock;
        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matName.toUpperCase(java.util.Locale.ROOT));
        if (mat != null && !mat.isAir()) {
            return new ItemStack(mat, 1);
        }
        return null;
    }

    public ItemStack createBukkitItem(StoredItem stored) {
        return itemFactory.createBukkitItem(stored);
    }

    private double parseDoubleOrDefault(String val, double def) {
        if (val == null || val.isBlank()) {
            return def;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
