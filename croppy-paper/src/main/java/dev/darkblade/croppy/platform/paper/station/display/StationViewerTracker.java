package dev.darkblade.croppy.platform.paper.station.display;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.rule.RuleAction;
import dev.darkblade.croppy.api.rule.RuleTrigger;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StationStateDefinition;
import dev.darkblade.croppy.api.station.component.DisplayComponent;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayLayerConfig;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import dev.darkblade.croppy.api.station.visual.StationInteractionBox;
import dev.darkblade.croppy.api.station.visual.StationModelOverride;
import dev.darkblade.croppy.api.station.visual.StationVisualDefinition;
import dev.darkblade.croppy.api.station.visual.TextDisplayTemplate;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.api.world.ChunkPosition;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.rule.RuleExecution;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.StationStateEvaluator;
import dev.darkblade.croppy.core.station.visual.StationPlaceholderFormatter;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final CroppyLogger logger;
    private final ProcessRegistry processRegistry;
    private final RuleEngine ruleEngine;

    private final Map<BlockPosition, PacketStationDisplay> activeDisplays = new ConcurrentHashMap<>();

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, CroppyLogger logger, ProcessRegistry processRegistry, RuleEngine ruleEngine) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.processRegistry = processRegistry;
        this.ruleEngine = ruleEngine;
    }

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, CroppyLogger logger, ProcessRegistry processRegistry) {
        this(server, renderer, itemFactory, logger, processRegistry, null);
    }

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, CroppyLogger logger) {
        this(server, renderer, itemFactory, logger, null, null);
    }

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, Logger logger, ProcessRegistry processRegistry, RuleEngine ruleEngine) {
        this(server, renderer, itemFactory, new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null")), processRegistry, ruleEngine);
    }

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, Logger logger, ProcessRegistry processRegistry) {
        this(server, renderer, itemFactory, logger, processRegistry, null);
    }

    public StationViewerTracker(Server server, PacketEventsStationRenderer renderer, ItemFactory itemFactory, Logger logger) {
        this(server, renderer, itemFactory, logger, null, null);
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

        // Storage info
        boolean hasInputs = false;
        boolean hasOutputs = false;
        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isPresent()) {
            hasInputs = !storageOpt.get().getInputs().isEmpty();
            hasOutputs = !storageOpt.get().getOutputs().isEmpty();
        }

        // Process info
        float progress = displayComp.getProgress();
        long remainingTicks = -1;
        long elapsedTicks = -1;
        String recipeName = null;
        String recipeDisplay = null;
        String phaseName = null;
        int phaseIndex = 1;
        int phaseTotal = 1;
        String stateStr = "IDLE";

        boolean hasProcess = process != null;
        ProcessState pState = hasProcess ? process.getState() : ProcessState.IDLE;

        if (hasProcess && (pState == ProcessState.RUNNING || pState == ProcessState.AWAITING_INPUT)) {
            stateStr = pState.name();
            recipeName = process.getProcessKey().value();
            recipeDisplay = process.getProcessKey().asString();
            elapsedTicks = process.getPhaseElapsedTicks();

            Optional<ProcessDefinition> defOpt = (processRegistry != null)
                    ? processRegistry.get(process.getProcessKey())
                    : Optional.empty();
            if (defOpt.isPresent()) {
                ProcessDefinition pDef = defOpt.get();
                recipeDisplay = pDef.displayName();
                phaseTotal = pDef.phases().size();
                int idx = process.getCurrentPhaseIndex();
                phaseIndex = idx + 1;
                if (idx < pDef.phases().size()) {
                    PhaseDefinition phaseDef = pDef.phases().get(idx);
                    phaseName = (phaseDef.properties() != null && phaseDef.properties().containsKey("name"))
                            ? phaseDef.properties().get("name")
                            : phaseDef.id();
                    long targetTicks = phaseDef.durationTicks() > 0 ? phaseDef.durationTicks() : phaseDef.timeoutTicks();
                    if (targetTicks > 0) {
                        remainingTicks = Math.max(0, targetTicks - elapsedTicks);
                    }
                }
            }
        } else if (hasProcess && pState == ProcessState.COMPLETED) {
            stateStr = "COMPLETED";
            recipeName = process.getProcessKey().value();
            recipeDisplay = process.getProcessKey().asString();
        } else {
            recipeName = def.displayName();
            recipeDisplay = def.displayName();
        }

        // Displayed Item
        Optional<Key> itemKeyOpt = displayComp.getDisplayedItem();
        ItemStack processItem = itemKeyOpt.map(this::resolveItemStack).orElse(null);

        // Format Text Component and Evaluate Adaptive Headroom Rules
        TextDisplayTemplate textTemplate = visual.getTextDisplay().orElse(null);
        Component textComponent = null;
        String rawText = null;

        Double cachedHeadroom = display.getCachedHeadroom();
        double headroom;
        if (cachedHeadroom != null) {
            headroom = cachedHeadroom;
        } else {
            headroom = calculateHeadroom(station, world);
            display.setCachedHeadroom(headroom);
        }

        DisplayTransform effectiveTextTransform = (textTemplate != null) ? textTemplate.transform() : DisplayTransform.DEFAULT;
        String effectiveFormat = (textTemplate != null) ? textTemplate.format() : TextDisplayTemplate.DEFAULT_FORMAT;
        boolean fallbackActive = false;
        Map<String, Object> fallbackActionParams = null;

        if (ruleEngine != null && textTemplate != null) {
            EvaluationContext layoutCtx = EvaluationContext.empty()
                    .with("display.headroom", headroom)
                    .with("station.temperature", temp)
                    .with("station.thermal_zone", thermalZone != null ? thermalZone : "COLD")
                    .with("process.state", stateStr)
                    .with("process.progress", (double) progress)
                    .with("has_inputs", hasInputs)
                    .with("has_outputs", hasOutputs);

            List<RuleExecution> layoutExecs = ruleEngine.evaluateAndApply(RuleTrigger.DISPLAY_LAYOUT, station, layoutCtx);
            if (!layoutExecs.isEmpty()) {
                RuleExecution layoutRule = layoutExecs.get(0);
                for (RuleAction action : layoutRule.appliedActions()) {
                    String actionType = action.type().toUpperCase(Locale.ROOT);
                    if ("TEXT_DISPLAY_FALLBACK".equals(actionType)) {
                        fallbackActive = true;
                        fallbackActionParams = action.parameters();
                    } else if ("TEXT_DISPLAY_FIT".equals(actionType)) {
                        double stationTop = 0.55;
                        if (visual.getBaseModel().isPresent()) {
                            stationTop = visual.getBaseModel().get().transform().translationY() + 0.05;
                        }
                        double targetY = stationTop + (headroom / 2.0);
                        effectiveTextTransform = effectiveTextTransform.withTranslation(
                                effectiveTextTransform.translationX(),
                                targetY,
                                effectiveTextTransform.translationZ()
                        );
                    } else if ("TEXT_DISPLAY_SCALE".equals(actionType)) {
                        Object rawScale = action.parameters().get("scale");
                        float scaleFactor = 0.75f;
                        if (rawScale instanceof Number n) {
                            scaleFactor = n.floatValue();
                        }
                        effectiveTextTransform = effectiveTextTransform.withScale(
                                scaleFactor,
                                scaleFactor,
                                scaleFactor
                        );
                        Object rawFormat = action.parameters().get("format");
                        if (rawFormat != null && !rawFormat.toString().isBlank()) {
                            effectiveFormat = rawFormat.toString().trim();
                        }
                    }
                }
            }
        }

        display.setActionFallbackActive(fallbackActive);
        display.setFallbackParams(fallbackActionParams);

        DisplayTransform previousTransform = display.getEffectiveTextTransform();
        boolean transformChanged = !Objects.equals(previousTransform, effectiveTextTransform);
        if (transformChanged) {
            display.setEffectiveTextTransform(effectiveTextTransform);
        }

        if (textTemplate != null && textTemplate.enabled()) {
            String statusText = displayComp.getStatusText();

            if (hasProcess && (pState == ProcessState.RUNNING || pState == ProcessState.AWAITING_INPUT)) {
                if (effectiveFormat.contains("{status_message}")) {
                    rawText = StationPlaceholderFormatter.format(
                            effectiveFormat,
                            recipeName,
                            recipeDisplay,
                            progress,
                            temp,
                            thermalZone,
                            statusText,
                            remainingTicks,
                            elapsedTicks,
                            phaseName,
                            phaseIndex,
                            phaseTotal,
                            stateStr
                    );
                } else if (statusText != null && !statusText.isBlank()) {
                    rawText = statusText;
                } else {
                    rawText = StationPlaceholderFormatter.format(
                            effectiveFormat,
                            recipeName,
                            recipeDisplay,
                            progress,
                            temp,
                            thermalZone,
                            statusText,
                            remainingTicks,
                            elapsedTicks,
                            phaseName,
                            phaseIndex,
                            phaseTotal,
                            stateStr
                    );
                }
            } else if (hasProcess && pState == ProcessState.COMPLETED) {
                if (statusText != null && !statusText.isBlank()) {
                    rawText = statusText;
                } else {
                    rawText = "<green><bold>Cooking Complete!</bold></green>\n<gray>Ready to serve</gray>";
                }
            } else {
                rawText = StationPlaceholderFormatter.format(
                        effectiveFormat,
                        recipeName,
                        recipeDisplay,
                        progress,
                        temp,
                        thermalZone,
                        statusText,
                        remainingTicks,
                        elapsedTicks,
                        phaseName,
                        phaseIndex,
                        phaseTotal,
                        stateStr
                );
            }

            textComponent = miniMessage.deserialize(rawText);
        }

        // Idle & Visibility Evaluation
        boolean isCold = temp <= 25.0;
        boolean isProcessActive = hasProcess && pState != ProcessState.IDLE;
        boolean isIdle = !isProcessActive && !hasInputs && !hasOutputs && isCold;
        boolean textDisplayVisible = textTemplate != null
                && textTemplate.enabled()
                && !fallbackActive
                && (!textTemplate.hideWhenIdle() || !isIdle);

        Component lastRenderedText = display.getLastRenderedText();
        boolean textChanged = !Objects.equals(lastRenderedText, textComponent);
        if (textChanged) {
            display.setLastRenderedText(textComponent);
        }

        boolean isHot = temp >= 100.0 || (thermalZone != null && (thermalZone.contains("BOIL") || thermalZone.contains("SEAR")));
        boolean isDirty = displayComp.isDirty();

        DisplayLayerConfig baseConfig = visual.getBaseModel().orElse(null);
        DisplayLayerConfig procConfig = visual.getProcessDisplay().orElse(null);
        StationInteractionBox interactBox = visual.getInteractionBox().orElse(null);
        boolean hideBaseBlock = visual.hideBaseBlock();
        Location blockLoc = hideBaseBlock ? new Location(world, pos.x(), pos.y(), pos.z()) : null;
        BlockData barrierData = hideBaseBlock ? resolveBarrierBlockData() : null;

        // Station State evaluation
        Optional<StationStateDefinition> activeStateOpt = StationStateEvaluator.evaluate(station, process);
        String activeStateId = activeStateOpt.map(StationStateDefinition::id).orElse("default");
        String previousStateId = station.getActiveStateId();
        boolean stateChanged = !Objects.equals(previousStateId, activeStateId);
        if (stateChanged) {
            station.setActiveStateId(activeStateId);
        }

        StationModelOverride modelOverride = activeStateOpt.flatMap(StationStateDefinition::getModel).orElse(null);
        DisplayTransform effectiveBaseTransform = (modelOverride != null && modelOverride.transform() != null)
                ? modelOverride.transform()
                : (baseConfig != null ? baseConfig.transform() : DisplayTransform.DEFAULT);

        ItemStack activeBaseItem = null;
        if (baseConfig != null && baseConfig.enabled()) {
            if (modelOverride != null && (modelOverride.getItemModel().isPresent() || modelOverride.hasCustomModelData())) {
                activeBaseItem = itemFactory.createStationModelItem(baseConfig.getItemKey().orElse(null), modelOverride);
            } else {
                activeBaseItem = baseConfig.getItemKey().map(this::resolveItemStack).orElse(null);
            }
        }

        StationInteractionBox effectiveInteractBox = activeStateOpt.flatMap(StationStateDefinition::getHitbox).orElse(interactBox);

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
                    BlockData blockData = resolveBlockData(baseConfig.getBlockState().orElse(null));
                    renderer.spawnBaseModel(player, display, baseConfig, activeBaseItem, blockData, effectiveBaseTransform);
                    if (hideBaseBlock && barrierData != null && blockLoc != null) {
                        player.sendBlockChange(blockLoc, barrierData);
                    }
                } else if (!inRange && viewing) {
                    display.removeViewer(VisualTier.BASE_MODEL, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.BASE_MODEL));
                    if (hideBaseBlock && blockLoc != null) {
                        player.sendBlockChange(blockLoc, world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
                    }
                } else if (inRange && viewing && (stateChanged || isDirty)) {
                    renderer.updateBaseModelMetadata(player, display, baseConfig, activeBaseItem, effectiveBaseTransform);
                    if (hideBaseBlock && barrierData != null && blockLoc != null) {
                        player.sendBlockChange(blockLoc, barrierData);
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
            if (textTemplate != null && textTemplate.enabled()) {
                boolean inRange = distSq <= ((long) textTemplate.viewDistance() * textTemplate.viewDistance());
                boolean viewing = display.isViewing(VisualTier.TEXT_DISPLAY, playerId);
                if (inRange && textDisplayVisible && textComponent != null) {
                    if (!viewing) {
                        display.addViewer(VisualTier.TEXT_DISPLAY, playerId);
                        renderer.spawnTextDisplay(player, display, textTemplate, textComponent, effectiveTextTransform);
                    } else if (isDirty || textChanged || transformChanged) {
                        renderer.updateTextDisplayMetadata(player, display, textTemplate, textComponent, effectiveTextTransform);
                    }
                } else if (viewing) {
                    display.removeViewer(VisualTier.TEXT_DISPLAY, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.TEXT_DISPLAY));
                }

                if (fallbackActive) {
                    handleActionBarFallback(player, display, rawText, distSq, centerX, centerY, centerZ);
                }
            }

            // 4. Interaction Tier
            if (effectiveInteractBox != null) {
                int interactDist = (baseConfig != null) ? baseConfig.viewDistance() : 32;
                boolean inRange = distSq <= ((long) interactDist * interactDist);
                boolean viewing = display.isViewing(VisualTier.INTERACTION, playerId);
                if (inRange && !viewing) {
                    display.addViewer(VisualTier.INTERACTION, playerId);
                    renderer.spawnInteraction(player, display, effectiveInteractBox);
                    if (hideBaseBlock && barrierData != null && blockLoc != null) {
                        player.sendBlockChange(blockLoc, barrierData);
                    }
                } else if (!inRange && viewing) {
                    display.removeViewer(VisualTier.INTERACTION, playerId);
                    renderer.destroyEntities(player, display.getEntityId(VisualTier.INTERACTION));
                    if (hideBaseBlock && blockLoc != null && (baseConfig == null || !display.isViewing(VisualTier.BASE_MODEL, playerId))) {
                        player.sendBlockChange(blockLoc, world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
                    }
                } else if (inRange && viewing && (stateChanged || isDirty)) {
                    renderer.updateInteractionHitboxMetadata(player, display, effectiveInteractBox.width(), effectiveInteractBox.height());
                }
            } else if (display.isViewing(VisualTier.INTERACTION, playerId)) {
                display.removeViewer(VisualTier.INTERACTION, playerId);
                renderer.destroyEntities(player, display.getEntityId(VisualTier.INTERACTION));
                if (hideBaseBlock && blockLoc != null && (baseConfig == null || !display.isViewing(VisualTier.BASE_MODEL, playerId))) {
                    player.sendBlockChange(blockLoc, world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData());
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

    /**
     * Immediately evaluates station proximity and synchronizes virtual display packets
     * for a player (e.g., on player join or teleport) without waiting for the recurring ticker.
     *
     * @param player         the joining/moving player
     * @param stationManager the station manager tracking in-world stations
     */
    public void tickPlayer(Player player, DefaultStationManager stationManager) {
        if (player == null || !player.isOnline() || stationManager == null) return;
        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        double px = playerLoc.getX();
        double py = playerLoc.getY();
        double pz = playerLoc.getZ();
        double maxDist = 48.0;
        double maxDistSq = maxDist * maxDist;

        for (StationInstance station : stationManager.getAllStations()) {
            BlockPosition pos = station.getPosition();
            if (!pos.worldName().equals(world.getName())) {
                continue;
            }
            double dx = (pos.x() + 0.5) - px;
            double dy = (pos.y() + 0.5) - py;
            double dz = (pos.z() + 0.5) - pz;
            if (dx * dx + dy * dy + dz * dz <= maxDistSq) {
                Optional<ProcessComponent> procOpt = station.getComponent(ProcessComponent.class);
                ProcessInstance proc = procOpt.flatMap(ProcessComponent::getActiveProcess).orElse(null);
                syncStation(station, world, proc);
            }
        }
    }

    /**
     * Masks the base block of all stations in the given chunk as BARRIER for a player receiving that chunk.
     *
     * @param player         the player receiving the chunk
     * @param chunk          the chunk loaded for the player
     * @param stationManager the station manager
     */
    public void maskStationsInChunk(Player player, Chunk chunk, DefaultStationManager stationManager) {
        if (player == null || !player.isOnline() || chunk == null || stationManager == null) return;
        ChunkPosition chunkPos = new ChunkPosition(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Set<StationInstance> stations = stationManager.getStationsInChunk(chunkPos);
        if (stations == null || stations.isEmpty()) return;

        BlockData barrierData = null;
        for (StationInstance station : stations) {
            boolean hideBaseBlock = station.getDefinition().getVisualDefinition()
                    .map(StationVisualDefinition::hideBaseBlock)
                    .orElse(false);
            if (hideBaseBlock) {
                if (barrierData == null) {
                    barrierData = resolveBarrierBlockData();
                    if (barrierData == null) return;
                }
                BlockPosition pos = station.getPosition();
                try {
                    player.sendBlockChange(new Location(chunk.getWorld(), pos.x(), pos.y(), pos.z()), barrierData);
                } catch (Exception ex) {
                    logger.trace(LogChannel.DISPLAY, () -> "Failed to mask station in chunk for " + player.getName(), ex);
                }
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
        } catch (Exception ex) {
            logger.trace(LogChannel.DISPLAY, () -> "Invalid station display block state '" + blockState + "'", ex);
            return null;
        }
    }

    public BlockData resolveBarrierBlockData() {
        try {
            return server.createBlockData(Material.BARRIER);
        } catch (Exception ex) {
            try {
                return Bukkit.createBlockData(Material.BARRIER);
            } catch (Exception fallbackEx) {
                logger.trace(LogChannel.DISPLAY, () -> "Failed to resolve BARRIER block data", fallbackEx);
                return null;
            }
        }
    }

    public double calculateHeadroom(StationInstance station, World world) {
        if (station == null || world == null) {
            return 3.0;
        }
        BlockPosition pos = station.getPosition();
        double ceilingY = 3.0;

        Block blockAbove1 = null;
        try {
            blockAbove1 = world.getBlockAt(pos.x(), pos.y() + 1, pos.z());
        } catch (Exception ignored) {}

        if (blockAbove1 != null) {
            VoxelShape shape1 = blockAbove1.getCollisionShape();
            if (shape1 != null && !shape1.getBoundingBoxes().isEmpty()) {
                double minY = shape1.getBoundingBoxes().stream()
                        .mapToDouble(BoundingBox::getMinY)
                        .min()
                        .orElse(0.0);
                ceilingY = 1.0 + minY;
            } else {
                Block blockAbove2 = null;
                try {
                    blockAbove2 = world.getBlockAt(pos.x(), pos.y() + 2, pos.z());
                } catch (Exception ignored) {}
                if (blockAbove2 != null) {
                    VoxelShape shape2 = blockAbove2.getCollisionShape();
                    if (shape2 != null && !shape2.getBoundingBoxes().isEmpty()) {
                        double minY = shape2.getBoundingBoxes().stream()
                                .mapToDouble(BoundingBox::getMinY)
                                .min()
                                .orElse(0.0);
                        ceilingY = 2.0 + minY;
                    }
                }
            }
        }

        double stationTop = 0.55;
        if (station.getDefinition() != null && station.getDefinition().getVisualDefinition().isPresent()) {
            StationVisualDefinition visual = station.getDefinition().getVisualDefinition().get();
            if (visual.getBaseModel().isPresent()) {
                stationTop = visual.getBaseModel().get().transform().translationY() + 0.05;
            }
        }

        return Math.max(0.0, ceilingY - stationTop);
    }

    public void invalidateHeadroom(BlockPosition pos) {
        if (pos == null) return;
        PacketStationDisplay display = activeDisplays.get(pos);
        if (display != null) {
            display.invalidateHeadroom();
        }
    }

    private void handleActionBarFallback(Player player, PacketStationDisplay display, String rawText, double distSq, double cx, double cy, double cz) {
        if (player == null || rawText == null || rawText.isBlank()) return;
        Map<String, Object> params = display.getFallbackParams();
        double maxDist = 4.0;
        double lookAngle = 15.0;
        String separator = " <dark_gray>•</dark_gray> ";
        if (params != null) {
            Object dObj = params.get("max_distance");
            if (dObj instanceof Number n) maxDist = n.doubleValue();
            Object aObj = params.get("look_angle");
            if (aObj instanceof Number n) lookAngle = n.doubleValue();
            Object sObj = params.get("separator");
            if (sObj != null) separator = sObj.toString();
        }

        if (distSq > maxDist * maxDist) return;

        Location eye = player.getEyeLocation();
        Vector toStation = new Vector(cx - eye.getX(), cy - eye.getY(), cz - eye.getZ()).normalize();
        double dot = eye.getDirection().dot(toStation);
        double minDot = Math.cos(Math.toRadians(lookAngle));
        if (dot < minDot) return;

        long now = System.currentTimeMillis();
        long lastSent = display.getLastActionBarTime();
        String flatText = flattenText(rawText, separator);

        if (!flatText.equals(display.getLastActionBarText()) || (now - lastSent >= 2000)) {
            player.sendActionBar(miniMessage.deserialize(flatText));
            display.setLastActionBarText(flatText);
            display.setLastActionBarTime(now);
        }
    }

    public static String flattenText(String multiLineText, String separator) {
        if (multiLineText == null) return "";
        String[] lines = multiLineText.split("\\r?\\n");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                validLines.add(trimmed);
            }
        }
        return String.join(separator != null ? separator : " • ", validLines);
    }
}
