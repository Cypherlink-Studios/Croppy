package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.storage.StationStorage;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.platform.paper.display.VirtualBlockDisplayTracker;
import dev.darkblade.croppy.platform.paper.station.display.StationViewerTracker;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Coordinates in-memory crop and crafting station instances with chunk loading and unloading.
 */
public class ChunkLifecycleListener implements Listener {

    private final CropManager cropManager;
    private final CropStorage cropStorage;
    private final SpeciesRegistry speciesRegistry;
    private final DisplaySynchronizer displaySynchronizer;
    private final VirtualBlockDisplayTracker displayTracker;
    private final DefaultStationManager stationManager;
    private final StationStorage stationStorage;
    private final StationViewerTracker viewerTracker;
    private final CroppyLogger logger;

    public ChunkLifecycleListener(
            CropManager cropManager,
            CropStorage cropStorage,
            SpeciesRegistry speciesRegistry,
            DisplaySynchronizer displaySynchronizer,
            VirtualBlockDisplayTracker displayTracker,
            DefaultStationManager stationManager,
            StationStorage stationStorage,
            StationViewerTracker viewerTracker,
            CroppyLogger logger
    ) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.cropStorage = Objects.requireNonNull(cropStorage, "cropStorage cannot be null");
        this.speciesRegistry = speciesRegistry;
        this.displaySynchronizer = displaySynchronizer;
        this.displayTracker = displayTracker;
        this.stationManager = stationManager;
        this.stationStorage = stationStorage;
        this.viewerTracker = viewerTracker;
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    public ChunkLifecycleListener(
            CropManager cropManager,
            CropStorage cropStorage,
            SpeciesRegistry speciesRegistry,
            DisplaySynchronizer displaySynchronizer,
            VirtualBlockDisplayTracker displayTracker,
            DefaultStationManager stationManager,
            StationStorage stationStorage,
            StationViewerTracker viewerTracker,
            Logger logger
    ) {
        this(
                cropManager,
                cropStorage,
                speciesRegistry,
                displaySynchronizer,
                displayTracker,
                stationManager,
                stationStorage,
                viewerTracker,
                new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null"))
        );
    }

    public ChunkLifecycleListener(
            CropManager cropManager,
            CropStorage cropStorage,
            DefaultStationManager stationManager,
            StationStorage stationStorage,
            StationViewerTracker viewerTracker,
            Logger logger
    ) {
        this(cropManager, cropStorage, null, null, null, stationManager, stationStorage, viewerTracker, logger);
    }

    public ChunkLifecycleListener(
            CropManager cropManager,
            CropStorage cropStorage,
            DefaultStationManager stationManager,
            StationStorage stationStorage,
            Logger logger
    ) {
        this(cropManager, cropStorage, null, null, null, stationManager, stationStorage, null, logger);
    }

    public ChunkLifecycleListener(CropManager cropManager, CropStorage cropStorage, Logger logger) {
        this(cropManager, cropStorage, null, null, null, null, null, null, logger);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // 1. Restore crops
        try {
            Collection<CropInstance> crops = cropStorage.loadChunk(worldName, chunkX, chunkZ);
            for (CropInstance crop : crops) {
                if (!cropManager.hasCrop(crop.getPosition())) {
                    Species species = (speciesRegistry != null)
                            ? speciesRegistry.get(crop.getSpeciesKey()).orElse(null)
                            : null;
                    int height = (species != null) ? species.getHeight() : 1;
                    cropManager.registerCrop(crop, height);

                    if (species != null && displaySynchronizer != null && !species.getStages().isEmpty()) {
                        int stageIdx = Math.min(crop.getStageIndex(), species.getStages().size() - 1);
                        displaySynchronizer.synchronizeDisplay(crop, species, species.getStages().get(stageIdx));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(LogChannel.STORAGE, "Error loading crops for chunk [" + chunkX + ", " + chunkZ + "]", e);
        }

        // 2. Restore stations
        if (stationManager != null && stationStorage != null) {
            try {
                Collection<StationInstance> stations = stationStorage.loadChunk(worldName, chunkX, chunkZ);
                for (StationInstance station : stations) {
                    if (stationManager.getStation(station.getPosition()).isEmpty()) {
                        stationManager.addStation(station);
                    }
                }
            } catch (Exception e) {
                logger.error(LogChannel.STORAGE, "Error loading stations for chunk [" + chunkX + ", " + chunkZ + "]", e);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // 1. Persist & evict crops
        try {
            List<CropInstance> chunkCrops = new ArrayList<>();
            for (CropInstance crop : cropManager.getAllCrops()) {
                if (crop.getPosition().worldName().equals(worldName)
                        && (crop.getPosition().x() >> 4) == chunkX
                        && (crop.getPosition().z() >> 4) == chunkZ) {
                    chunkCrops.add(crop);
                }
            }

            cropStorage.saveChunk(worldName, chunkX, chunkZ, chunkCrops);

            for (CropInstance crop : chunkCrops) {
                cropManager.removeCrop(crop.getPosition());
                if (displayTracker != null) {
                    displayTracker.removeDisplay(crop.getPosition());
                }
            }
        } catch (Exception e) {
            logger.error(LogChannel.STORAGE, "Error saving crops for unloaded chunk [" + chunkX + ", " + chunkZ + "]", e);
        }

        // 2. Persist & evict stations
        if (stationManager != null && stationStorage != null) {
            try {
                List<StationInstance> chunkStations = new ArrayList<>();
                for (StationInstance station : stationManager.getAllStations()) {
                    if (station.getPosition().worldName().equals(worldName)
                            && (station.getPosition().x() >> 4) == chunkX
                            && (station.getPosition().z() >> 4) == chunkZ) {
                        chunkStations.add(station);
                    }
                }

                stationStorage.saveChunk(worldName, chunkX, chunkZ, chunkStations);

                for (StationInstance station : chunkStations) {
                    stationManager.removeStation(station.getPosition());
                }
            } catch (Exception e) {
                logger.error(LogChannel.STORAGE, "Error saving stations for unloaded chunk [" + chunkX + ", " + chunkZ + "]", e);
            }
        }

        if (viewerTracker != null) {
            viewerTracker.onChunkUnload(worldName, chunkX, chunkZ);
        }
    }
}
