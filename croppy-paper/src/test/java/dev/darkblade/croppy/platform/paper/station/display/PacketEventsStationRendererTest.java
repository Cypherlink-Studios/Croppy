package dev.darkblade.croppy.platform.paper.station.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import dev.darkblade.croppy.api.station.visual.BillboardMode;
import dev.darkblade.croppy.api.station.visual.DisplayTransform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PacketEventsStationRendererTest {

    private PacketEventsStationRenderer renderer;

    @BeforeAll
    static void initPacketEvents() {
        if (PacketEvents.getAPI() == null) {
            PacketEvents.setAPI(new PacketEventsAPI<Object>() {
                @Override public boolean isLoaded() { return true; }
                @Override public void init() {}
                @Override public boolean isInitialized() { return true; }
                @Override public boolean isTerminated() { return false; }
                @Override public Object getPlugin() { return null; }
                @Override public ServerManager getServerManager() { return null; }
                @Override public ProtocolManager getProtocolManager() { return null; }
                @Override public PlayerManager getPlayerManager() { return null; }
                @Override public NettyManager getNettyManager() { return null; }
                @Override public ChannelInjector getInjector() { return null; }
            });
        }
    }

    @BeforeEach
    void setUp() {
        renderer = new PacketEventsStationRenderer(Logger.getLogger("test"));
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0f, 45.0f, 90.0f, 135.0f, 180.0f, 225.0f, 270.0f, 315.0f})
    void testBillboardCenterIgnoresStationYaw(float stationYaw) {
        DisplayTransform transform = DisplayTransform.of(0.0, 1.35, 0.0, 1.0f, 1.0f, 1.0f);
        List<EntityData<?>> metadata = new ArrayList<>();

        renderer.applyTransformMetadata(metadata, transform, BillboardMode.CENTER, 0, stationYaw);

        Quaternion4f rot = findData(metadata, PacketEventsStationRenderer.INDEX_ROTATION_LEFT);
        assertNotNull(rot, "INDEX_ROTATION_LEFT must be present");
        assertEquals(0.0f, rot.getX(), 1e-5f);
        assertEquals(0.0f, rot.getY(), 1e-5f);
        assertEquals(0.0f, rot.getZ(), 1e-5f);
        assertEquals(1.0f, rot.getW(), 1e-5f);

        Vector3f translation = findData(metadata, PacketEventsStationRenderer.INDEX_TRANSLATION);
        assertNotNull(translation);
        assertEquals(0.0f, translation.getX(), 1e-5f);
        assertEquals(1.35f, translation.getY(), 1e-5f);
        assertEquals(0.0f, translation.getZ(), 1e-5f);

        Byte billboard = findData(metadata, PacketEventsStationRenderer.INDEX_BILLBOARD);
        assertEquals((byte) 3, billboard); // CENTER
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0f, 45.0f, 90.0f, 180.0f, 270.0f})
    void testBillboardVerticalIgnoresStationYaw(float stationYaw) {
        DisplayTransform transform = DisplayTransform.of(0.2, 1.5, -0.3, 1.0f, 1.0f, 1.0f);
        List<EntityData<?>> metadata = new ArrayList<>();

        renderer.applyTransformMetadata(metadata, transform, BillboardMode.VERTICAL, 0, stationYaw);

        Quaternion4f rot = findData(metadata, PacketEventsStationRenderer.INDEX_ROTATION_LEFT);
        assertNotNull(rot);
        assertEquals(0.0f, rot.getX(), 1e-5f);
        assertEquals(0.0f, rot.getY(), 1e-5f);
        assertEquals(0.0f, rot.getZ(), 1e-5f);
        assertEquals(1.0f, rot.getW(), 1e-5f);

        // Offsets should NOT be rotated by stationYaw in camera space
        Vector3f translation = findData(metadata, PacketEventsStationRenderer.INDEX_TRANSLATION);
        assertNotNull(translation);
        assertEquals(0.2f, translation.getX(), 1e-5f);
        assertEquals(1.5f, translation.getY(), 1e-5f);
        assertEquals(-0.3f, translation.getZ(), 1e-5f);

        Byte billboard = findData(metadata, PacketEventsStationRenderer.INDEX_BILLBOARD);
        assertEquals((byte) 1, billboard); // VERTICAL
    }

    @Test
    void testBillboardFixedAppliesStationYaw() {
        DisplayTransform transform = DisplayTransform.of(1.0, 0.0, 0.0, 1.0f, 1.0f, 1.0f);
        List<EntityData<?>> metadata = new ArrayList<>();

        // 90 degrees yaw: in Minecraft yaw, yawRad = -90° = -PI/2
        // cos(-PI/2) = 0, sin(-PI/2) = -1
        // rotatedTx = tx * cos - tz * sin = 1 * 0 - 0 * (-1) = 0
        // rotatedTz = tx * sin + tz * cos = 1 * (-1) + 0 * 0 = -1
        renderer.applyTransformMetadata(metadata, transform, BillboardMode.FIXED, 0, 90.0f);

        Quaternion4f rot = findData(metadata, PacketEventsStationRenderer.INDEX_ROTATION_LEFT);
        assertNotNull(rot);
        assertNotEquals(0.0f, rot.getY(), 1e-3f);
        assertEquals(0.0f, rot.getX(), 1e-5f);
        assertEquals(0.0f, rot.getZ(), 1e-5f);

        Vector3f translation = findData(metadata, PacketEventsStationRenderer.INDEX_TRANSLATION);
        assertNotNull(translation);
        assertEquals(0.0f, translation.getX(), 1e-4f);
        assertEquals(-1.0f, translation.getZ(), 1e-4f);

        Byte billboard = findData(metadata, PacketEventsStationRenderer.INDEX_BILLBOARD);
        assertEquals((byte) 0, billboard); // FIXED
    }

    @SuppressWarnings("unchecked")
    private <T> T findData(List<EntityData<?>> metadata, int index) {
        return (T) metadata.stream()
                .filter(d -> d.getIndex() == index)
                .findFirst()
                .map(EntityData::getValue)
                .orElse(null);
    }
}
