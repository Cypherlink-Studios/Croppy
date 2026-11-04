package dev.darkblade.croppy.platform.paper.station.display;

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
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayLayerConfig;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import dev.darkblade.croppy.api.station.visual.TextDisplayTemplate;
import dev.darkblade.croppy.api.world.BlockPosition;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Constructs and dispatches PacketEvents packets for virtual station displays.
 */
public class PacketEventsStationRenderer {

    // Common Display Entity Metadata indices (Minecraft 1.20 - 1.21)
    static final int INDEX_INTERPOLATION_START = 8;
    static final int INDEX_INTERPOLATION_DURATION = 9;
    static final int INDEX_TRANSLATION = 11;
    static final int INDEX_SCALE = 12;
    static final int INDEX_ROTATION_LEFT = 13;
    static final int INDEX_ROTATION_RIGHT = 14;
    static final int INDEX_BILLBOARD = 15;

    // Item Display specific indices
    static final int INDEX_ITEM = 23;
    static final int INDEX_ITEM_TRANSFORM = 24;

    // Block Display specific indices
    static final int INDEX_BLOCK_STATE = 23;

    // Text Display specific indices
    static final int INDEX_TEXT = 23;
    static final int INDEX_LINE_WIDTH = 24;
    static final int INDEX_BACKGROUND_COLOR = 25;
    static final int INDEX_TEXT_OPACITY = 26;
    static final int INDEX_TEXT_FLAGS = 27;

    private final Logger logger;

    public PacketEventsStationRenderer(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    /**
     * Spawns the base model virtual display entity (Item or Block Display) for a player.
     */
    public void spawnBaseModel(Player player, PacketStationDisplay display, DisplayLayerConfig config, ItemStack item, BlockData blockData) {
        if (!config.enabled()) return;

        BlockPosition pos = display.getPosition();
        int entityId = display.getEntityId(VisualTier.BASE_MODEL);
        UUID uuid = display.getEntityUuid(VisualTier.BASE_MODEL);

        Vector3d spawnPos = new Vector3d(pos.x() + 0.5, pos.y(), pos.z() + 0.5);
        boolean isBlock = "BLOCK".equalsIgnoreCase(config.type());

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                isBlock ? EntityTypes.BLOCK_DISPLAY : EntityTypes.ITEM_DISPLAY,
                spawnPos,
                0.0f,
                0.0f,
                0.0f,
                0,
                Optional.empty()
        );
        sendPacket(player, spawnPacket);

        List<EntityData<?>> metadata = new ArrayList<>();
        applyTransformMetadata(metadata, config.transform(), config.billboard(), config.interpolationDuration(), display.getStation().getYaw());

        if (isBlock && blockData != null) {
            int blockStateId = SpigotConversionUtil.fromBukkitBlockData(blockData).getGlobalId();
            metadata.add(new EntityData(INDEX_BLOCK_STATE, EntityDataTypes.BLOCK_STATE, blockStateId));
        } else if (item != null) {
            metadata.add(new EntityData(INDEX_ITEM, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)));
            metadata.add(new EntityData(INDEX_ITEM_TRANSFORM, EntityDataTypes.BYTE, (byte) 8)); // FIXED
        }

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        sendPacket(player, metaPacket);
    }

    /**
     * Spawns the in-process item virtual display entity for a player.
     */
    public void spawnProcessItem(Player player, PacketStationDisplay display, DisplayLayerConfig config, ItemStack item, double temp, String thermalZone) {
        if (!config.enabled() || item == null || item.getType().isAir()) return;

        BlockPosition pos = display.getPosition();
        int entityId = display.getEntityId(VisualTier.PROCESS_ITEM);
        UUID uuid = display.getEntityUuid(VisualTier.PROCESS_ITEM);

        Vector3d spawnPos = new Vector3d(pos.x() + 0.5, pos.y(), pos.z() + 0.5);
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.ITEM_DISPLAY,
                spawnPos,
                0.0f,
                0.0f,
                0.0f,
                0,
                Optional.empty()
        );
        sendPacket(player, spawnPacket);

        updateProcessItemMetadata(player, display, config, item, temp, thermalZone);
    }

    /**
     * Updates the in-process item display metadata with rotation and thermal jitter interpolation.
     */
    public void updateProcessItemMetadata(Player player, PacketStationDisplay display, DisplayLayerConfig config, ItemStack item, double temp, String thermalZone) {
        int entityId = display.getEntityId(VisualTier.PROCESS_ITEM);
        List<EntityData<?>> metadata = new ArrayList<>();

        float stationYaw = display.getStation().getYaw();
        float yawRad = (float) Math.toRadians(-stationYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);

        DisplayTransform baseTransform = config.transform();
        double tx = baseTransform.translationX();
        double ty = baseTransform.translationY();
        double tz = baseTransform.translationZ();

        float rotatedTx = (float) (tx * cos - tz * sin);
        float rotatedTz = (float) (tx * sin + tz * cos);

        boolean isHot = temp >= 100.0 || (thermalZone != null && (thermalZone.contains("BOIL") || thermalZone.contains("SEAR") || thermalZone.contains("BURNING")));
        int interpolation = isHot ? 2 : config.interpolationDuration();

        double jitterX = 0.0;
        double jitterY = 0.0;
        double jitterZ = 0.0;
        if (isHot) {
            jitterX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.03;
            jitterY = ThreadLocalRandom.current().nextDouble() * 0.02;
            jitterZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.03;
        }

        // Continuous smooth rotation around Y axis
        long now = System.currentTimeMillis();
        float speed = isHot ? 0.005f : 0.002f;
        float angle = (float) ((now * speed) % (2 * Math.PI));

        Quaternionf q = new Quaternionf().rotateY(yawRad).rotateXYZ(baseTransform.pitch(), baseTransform.yaw() + angle, baseTransform.roll());
        Quaternion4f rot = new Quaternion4f(q.x, q.y, q.z, q.w);
        Quaternion4f identity = new Quaternion4f(0.0f, 0.0f, 0.0f, 1.0f);

        metadata.add(new EntityData(INDEX_INTERPOLATION_START, EntityDataTypes.INT, 0));
        metadata.add(new EntityData(INDEX_INTERPOLATION_DURATION, EntityDataTypes.INT, interpolation));
        metadata.add(new EntityData(INDEX_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f((float) (rotatedTx + jitterX), (float) (ty + jitterY), (float) (rotatedTz + jitterZ))));
        metadata.add(new EntityData(INDEX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(baseTransform.scaleX(), baseTransform.scaleY(), baseTransform.scaleZ())));
        metadata.add(new EntityData(INDEX_ROTATION_LEFT, EntityDataTypes.QUATERNION, rot));
        metadata.add(new EntityData(INDEX_ROTATION_RIGHT, EntityDataTypes.QUATERNION, identity));
        metadata.add(new EntityData(INDEX_BILLBOARD, EntityDataTypes.BYTE, toBillboardByte(config.billboard())));

        if (item != null && !item.getType().isAir()) {
            metadata.add(new EntityData(INDEX_ITEM, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)));
            metadata.add(new EntityData(INDEX_ITEM_TRANSFORM, EntityDataTypes.BYTE, (byte) 8)); // FIXED
        }

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        sendPacket(player, metaPacket);
    }

    /**
     * Spawns the floating holographic Text Display entity for a player.
     */
    public void spawnTextDisplay(Player player, PacketStationDisplay display, TextDisplayTemplate template, Component text) {
        if (!template.enabled()) return;

        BlockPosition pos = display.getPosition();
        int entityId = display.getEntityId(VisualTier.TEXT_DISPLAY);
        UUID uuid = display.getEntityUuid(VisualTier.TEXT_DISPLAY);

        Vector3d spawnPos = new Vector3d(pos.x() + 0.5, pos.y(), pos.z() + 0.5);
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.TEXT_DISPLAY,
                spawnPos,
                0.0f,
                0.0f,
                0.0f,
                0,
                Optional.empty()
        );
        sendPacket(player, spawnPacket);

        updateTextDisplayMetadata(player, display, template, text);
    }

    /**
     * Updates the status text metadata on a client's Text Display entity.
     */
    public void updateTextDisplayMetadata(Player player, PacketStationDisplay display, TextDisplayTemplate template, Component text) {
        int entityId = display.getEntityId(VisualTier.TEXT_DISPLAY);
        List<EntityData<?>> metadata = new ArrayList<>();

        applyTransformMetadata(metadata, template.transform(), template.billboard(), 0, display.getStation().getYaw());

        Component cleanText = (text == null) ? Component.empty() : text;
        metadata.add(new EntityData(INDEX_TEXT, EntityDataTypes.ADV_COMPONENT, cleanText));
        metadata.add(new EntityData(INDEX_LINE_WIDTH, EntityDataTypes.INT, template.lineWidth()));
        metadata.add(new EntityData(INDEX_BACKGROUND_COLOR, EntityDataTypes.INT, template.backgroundColor()));

        byte flags = 0;
        if (template.textShadow()) flags |= 0x01;
        if (template.seeThrough()) flags |= 0x02;
        metadata.add(new EntityData(INDEX_TEXT_FLAGS, EntityDataTypes.BYTE, flags));

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        sendPacket(player, metaPacket);
    }

    /**
     * Sends packet destroying the specified virtual entity IDs for a player.
     */
    public void destroyEntities(Player player, int... entityIds) {
        if (entityIds == null || entityIds.length == 0) return;
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityIds);
        sendPacket(player, packet);
    }

    public void sendPacket(Player player, PacketWrapper<?> packet) {
        if (player == null || !player.isOnline()) return;
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Failed to send PacketEvents packet to player " + player.getName(), ex);
        }
    }

    void applyTransformMetadata(List<EntityData<?>> metadata, DisplayTransform transform, BillboardMode billboard, int interpolation, float stationYaw) {
        metadata.add(new EntityData(INDEX_INTERPOLATION_START, EntityDataTypes.INT, 0));
        metadata.add(new EntityData(INDEX_INTERPOLATION_DURATION, EntityDataTypes.INT, interpolation));

        boolean isFixed = (billboard == null || billboard == BillboardMode.FIXED);
        float effectiveYaw = isFixed ? stationYaw : 0.0f;
        float yawRad = (float) Math.toRadians(-effectiveYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);
        double tx = transform.translationX();
        double ty = transform.translationY();
        double tz = transform.translationZ();
        float rotatedTx = isFixed ? (float) (tx * cos - tz * sin) : (float) tx;
        float rotatedTz = isFixed ? (float) (tx * sin + tz * cos) : (float) tz;

        metadata.add(new EntityData(INDEX_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(rotatedTx, (float) ty, rotatedTz)));
        metadata.add(new EntityData(INDEX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(transform.scaleX(), transform.scaleY(), transform.scaleZ())));

        Quaternionf q = new Quaternionf();
        if (isFixed) {
            q.rotateY(yawRad);
        }
        q.rotateXYZ(transform.pitch(), transform.yaw(), transform.roll());

        Quaternion4f rot = new Quaternion4f(q.x, q.y, q.z, q.w);
        metadata.add(new EntityData(INDEX_ROTATION_LEFT, EntityDataTypes.QUATERNION, rot));
        metadata.add(new EntityData(INDEX_ROTATION_RIGHT, EntityDataTypes.QUATERNION, new Quaternion4f(0.0f, 0.0f, 0.0f, 1.0f)));
        metadata.add(new EntityData(INDEX_BILLBOARD, EntityDataTypes.BYTE, toBillboardByte(billboard)));
    }

    private byte toBillboardByte(BillboardMode mode) {
        if (mode == null) return 0;
        return switch (mode) {
            case FIXED -> (byte) 0;
            case VERTICAL -> (byte) 1;
            case HORIZONTAL -> (byte) 2;
            case CENTER -> (byte) 3;
        };
    }
}
