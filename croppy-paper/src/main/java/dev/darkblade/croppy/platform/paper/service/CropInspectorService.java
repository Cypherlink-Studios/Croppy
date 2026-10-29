package dev.darkblade.croppy.platform.paper.service;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.EnvironmentProvider;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Service generating Adventure MiniMessage status cards for crop inspection.
 */
public class CropInspectorService {

    private final CropManager cropManager;
    private final SpeciesRegistry speciesRegistry;
    private final EnvironmentProvider environmentProvider;
    private final MiniMessage miniMessage;

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider
    ) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.speciesRegistry = Objects.requireNonNull(speciesRegistry, "speciesRegistry cannot be null");
        this.environmentProvider = Objects.requireNonNull(environmentProvider, "environmentProvider cannot be null");
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Inspects a crop at the specified world position and returns its formatted status card.
     *
     * @param position spatial block coordinates
     * @return formatted MiniMessage component, or empty if no crop exists at the position
     */
    public Optional<Component> inspect(BlockPosition position) {
        Optional<CropInstance> cropOpt = cropManager.getCrop(position);
        if (cropOpt.isEmpty()) {
            return Optional.empty();
        }

        CropInstance crop = cropOpt.get();
        Optional<Species> speciesOpt = speciesRegistry.get(crop.getSpeciesKey());
        if (speciesOpt.isEmpty()) {
            return Optional.of(miniMessage.deserialize(
                    "<red>Error: Crop exists but species '" + crop.getSpeciesKey().asString() + "' is not registered."
            ));
        }

        CropEnvironment environment = environmentProvider.getEnvironment(position);
        return Optional.of(formatCropCard(crop, speciesOpt.get(), environment));
    }

    /**
     * Formats a complete crop status card into a rich Kyori Adventure Component.
     */
    public Component formatCropCard(CropInstance crop, Species species, CropEnvironment environment) {
        int totalStages = Math.max(1, species.getTotalStages());
        int currentStageIdx = Math.min(crop.getStageIndex(), totalStages - 1);
        StageDefinition stage = species.getStage(currentStageIdx);
        CropRequirements req = species.getRequirements();

        int reqLight = req != null ? req.minLight() : 0;
        int reqWater = req != null ? req.waterFluidLevel() : 0;

        int currentLight = environment.lightLevel();
        int currentWater = environment.waterFluidLevel();

        boolean lightOk = currentLight >= reqLight;
        boolean waterOk = currentWater >= reqWater;

        double progressRatio;
        if (crop.isMature(species)) {
            progressRatio = 1.0;
        } else if (stage.progressRequired() > 0) {
            progressRatio = Math.min(1.0, Math.max(0.0, crop.getProgress() / stage.progressRequired()));
        } else {
            progressRatio = 1.0;
        }

        String progressBar = renderProgressBar(progressRatio, 10);
        String pctStr = String.format(Locale.ROOT, "%.1f%%", progressRatio * 100.0);

        String statusStr;
        if (crop.isMature(species)) {
            statusStr = "<aqua><bold>Mature</bold></aqua>";
        } else if (!environment.isLoaded()) {
            statusStr = "<red>Unloaded Chunk</red>";
        } else if (!lightOk && !waterOk) {
            statusStr = "<red>Stalled (Light & Water)</red>";
        } else if (!lightOk) {
            statusStr = "<red>Stalled (Insufficient Light)</red>";
        } else if (!waterOk) {
            statusStr = "<red>Stalled (Insufficient Water)</red>";
        } else {
            statusStr = "<green><bold>Growing</bold></green>";
        }

        String lightColor = lightOk ? "<green>" : "<red>";
        String waterColor = waterOk ? "<blue>" : "<red>";

        String card = """
                <gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>Crop Inspection
                <dark_gray>├─ <gold>Species: <yellow>%s <gray>(%s)
                <dark_gray>├─ <gold>Stage: <yellow>%d/%d <gray>(ID: %s)
                <dark_gray>├─ <gold>Progress: %s <gray>(%s)
                <dark_gray>├─ <gold>Light Level: %s%d <gray>(Min: %d)
                <dark_gray>├─ <gold>Water Level: %s%d/8 <gray>(Min: %d)
                <dark_gray>└─ <gold>Status: %s"""
                .formatted(
                        species.getName(),
                        species.getKey().asString(),
                        currentStageIdx + 1,
                        totalStages,
                        stage.id(),
                        progressBar,
                        pctStr,
                        lightColor,
                        currentLight,
                        reqLight,
                        waterColor,
                        currentWater,
                        reqWater,
                        statusStr
                );

        return miniMessage.deserialize(card);
    }

    /**
     * Renders a 10-bar visual progress indicator.
     * Example: [■■■■■□□□□□]
     */
    public static String renderProgressBar(double ratio, int totalBars) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int filled = (int) Math.round(clamped * totalBars);
        int empty = totalBars - filled;

        return "<dark_gray>[<green>" + "■".repeat(filled) + "<dark_gray>" + "□".repeat(empty) + "]</dark_gray>";
    }

    /**
     * Creates an ItemStack configured as the Crop Inspection Tool.
     */
    public static ItemStack createInspectionTool(Plugin plugin, PluginConfig config) {
        ItemStack item = new ItemStack(config.getToolMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(config.getToolName());
            meta.lore(config.getToolLore());
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "tool"),
                    PersistentDataType.STRING,
                    "crop_inspector"
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if the given ItemStack is the Crop Inspection Tool.
     */
    public static boolean isInspectionTool(ItemStack item, Plugin plugin) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String marker = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "tool"),
                PersistentDataType.STRING
        );
        return "crop_inspector".equals(marker);
    }
}
