package dev.darkblade.croppy.platform.paper.display;

import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.station.display.VirtualEntityIdGenerator;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State container managing virtual entity IDs (ItemDisplay and Interaction) and player tracking
 * sets for a discrete 3D block or crop display in the world.
 */
public final class VirtualBlockDisplay {

    private final BlockPosition position;
    private final int itemDisplayId;
    private final UUID itemDisplayUuid;
    private final int interactionId;
    private final UUID interactionUuid;

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    private volatile float yaw;
    private volatile Model3DDefinition currentModel;

    public VirtualBlockDisplay(BlockPosition position, float yaw, Model3DDefinition model) {
        this.position = Objects.requireNonNull(position, "position must not be null");
        this.yaw = yaw;
        this.currentModel = model;

        this.itemDisplayId = VirtualEntityIdGenerator.nextEntityId();
        this.itemDisplayUuid = UUID.randomUUID();

        this.interactionId = VirtualEntityIdGenerator.nextEntityId();
        this.interactionUuid = UUID.randomUUID();
    }

    public VirtualBlockDisplay(BlockPosition position, float yaw) {
        this(position, yaw, null);
    }

    public BlockPosition getPosition() {
        return position;
    }

    public int getItemDisplayId() {
        return itemDisplayId;
    }

    public UUID getItemDisplayUuid() {
        return itemDisplayUuid;
    }

    public int getInteractionId() {
        return interactionId;
    }

    public UUID getInteractionUuid() {
        return interactionUuid;
    }

    public int[] getAllEntityIds() {
        return new int[]{itemDisplayId, interactionId};
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public Model3DDefinition getModel() {
        return currentModel;
    }

    public void setModel(Model3DDefinition currentModel) {
        this.currentModel = currentModel;
    }

    public boolean addViewer(UUID playerId) {
        return viewers.add(playerId);
    }

    public boolean removeViewer(UUID playerId) {
        return viewers.remove(playerId);
    }

    public boolean isViewing(UUID playerId) {
        return viewers.contains(playerId);
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public void clearViewers() {
        viewers.clear();
    }
}
