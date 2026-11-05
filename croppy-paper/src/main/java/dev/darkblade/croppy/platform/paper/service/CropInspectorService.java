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
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage;

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider,
            FertilizerRegistry fertilizerRegistry,
            Server server,
            PaperMessageService messageService
    ) {
        this.cropManager = Objects.requireNonNull(cropManager, "cropManager cannot be null");
        this.speciesRegistry = Objects.requireNonNull(speciesRegistry, "speciesRegistry cannot be null");
        this.environmentProvider = Objects.requireNonNull(environmentProvider, "environmentProvider cannot be null");
        this.fertilizerRegistry = fertilizerRegistry;
        this.server = server;
        this.messageService = messageService;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider,
            FertilizerRegistry fertilizerRegistry,
            Server server
    ) {
        this(cropManager, speciesRegistry, environmentProvider, fertilizerRegistry, server, null);
    }

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider,
            FertilizerRegistry fertilizerRegistry
    ) {
        this(cropManager, speciesRegistry, environmentProvider, fertilizerRegistry, null, null);
    }

    public CropInspectorService(
            CropManager cropManager,
            SpeciesRegistry speciesRegistry,
            EnvironmentProvider environmentProvider
    ) {
        this(cropManager, speciesRegistry, environmentProvider, null, null, null);
    }

    /**
     * Inspects a crop at the specified world position and returns its formatted status card.
     *
     * @param position spatial block coordinates
     * @return formatted MiniMessage component, or empty if no crop exists at the position
     */
    public Optional<Component> inspect(BlockPosition position) {
        return inspect(position, null);
    }

    public Optional<Component> inspect(BlockPosition position, Audience audience) {
        Optional<CropInstance> cropOpt = cropManager.getCropAt(position);
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
            World world = server.getWorld(crop.getPosition().worldName());
            if (world != null) {
                currentDay = world.getFullTime() / 24000L;
            }
        }

        CropEnvironment environment = environmentProvider.getEnvironment(crop.getPosition());
        return Optional.of(formatCropCard(crop, speciesOpt.get(), environment, currentDay, audience));
    }

    public Component formatCropCard(CropInstance crop, Species species, CropEnvironment environment) {
        return formatCropCard(crop, species, environment, -1L, null);
    }

    public Component formatCropCard(CropInstance crop, Species species, CropEnvironment environment, long currentDay) {
        return formatCropCard(crop, species, environment, currentDay, null);
    }

    /**
     * Formats a complete crop status card into a rich Kyori Adventure Component.
     */
    public Component formatCropCard(CropInstance crop, Species species, CropEnvironment environment, long currentDay, Audience audience) {
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
            statusStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.states.mature")))
                    : "<aqua><bold>Mature</bold></aqua>";
        } else if (!environment.isLoaded()) {
            statusStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.states.unloaded")))
                    : "<red>Unloaded Chunk</red>";
        } else if (!lightOk && !waterOk) {
            statusStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.states.stalled-both")))
                    : "<red>Stalled (Light & Water)</red>";
        } else if (!lightOk) {
            statusStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.states.stalled-light")))
                    : "<red>Stalled (Insufficient Light)</red>";
        } else if (!waterOk) {
            statusStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.states.stalled-water")))
                    : "<red>Stalled (Insufficient Water)</red>";
        } else {
            statusStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.states.growing")))
                    : "<green><bold>Growing</bold></green>";
        }

        NamedTextColor lightColor = lightOk ? NamedTextColor.GREEN : NamedTextColor.RED;
        NamedTextColor waterColor = waterOk ? NamedTextColor.BLUE : NamedTextColor.RED;

        // Daily Hydration string
        String hydrationStr;
        if (currentDay >= 0) {
            if (crop.isWateredOn(currentDay)) {
                hydrationStr = (messageService != null)
                        ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.hydration-states.watered-today")))
                        : "<blue>Watered today 💧</blue>";
            } else {
                long dryDays = crop.getLastWateredDay() >= 0 ? (currentDay - crop.getLastWateredDay()) : (currentDay + 1);
                hydrationStr = (messageService != null)
                        ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.hydration-states.dry-days"), PaperMessageService.arg("days", dryDays)))
                        : "<red>Dry (" + dryDays + " day(s) ago) 🥀</red>";
            }
        } else {
            hydrationStr = crop.getLastWateredDay() >= 0
                    ? ((messageService != null)
                            ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.hydration-states.watered-day"), PaperMessageService.arg("day", crop.getLastWateredDay())))
                            : "<blue>Watered (Day " + crop.getLastWateredDay() + ") 💧</blue>")
                    : ((messageService != null)
                            ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.hydration-states.unwatered")))
                            : "<gray>Unwatered 🥀</gray>");
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
            fertilizerStr = (messageService != null)
                    ? miniMessage.serialize(messageService.render(audience, MessageKey.of("inspector.none")))
                    : "<gray>None</gray>";
        }

        // Unwatered policy string
        UnwateredPolicy policy = species.getUnwateredPolicy();
        String policyStr = policy != null ? policy.type().name() : "PAUSE";

        if (messageService != null) {
            Component title = messageService.render(audience, MessageKey.of("inspector.title"));
            Component speciesName = messageService.resolveSpeciesDisplayName(audience, species);
            Component speciesLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.species"),
                    PaperMessageService.args("name", speciesName, "key", species.getKey().asString())
            );
            Component stageLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.stage"),
                    PaperMessageService.args("current", currentStageIdx + 1, "total", totalStages, "id", stage.id())
            );
            Component progressLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.progress"),
                    PaperMessageService.args("bar", miniMessage.deserialize(progressBar), "percent", pctStr)
            );
            Component hydrationLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.hydration"),
                    PaperMessageService.arg("status", miniMessage.deserialize(hydrationStr))
            );
            Component fertilizerLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.fertilizer"),
                    PaperMessageService.arg("status", miniMessage.deserialize(fertilizerStr))
            );
            Component policyLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.policy"),
                    PaperMessageService.arg("policy", policyStr)
            );
            Component lightLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.light"),
                    PaperMessageService.args("color", lightColor, "level", currentLight, "min", reqLight)
            );
            Component waterLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.water"),
                    PaperMessageService.args("color", waterColor, "level", currentWater, "min", reqWater)
            );
            Component statusLine = messageService.render(
                    audience,
                    MessageKey.of("inspector.status"),
                    PaperMessageService.arg("status", miniMessage.deserialize(statusStr))
            );

            return Component.empty()
                    .append(title).append(Component.newline())
                    .append(speciesLine).append(Component.newline())
                    .append(stageLine).append(Component.newline())
                    .append(progressLine).append(Component.newline())
                    .append(hydrationLine).append(Component.newline())
                    .append(fertilizerLine).append(Component.newline())
                    .append(policyLine).append(Component.newline())
                    .append(lightLine).append(Component.newline())
                    .append(waterLine).append(Component.newline())
                    .append(statusLine);
        }

        String lightColorTag = "<" + lightColor.toString() + ">";
        String waterColorTag = "<" + waterColor.toString() + ">";

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
                        lightColorTag,
                        currentLight,
                        reqLight,
                        waterColorTag,
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

        // 1. Check standardized croppy:item_id
        String itemId = meta.getPersistentDataContainer().get(
                new NamespacedKey("croppy", "item_id"),
                PersistentDataType.STRING
        );
        if ("croppy:inspection_tool".equalsIgnoreCase(itemId)) {
            return true;
        }

        // 2. Check legacy tool key
        String marker = meta.getPersistentDataContainer().get(
                new NamespacedKey("croppy", "tool"),
                PersistentDataType.STRING
        );
        return "crop_inspector".equals(marker);
    }
}
