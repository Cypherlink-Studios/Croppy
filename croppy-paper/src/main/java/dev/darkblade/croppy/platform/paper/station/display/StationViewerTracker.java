package dev.darkblade.croppy.platform.paper.station.display;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayLayerConfig;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import dev.darkblade.croppy.api.station.visual.StationInteractionBox;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import dev.darkblade.croppy.api.station.visual.TextDisplayTemplate;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.visual.StationPlaceholderFormatter;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages virtual display lifecycles, player distance tracking, and Level-of-Detail (LoD) culling.
 */
public class StationViewerTracker {

    private final Server server;
    private final PacketEventsStationRenderer renderer;
    private final ItemFactory itemFactory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Logger logger;

    private final Map<BlockPosition, PacketStationDisplay> activeDisplays = new ConcurrentHashMap<>();

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, Logger logger) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    public PacketStationDisplay getOrCreateDisplay(StationInstance station) {
        return activeDisplays.computeIfAbsent(station.getPosition(), pos -> new PacketStationDisplay(station));
    }

    public Optional<PacketStationDisplay> getDisplay(BlockPosition pos) {
        return Optional.ofNullable(activeDisplays.get(pos));
    }

    public Optional<PacketStationDisplay> getDisplayByInteractionId(int entityId) {
        for (PacketStationDisplay display : activeDisplays.values()) {
            if (display.getInteractionId() == entityId) {
                return Optional.of(display);
            }
        }
        return Optional.empty();
    }

    public Map<BlockPosition, PacketStationDisplay> getActiveDisplays() {
        return Collections.unmodifiableMap(activeDisplays);
    }

    /**
     * Evaluates player distances and synchronizes virtual display packets for a single station.
     */
    public void syncStation(StationInstance station, World world, ProcessInstance process) {
        if (station == null || world == null) return;

        StationDefinition def = station.getDefinition();
        if (!def.hasDisplay()) return;

        Optional<DisplayComponent> displayOpt = station.getComponent(DisplayComponent.class);
        if (displayOpt.isEmpty()) return;

        DisplayComponent displayComp = displayOpt.get();
        PacketStationDisplay display = getOrCreateDisplay(station);
        StationVisualDefinition visual = def.getVisualDefinition().orElseGet(() -> createFallbackVisual(def));

        BlockPosition pos = station.getPosition();
        double centerX = pos.x() + 0.5;
        double centerY = pos.y() + 0.5;
        double centerZ = pos.z() + 0.5;

        // Thermal info
        double temp = 20.0;
        String thermalZone = displayComp.getThermalZone();
        Optional<ThermalComponent> thermalOpt = station.getComponent(ThermalComponent.class);
        if (thermalOpt.isPresent()) {
            temp = thermalOpt.get().getTemperature();
        }

        // Process info
        float progress = displayComp.getProgress();
        long remainingTicks = -1;
        String recipeName = null;
        String recipeDisplay = null;
        if (process != null && (process.getState() == ProcessState.RUNNING || process.getState() == ProcessState.AWAITING_INPUT)) {
            recipeName = process.getProcessKey().value();
            recipeDisplay = process.getProcessKey().asString();
            remainingTicks = process.getPhaseElapsedTicks();
        }

        // Displayed Item
        Optional<Key> itemKeyOpt = displayComp.getDisplayedItem();
        ItemStack processItem = itemKeyOpt.map(this::resolveItemStack).orElse(null);

        // Format Text Component
        TextDisplayTemplate textTemplate = visual.getTextDisplay().orElse(null);
        Component textComponent = null;
        if (textTemplate != null && textTemplate.enabled()) {
            String rawText = StationPlaceholderFormatter.format(
                    textTemplate.format(),
                    recipeName,
                    recipeDisplay,
                    progress,
                    temp,
                    thermalZone,
                    displayComp.getStatusText(),
                    remainingTicks
            );
            textComponent = miniMessage.deserialize(rawText);
        }

        boolean isHot = temp >= 100.0 || (thermalZone != null && (thermalZone.contains("BOIL") || thermalZone.contains("SEAR")));
        boolean isDirty = displayComp.isDirty();

        DisplayLayerConfig baseConfig = visual.getBaseModel().orElse(null);
        DisplayLayerConfig procConfig = visual.getProcessDisplay().orElse(null);
        StationInteractionBox interactBox = visual.getInteractionBox().orElse(null);
        boolean hideBaseBlock = visual.hideBaseBlock();
        Location blockLoc = hideBaseBlock ? new Location(world, pos.x(), pos.y(), pos.z()) : null;
        BlockData barrierData = hideBaseBlock ? resolveBarrierBlockData() : null;

        Collection<? extends Player> players = world.getPlayers();
        for (Player player : players) {
            UUID playerId = player.getUniqueId();
            double dx = player.getLocation().getX() - centerX;
            double dy = player.getLocation().getY() - centerY;
            double dz = player.getLocation().getZ() - centerZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            // 1. Base Model Tier
            if (baseConfig != null && baseConfig.enabled()) {
                boolean inRange = distSq <= ((long) baseConfig.viewDistance() * baseConfig.viewDistance());
                boolean viewing = display.isViewing(VisualTier.BASE_MODEL, playerId);
                if (inRange && !viewing) {
                    display.addViewer(VisualTier.BASE_MODEL, playerId);
                    ItemStack baseItem = baseConfig.getItemKey().map(this::resolveItemStack).orElse(null);
                    BlockData blockData = resolveBlockData(baseConfig.getBlockState().orElse(null));
                    renderer.spawnBaseModel(player, display, baseConfig, baseItem, blockData);
                    if (hideBaseBlock && barrierData != null && blockLoc != null) {
                        player.sendBlockChange(blockLoc, barrierData);
                    }
                } else if (!inRange && viewing) {
                    display.removeViewer(VisualTier.BASE_MODEL, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.BASE_MODEL));
                    if (hideBaseBlock && blockLoc != null) {
                        player.sendBlockChange(blockLoc, world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
                    }
                }
            }

            // 2. Process Item Tier
            if (procConfig != null && procConfig.enabled() && processItem != null) {
                boolean inRange = distSq <= ((long) procConfig.viewDistance() * procConfig.viewDistance());
                boolean viewing = display.isViewing(VisualTier.PROCESS_ITEM, playerId);
                if (inRange && !viewing) {
                    display.addViewer(VisualTier.PROCESS_ITEM, playerId);
                    renderer.spawnProcessItem(player, display, procConfig, processItem, temp, thermalZone);
                } else if (!inRange && viewing) {
                    display.removeViewer(VisualTier.PROCESS_ITEM, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.PROCESS_ITEM));
                } else if (inRange && viewing && (isDirty || isHot)) {
                    renderer.updateProcessItemMetadata(player, display, procConfig, processItem, temp, thermalZone);
                }
            } else if (display.isViewing(VisualTier.PROCESS_ITEM, playerId)) {
                // Item cleared
                display.removeViewer(VisualTier.PROCESS_ITEM, playerId);
                renderer.destroyEntities(player, display.getEntityId(VisualTier.PROCESS_ITEM));
            }

            // 3. Text Display Tier
            if (textTemplate != null && textTemplate.enabled() && textComponent != null) {
                boolean inRange = distSq <= ((long) textTemplate.viewDistance() * textTemplate.viewDistance());
                boolean viewing = display.isViewing(VisualTier.TEXT_DISPLAY, playerId);
                if (inRange && !viewing) {
                    display.addViewer(VisualTier.TEXT_DISPLAY, playerId);
                    renderer.spawnTextDisplay(player, display, textTemplate, textComponent);
                } else if (!inRange && viewing) {
                    display.removeViewer(VisualTier.TEXT_DISPLAY, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.TEXT_DISPLAY));
                } else if (inRange && viewing && isDirty) {
                    renderer.updateTextDisplayMetadata(player, display, textTemplate, textComponent);
                }
            }

            // 4. Interaction Tier
            if (interactBox != null) {
                int interactDist = (baseConfig != null) ? baseConfig.viewDistance() : 32;
                boolean inRange = distSq <= ((long) interactDist * interactDist);
                boolean viewing = display.isViewing(VisualTier.INTERACTION, playerId);
                if (inRange && !viewing) {
                    display.addViewer(VisualTier.INTERACTION, playerId);
                    renderer.spawnInteraction(player, display, interactBox);
                    if (hideBaseBlock && barrierData != null && blockLoc != null) {
                        player.sendBlockChange(blockLoc, barrierData);
                    }
                } else if (!inRange && viewing) {
                    display.removeViewer(VisualTier.INTERACTION, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.INTERACTION));
                    if (hideBaseBlock && blockLoc != null && (baseConfig == null || !display.isViewing(VisualTier.BASE_MODEL, playerId))) {
                        player.sendBlockChange(blockLoc, world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
                    }
                }
            }
        }

        displayComp.setDirty(false);
    }

    /**
     * Completely destroys virtual display entities and removes the station from active tracking.
     */
    public void destroyStation(BlockPosition pos) {
        PacketStationDisplay display = activeDisplays.remove(pos);
        if (display == null) return;

        StationDefinition def = display.getStation().getDefinition();
        boolean hideBaseBlock = def.getVisualDefinition().map(StationVisualDefinition::hideBaseBlock).orElse(false);

        int[] allIds = display.getAllEntityIds();
        for (UUID playerId : display.getAllViewers()) {
            Player player = server.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                renderer.destroyEntities(player, allIds);
                if (hideBaseBlock) {
                    World world = server.getWorld(pos.worldName());
                    if (world != null) {
                        player.sendBlockChange(new Location(world, pos.x(), pos.y(), pos.z()), world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
                    }
                }
            }
        }
        display.clearAllViewers();
    }

    /**
     * Cleans up tracking state when a player quits or teleports out.
     */
    public void onPlayerQuit(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        for (PacketStationDisplay display : activeDisplays.values()) {
            display.removePlayerFromAll(playerId);
        }
    }

    /**
     * Cleans up all stations within an unloaded chunk.
     */
    public void onChunkUnload(String worldName, int chunkX, int chunkZ) {
        for (BlockPosition pos : activeDisplays.keySet()) {
            if (pos.worldName().equals(worldName) && (pos.x() >> 4) == chunkX && (pos.z() >> 4) == chunkZ) {
                destroyStation(pos);
            }
        }
    }

    private StationVisualDefinition createFallbackVisual(StationDefinition def) {
        String keyVal = def.key().value().toLowerCase(Locale.ROOT);
        double yOffset = 1.1;
        float pitchAngle = 0.0f;
        float sx = 0.6f, sy = 0.6f, sz = 0.6f;

        if (keyVal.contains("pot")) {
            yOffset = 0.35;
            sx = 0.55f; sy = 0.15f; sz = 0.55f;
        } else if (keyVal.contains("skillet") || keyVal.contains("pan")) {
            yOffset = 0.08;
            pitchAngle = (float) Math.toRadians(90.0);
            sx = 0.45f; sy = 0.45f; sz = 0.45f;
        }

        DisplayLayerConfig proc = new DisplayLayerConfig(
                true,
                "ITEM",
                null,
                null,
                new DisplayTransform(0.0, yOffset, 0.0, pitchAngle, 0.0f, 0.0f, sx, sy, sz),
                BillboardMode.FIXED,
                24,
                0
        );

        TextDisplayTemplate text = new TextDisplayTemplate(
                true,
                TextDisplayTemplate.DEFAULT_FORMAT,
                DisplayTransform.of(0.0, 1.35, 0.0, 1.0f, 1.0f, 1.0f),
                BillboardMode.CENTER,
                8,
                200,
                0,
                false,
                true
        );

        return new StationVisualDefinition(null, proc, text);
    }

    private ItemStack resolveItemStack(Key itemKey) {
        if (itemKey == null) return null;
        Optional<ItemDefinition> defOpt = itemFactory.getItemRegistry().get(itemKey);
        if (defOpt.isPresent()) {
            return itemFactory.createItemStack(defOpt.get(), 1);
        }
        Material mat = Material.matchMaterial(itemKey.value().toUpperCase(Locale.ROOT));
        if (mat != null && !mat.isAir()) {
            return new ItemStack(mat);
        }
        return new ItemStack(Material.PAPER);
    }

    private BlockData resolveBlockData(String blockState) {
        if (blockState == null || blockState.isBlank()) return null;
        try {
            return Bukkit.createBlockData(blockState);
        } catch (Exception ignored) {
            return null;
        }
    }

    public BlockData resolveBarrierBlockData() {
        try {
            return server.createBlockData(Material.BARRIER);
        } catch (Exception ex) {
            try {
                return Bukkit.createBlockData(Material.BARRIER);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
