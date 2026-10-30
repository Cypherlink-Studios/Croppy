package dev.darkblade.croppy.platform.paper.service;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.EnvironmentProvider;
import dev.darkblade.croppy.api.growth.UnwateredPolicy;
import dev.darkblade.croppy.api.identifier.Key;
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
import org.bukkit.Server;
import org.bukkit.World;
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
    private final FertilizerRegistry fertilizerRegistry;
    private final Server server;
    private final MiniMessage miniMessage;

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider,
            FertilizerRegistry fertilizerRegistry,
            Server server
    ) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.speciesRegistry = Objects.requireNonNull(speciesRegistry, "speciesRegistry cannot be null");
        this.environmentProvider = Objects.requireNonNull(environmentProvider, "environmentProvider cannot be null");
        this.fertilizerRegistry = fertilizerRegistry;
        this.server = server;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider,
            FertilizerRegistry fertilizerRegistry
    ) {
        this(cropManager, speciesRegistry, environmentProvider, fertilizerRegistry, null);
    }

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider
    ) {
        this(cropManager, speciesRegistry, environmentProvider, null, null);
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

        long currentDay = -1L;
        if (server != null) {
            World world = server.getWorld(position.worldName());
            if (world != null) {
                currentDay = world.getFullTime() / 24000L;
            }
        }

        CropEnvironment environment = environmentProvider.getEnvironment(position);
        return Optional.of(formatCropCard(crop, speciesOpt.get(), environment, currentDay));
    }

    public Component formatCropCard(CropInstance crop, Species species, CropEnvironment environment) {
        return formatCropCard(crop, species, environment, -1L);
    }

    /**
     * Formats a complete crop status card into a rich Kyori Adventure Component.
     */
    public Component formatCropCard(CropInstance crop, Species species, CropEnvironment environment, long currentDay) {
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

        // Daily Hydration string
        String hydrationStr;
        if (currentDay >= 0) {
            if (crop.isWateredOn(currentDay)) {
                hydrationStr = "<blue>Regado hoy 💧</blue>";
            } else {
                long dryDays = crop.getLastWateredDay() >= 0 ? (currentDay - crop.getLastWateredDay()) : (currentDay + 1);
                hydrationStr = "<red>Seco (hace " + dryDays + " día(s)) 🥀</red>";
            }
        } else {
            hydrationStr = crop.getLastWateredDay() >= 0
                    ? "<blue>Regado (Día " + crop.getLastWateredDay() + ") 💧</blue>"
                    : "<gray>Sin regar 🥀</gray>";
        }

        // Fertilizer string
        String fertilizerStr;
        Key fertKey = crop.getFertilizerKey();
        if (fertKey != null) {
            if (fertilizerRegistry != null && fertilizerRegistry.get(fertKey).isPresent()) {
                Fertilizer fert = fertilizerRegistry.get(fertKey).get();
                fertilizerStr = "<green>" + fert.name() + " (" + fert.type().name() + ")</green>";
            } else {
                fertilizerStr = "<green>" + fertKey.asString() + "</green>";
            }
        } else {
            fertilizerStr = "<gray>Ninguno</gray>";
        }

        // Unwatered policy string
        UnwateredPolicy policy = species.getUnwateredPolicy();
        String policyStr = policy != null ? policy.type().name() : "PAUSE";

        String card = """
                <gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>Crop Inspection
                <dark_gray>├─ <gold>Species: <yellow>%s <gray>(%s)
                <dark_gray>├─ <gold>Stage: <yellow>%d/%d <gray>(ID: %s)
                <dark_gray>├─ <gold>Progress: %s <gray>(%s)
                <dark_gray>├─ <gold>Hydration: %s
                <dark_gray>├─ <gold>Fertilizer: %s
                <dark_gray>├─ <gold>Unwatered Policy: <yellow>%s</yellow>
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
                        hydrationStr,
                        fertilizerStr,
                        policyStr,
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
