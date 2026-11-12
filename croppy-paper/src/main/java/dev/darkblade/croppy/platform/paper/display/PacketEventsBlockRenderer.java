package dev.darkblade.croppy.platform.paper.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.species.RotationCalculator;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Constructs and dispatches PacketEvents packets for virtual 3D block and crop displays
 * pairing ItemDisplay model geometries with Interaction entity hitboxes.
 */
public class PacketEventsBlockRenderer {

    // Item Display metadata indices
    private static final int INDEX_INTERPOLATION_START = 8;
    private static final int INDEX_INTERPOLATION_DURATION = 9;
    private static final int INDEX_TRANSLATION = 11;
    private static final int INDEX_SCALE = 12;
    private static final int INDEX_ROTATION_LEFT = 13;
    private static final int INDEX_ROTATION_RIGHT = 14;
    private static final int INDEX_BILLBOARD = 15;
    private static final int INDEX_ITEM = 23;
    private static final int INDEX_ITEM_TRANSFORM = 24;

    // Interaction Entity metadata indices
    private static final int INDEX_INTERACTION_WIDTH = 8;
    private static final int INDEX_INTERACTION_HEIGHT = 9;
    private static final int INDEX_INTERACTION_RESPONSE = 10;

    private static final byte ITEM_TRANSFORM_FIXED = 8;

    private final CroppyLogger logger;

    public PacketEventsBlockRenderer(Logger logger) {
        this(new JulCroppyLogger(Objects.requireNonNull(logger, "logger cannot be null")));
    }

    public PacketEventsBlockRenderer(CroppyLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    /**
     * Spawns both the ItemDisplay and Interaction entities for a player.
     *
     * @param player  target player
     * @param display virtual block display container
     * @param item    item stack with custom item_model to render
     */
    public void spawnBlockDisplay(Player player, VirtualBlockDisplay display, ItemStack item) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(display, "display cannot be null");

        BlockPosition pos = display.getPosition();
        Vector3d itemPos = new Vector3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Vector3d interactionPos = new Vector3d(pos.x() + 0.5, pos.y(), pos.z() + 0.5);

        // 1. Spawn ItemDisplay
        WrapperPlayServerSpawnEntity spawnItem = new WrapperPlayServerSpawnEntity(
                display.getItemDisplayId(),
                Optional.of(display.getItemDisplayUuid()),
                EntityTypes.ITEM_DISPLAY,
                itemPos,
                0.0f,
                0.0f,
                0.0f,
                0,
                Optional.empty()
        );
        sendPacket(player, spawnItem);

        // 2. Spawn Interaction
        WrapperPlayServerSpawnEntity spawnInteraction = new WrapperPlayServerSpawnEntity(
                display.getInteractionId(),
                Optional.of(display.getInteractionUuid()),
                EntityTypes.INTERACTION,
                interactionPos,
                0.0f,
                0.0f,
                0.0f,
                0,
                Optional.empty()
        );
        sendPacket(player, spawnInteraction);

        // 3. Send initial metadata
        updateBlockDisplay(player, display, item, false);
    }

    /**
     * Dispatches metadata updates for both ItemDisplay and Interaction entities.
     *
     * @param player      target player
     * @param display     virtual block display container
     * @param item        item stack to render
     * @param interpolate whether to apply client-side interpolation
     */
    public void updateBlockDisplay(Player player, VirtualBlockDisplay display, ItemStack item, boolean interpolate) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(display, "display cannot be null");

        Model3DDefinition model = display.getModel();
        float scaleX = model != null ? model.scaleX() : 1.0f;
        float scaleY = model != null ? model.scaleY() : 1.0f;
        float scaleZ = model != null ? model.scaleZ() : 1.0f;

        float transX = model != null ? model.translationX() : 0.0f;
        float transY = model != null ? model.translationY() : 0.0f;
        float transZ = model != null ? model.translationZ() : 0.0f;

        float hitboxWidth = model != null ? model.hitboxWidth() : 0.8f;
        float hitboxHeight = model != null ? model.hitboxHeight() : 0.8f;

        // Calculate rotation quaternion from yaw
        float[] q = RotationCalculator.toYQuaternion(display.getYaw());
        Quaternion4f leftRot = new Quaternion4f(q[0], q[1], q[2], q[3]);
        Quaternion4f identity = new Quaternion4f(0.0f, 0.0f, 0.0f, 1.0f);

        // ItemDisplay metadata
        List<EntityData<?>> itemMeta = new ArrayList<>();
        itemMeta.add(new EntityData(INDEX_INTERPOLATION_START, EntityDataTypes.INT, 0));
        itemMeta.add(new EntityData(INDEX_INTERPOLATION_DURATION, EntityDataTypes.INT, interpolate ? 5 : 0));
        itemMeta.add(new EntityData(INDEX_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(transX, transY, transZ)));
        itemMeta.add(new EntityData(INDEX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(scaleX, scaleY, scaleZ)));
        itemMeta.add(new EntityData(INDEX_ROTATION_LEFT, EntityDataTypes.QUATERNION, leftRot));
        itemMeta.add(new EntityData(INDEX_ROTATION_RIGHT, EntityDataTypes.QUATERNION, identity));
        itemMeta.add(new EntityData(INDEX_BILLBOARD, EntityDataTypes.BYTE, (byte) 0)); // FIXED

        if (item != null && !item.getType().isAir()) {
            itemMeta.add(new EntityData(INDEX_ITEM, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)));
            itemMeta.add(new EntityData(INDEX_ITEM_TRANSFORM, EntityDataTypes.BYTE, ITEM_TRANSFORM_FIXED));
        }

        WrapperPlayServerEntityMetadata itemPacket = new WrapperPlayServerEntityMetadata(display.getItemDisplayId(), itemMeta);
        sendPacket(player, itemPacket);

        // Interaction Entity metadata
        List<EntityData<?>> interactionMeta = new ArrayList<>();
        interactionMeta.add(new EntityData(INDEX_INTERACTION_WIDTH, EntityDataTypes.FLOAT, hitboxWidth));
        interactionMeta.add(new EntityData(INDEX_INTERACTION_HEIGHT, EntityDataTypes.FLOAT, hitboxHeight));
        interactionMeta.add(new EntityData(INDEX_INTERACTION_RESPONSE, EntityDataTypes.BOOLEAN, true));

        WrapperPlayServerEntityMetadata interactionPacket = new WrapperPlayServerEntityMetadata(display.getInteractionId(), interactionMeta);
        sendPacket(player, interactionPacket);
    }

    /**
     * Destroys both virtual entities for a player.
     *
     * @param player  target player
     * @param display virtual block display container
     */
    public void destroyBlockDisplay(Player player, VirtualBlockDisplay display) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(display, "display cannot be null");

        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(display.getAllEntityIds());
        sendPacket(player, destroyPacket);
    }

    public void sendPacket(Player player, PacketWrapper<?> packet) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Exception e) {
            logger.warn(LogChannel.DISPLAY, "Failed to dispatch packet to player " + player.getName(), e);
        }
    }
}
