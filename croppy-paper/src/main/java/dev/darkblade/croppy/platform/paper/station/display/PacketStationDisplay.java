package dev.darkblade.croppy.platform.paper.station.display;

import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.world.BlockPosition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State container managing virtual entity IDs and player tracking sets for a station's visual tiers.
 */
public final class PacketStationDisplay {

    private final BlockPosition position;
    private final StationInstance station;

    private final int baseModelId;
    private final UUID baseModelUuid;

    private final int processItemId;
    private final UUID processItemUuid;

    private final int textDisplayId;
    private final UUID textDisplayUuid;

    private final int interactionId;
    private final UUID interactionUuid;

    private final Map<VisualTier, Set<UUID>> tierViewers = new ConcurrentHashMap<>();

    public PacketStationDisplay(StationInstance station) {
        this.station = Objects.requireNonNull(station, "station cannot be null");
        this.position = station.getPosition();

        this.baseModelId = VirtualEntityIdGenerator.nextEntityId();
        this.baseModelUuid = UUID.randomUUID();

        this.processItemId = VirtualEntityIdGenerator.nextEntityId();
        this.processItemUuid = UUID.randomUUID();

        this.textDisplayId = VirtualEntityIdGenerator.nextEntityId();
        this.textDisplayUuid = UUID.randomUUID();

        this.interactionId = VirtualEntityIdGenerator.nextEntityId();
        this.interactionUuid = UUID.randomUUID();

        for (VisualTier tier : VisualTier.values()) {
            tierViewers.put(tier, ConcurrentHashMap.newKeySet());
        }
    }

    public BlockPosition getPosition() {
        return position;
    }

    public StationInstance getStation() {
        return station;
    }

    public int getEntityId(VisualTier tier) {
        return switch (tier) {
            case BASE_MODEL -> baseModelId;
            case PROCESS_ITEM -> processItemId;
            case TEXT_DISPLAY -> textDisplayId;
            case INTERACTION -> interactionId;
        };
    }

    public UUID getEntityUuid(VisualTier tier) {
        return switch (tier) {
            case BASE_MODEL -> baseModelUuid;
            case PROCESS_ITEM -> processItemUuid;
            case TEXT_DISPLAY -> textDisplayUuid;
            case INTERACTION -> interactionUuid;
        };
    }

    public int getInteractionId() {
        return interactionId;
    }

    public UUID getInteractionUuid() {
        return interactionUuid;
    }

    public int[] getAllEntityIds() {
        return new int[]{baseModelId, processItemId, textDisplayId, interactionId};
    }

    public boolean addViewer(VisualTier tier, UUID playerId) {
        return tierViewers.get(tier).add(playerId);
    }

    public boolean removeViewer(VisualTier tier, UUID playerId) {
        return tierViewers.get(tier).remove(playerId);
    }

    public boolean isViewing(VisualTier tier, UUID playerId) {
        return tierViewers.get(tier).contains(playerId);
    }

    public Set<UUID> getViewers(VisualTier tier) {
        return Collections.unmodifiableSet(tierViewers.get(tier));
    }

    public Set<UUID> getAllViewers() {
        Set<UUID> all = new HashSet<>();
        for (Set<UUID> viewers : tierViewers.values()) {
            all.addAll(viewers);
        }
        return all;
    }

    public void removePlayerFromAll(UUID playerId) {
        for (Set<UUID> viewers : tierViewers.values()) {
            viewers.remove(playerId);
        }
    }

    public void clearAllViewers() {
        for (Set<UUID> viewers : tierViewers.values()) {
            viewers.clear();
        }
    }
}
