package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Paper platform evaluator detecting active heat source blocks and stations beneath crafting stations
 * and converging thermal component temperatures.
 */
public final class PaperThermalEvaluator {

    public static final double DEFAULT_AMBIENT_TEMPERATURE = 20.0;

    private final Function<BlockPosition, Optional<StationInstance>> stationLookup;

    public PaperThermalEvaluator() {
        this((Function<BlockPosition, Optional<StationInstance>>) null);
    }

    public PaperThermalEvaluator(DefaultStationManager stationManager) {
        this(stationManager != null ? stationManager::getStation : null);
    }

    public PaperThermalEvaluator(Function<BlockPosition, Optional<StationInstance>> stationLookup) {
        this.stationLookup = stationLookup;
    }

    /**
     * Immutable result of a heat source evaluation.
     *
     * @param isHeated          whether the block is actively providing heat
     * @param targetTemperature target temperature in Celsius
     */
    public record HeatSourceResult(boolean isHeated, double targetTemperature) {}

    /**
     * Evaluates a heat source at the given coordinate, prioritizing custom Croppy heat source stations
     * over vanilla block materials.
     *
     * @param posBelow   spatial position beneath the station (y - 1)
     * @param blockBelow live Bukkit block beneath the station (y - 1), or null
     * @return heat source result
     */
    public HeatSourceResult evaluateHeatSource(BlockPosition posBelow, Block blockBelow) {
        if (stationLookup != null && posBelow != null) {
            Optional<StationInstance> stationBelow = stationLookup.apply(posBelow);
            if (stationBelow.isPresent()) {
                Optional<HeatSourceComponent> heatSourceOpt = stationBelow.get().getComponent(HeatSourceComponent.class);
                if (heatSourceOpt.isPresent()) {
                    HeatSourceComponent heatSource = heatSourceOpt.get();
                    if (heatSource.isActive()) {
                        return new HeatSourceResult(true, heatSource.getOutputTemperature());
                    } else {
                        return new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
                    }
                }
            }
        }

        return evaluateBlock(blockBelow);
    }

    /**
     * Evaluates a Bukkit block directly.
     *
     * @param block the block beneath the station (y - 1)
     * @return heat source result
     */
    public HeatSourceResult evaluateBlock(Block block) {
        if (block == null) {
            return new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
        }

        boolean isLit = true;
        if (block.getBlockData() instanceof Lightable lightable) {
            isLit = lightable.isLit();
        }

        String matName = (block.getType() != null) ? block.getType().name() : "";
        return evaluateMaterial(matName, isLit);
    }

    /**
     * Evaluates a material name and its lit state.
     *
     * @param materialName material name (e.g. "CAMPFIRE", "FIRE")
     * @param isLit        whether the block is actively burning/lit
     * @return heat source result
     */
    public HeatSourceResult evaluateMaterial(String materialName, boolean isLit) {
        if (materialName == null || materialName.isBlank()) {
            return new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
        }

        String name = materialName.toUpperCase(Locale.ROOT);
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }

        switch (name) {
            case "CAMPFIRE" -> {
                return isLit ? new HeatSourceResult(true, 300.0) : new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
            }
            case "SOUL_CAMPFIRE" -> {
                return isLit ? new HeatSourceResult(true, 450.0) : new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
            }
            case "SMOKER" -> {
                return isLit ? new HeatSourceResult(true, 250.0) : new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
            }
            case "FURNACE" -> {
                return isLit ? new HeatSourceResult(true, 350.0) : new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
            }
            case "BLAST_FURNACE" -> {
                return isLit ? new HeatSourceResult(true, 400.0) : new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
            }
            case "FIRE" -> {
                return new HeatSourceResult(true, 500.0);
            }
            case "SOUL_FIRE" -> {
                return new HeatSourceResult(true, 650.0);
            }
            case "MAGMA_BLOCK" -> {
                return new HeatSourceResult(true, 600.0);
            }
            case "LAVA", "LAVA_CAULDRON" -> {
                return new HeatSourceResult(true, 1000.0);
            }
            default -> {
                return new HeatSourceResult(false, DEFAULT_AMBIENT_TEMPERATURE);
            }
        }
    }

    /**
     * Updates the thermal state of a station instance using the block below it.
     *
     * @param station the station instance
     * @param world   the Bukkit world
     * @param step    convergence step delta
     */
    public void updateThermalState(StationInstance station, World world, double step) {
        Objects.requireNonNull(station, "station cannot be null");
        if (world == null) {
            return;
        }

        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        if (thermalOpt.isEmpty()) {
            return;
        }

        BlockPosition pos = station.getPosition();
        BlockPosition posBelow = new BlockPosition(pos.worldName(), pos.x(), pos.y() - 1, pos.z());
        Block blockBelow = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());
        HeatSourceResult result = evaluateHeatSource(posBelow, blockBelow);

        ThermalComponent thermal = thermalOpt.get();
        thermal.setHeated(result.isHeated());
        thermal.setTargetTemperature(result.targetTemperature());
        thermal.converge(step);
    }
}
