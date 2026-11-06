package dev.darkblade.croppy.integration.papi;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.CroppyCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * PlaceholderAPI expansion exposing live agricultural, soil, cooking workstation,
 * and compendium progression telemetry.
 */
public class CroppyPlaceholderExpansion extends PlaceholderExpansion {

    private final CroppyCore core;
    private final String version;
    private final int raycastDistance;

    public CroppyPlaceholderExpansion(CroppyCore core, String version, int raycastDistance) {
        this.core = Objects.requireNonNull(core, "CroppyCore cannot be null");
        this.version = Objects.requireNonNull(version, "version cannot be null");
        this.raycastDistance = Math.max(1, Math.min(raycastDistance, 32));
    }

    @Override
    public @NotNull String getIdentifier() {
        return "croppy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DarkBladeDev";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        String query = params.toLowerCase(Locale.ROOT);

        // 1. Global System Placeholders
        switch (query) {
            case "version":
                return version;
            case "total_active_crops":
                return String.valueOf(core.getCropManager().getAllCrops().size());
            case "total_active_stations":
                return String.valueOf(core.getStationManager().getAllStations().size());
        }

        // 2. Player Compendium Placeholders
        if (offlinePlayer != null) {
            if (query.equals("compendium_total")) {
                return String.valueOf(core.getRecipeRegistry().size());
            }
            if (query.equals("compendium_discovered")) {
                return String.valueOf(getDiscoveredCount(offlinePlayer));
            }
            if (query.equals("compendium_percentage")) {
                int total = core.getRecipeRegistry().size();
                if (total == 0) {
                    return "0.0%";
                }
                double pct = (getDiscoveredCount(offlinePlayer) * 100.0) / total;
                return String.format(Locale.ROOT, "%.1f%%", pct);
            }
            if (query.startsWith("compendium_is_unlocked_")) {
                String rawKey = params.substring("compendium_is_unlocked_".length());
                Key recipeKey = Key.parse(rawKey);
                KnowledgeState state = core.getRecipeDiscoveryEngine().getKnowledgeState(offlinePlayer.getUniqueId(), recipeKey);
                return state == KnowledgeState.DISCOVERED ? "true" : "false";
            }
        }

        // 3. Target Crosshair Placeholders (requires online player)
        if (query.startsWith("target_") && offlinePlayer instanceof Player onlinePlayer) {
            Block targetBlock = onlinePlayer.getTargetBlockExact(raycastDistance);
            if (targetBlock == null) {
                return query.equals("target_is_crop") || query.equals("target_is_station") ? "false" : "";
            }

            BlockPosition pos = new BlockPosition(targetBlock.getWorld().getName(), targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
            Optional<CropInstance> cropOpt = core.getCropManager().getCrop(pos);
            Optional<StationInstance> stationOpt = core.getStationManager().getStation(pos);

            // Handle Target Crop Telemetry
            if (query.equals("target_is_crop")) {
                return cropOpt.isPresent() ? "true" : "false";
            }
            if (cropOpt.isPresent()) {
                CropInstance crop = cropOpt.get();
                switch (query) {
                    case "target_crop_species":
                        return core.getSpeciesRegistry().get(crop.getSpeciesKey())
                                .map(Species::getName)
                                .orElse(crop.getSpeciesKey().asString());
                    case "target_crop_stage":
                        return String.valueOf(crop.getStageIndex());
                    case "target_crop_max_stage":
                        return core.getSpeciesRegistry().get(crop.getSpeciesKey())
                                .map(sp -> String.valueOf(Math.max(0, sp.getTotalStages() - 1)))
                                .orElse("0");
                    case "target_crop_progress":
                        return String.format(Locale.ROOT, "%.0f%%", crop.getProgress() * 100.0);
                    case "target_crop_is_watered":
                        long currentDay = targetBlock.getWorld().getFullTime() / 24000L;
                        return crop.isWateredOn(currentDay) ? "true" : "false";
                    case "target_crop_fertilizer":
                        return crop.hasFertilizer() ? crop.getFertilizerKey().asString() : "none";
                }
            }

            // Handle Target Station Telemetry
            if (query.equals("target_is_station")) {
                return stationOpt.isPresent() ? "true" : "false";
            }
            if (stationOpt.isPresent()) {
                StationInstance station = stationOpt.get();
                switch (query) {
                    case "target_station_type":
                        return station.getStationKey().asString();
                    case "target_station_temperature":
                        return station.getComponent(ThermalComponent.class)
                                .map(tc -> String.format(Locale.ROOT, "%.0f°C", tc.getTemperature()))
                                .orElse("0°C");
                    case "target_station_is_cooking":
                        return station.getComponent(ProcessComponent.class)
                                .map(pc -> pc.hasActiveProcess() ? "true" : "false")
                                .orElse("false");
                }
            }
        }

        return null;
    }

    private int getDiscoveredCount(OfflinePlayer player) {
        Map<Key, PlayerRecipeKnowledge> map = core.getRecipeDiscoveryEngine().getAllKnowledge(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (PlayerRecipeKnowledge knowledge : map.values()) {
            if (knowledge.state() == KnowledgeState.DISCOVERED) {
                count++;
            }
        }
        return count;
    }
}
