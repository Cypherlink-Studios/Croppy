package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Compact binary serializer/deserializer for spatial crop instances supporting schema versioning.
 */
public final class CropBinaryCodec {

    private static final int MAGIC = 0x43525059; // 'CRPY'
    public static final int VERSION_1 = 1;
    public static final int VERSION_2 = 2;
    public static final int VERSION_3 = 3;
    public static final int CURRENT_VERSION = VERSION_3;

    private CropBinaryCodec() {}

    /**
     * Serializes a collection of crop instances into a compact byte array using current schema version (V3).
     */
    public static byte[] serialize(Collection<CropInstance> crops) {
        if (crops == null || crops.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(MAGIC);
            out.writeInt(CURRENT_VERSION);
            out.writeInt(crops.size());
            for (CropInstance crop : crops) {
                out.writeUTF(crop.getSpeciesKey().asString());
                out.writeUTF(crop.getPosition().worldName());
                out.writeInt(crop.getPosition().x());
                out.writeInt(crop.getPosition().y());
                out.writeInt(crop.getPosition().z());
                out.writeInt(crop.getStageIndex());
                out.writeDouble(crop.getProgress());
                out.writeLong(crop.getPlantedAtMillis());
                out.writeLong(crop.getLastUpdatedMillis());
                out.writeLong(crop.getLastWateredDay());
                if (crop.getFertilizerKey() != null) {
                    out.writeBoolean(true);
                    out.writeUTF(crop.getFertilizerKey().asString());
                } else {
                    out.writeBoolean(false);
                }
                out.writeFloat(crop.getYaw());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize crop collection", e);
        }

        return baos.toByteArray();
    }

    /**
     * Deserializes a byte array into a list of crop instances, supporting V1, V2, and V3 formats.
     */
    public static List<CropInstance> deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            return Collections.emptyList();
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IllegalArgumentException("Invalid binary crop data header: " + Integer.toHexString(magic));
            }
            int version = in.readInt();
            if (version != VERSION_1 && version != VERSION_2 && version != VERSION_3) {
                throw new IllegalArgumentException("Unsupported binary crop data version: " + version);
            }
            int count = in.readInt();
            List<CropInstance> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Key speciesKey = Key.parse(in.readUTF());
                String world = in.readUTF();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                int stageIndex = in.readInt();
                double progress = in.readDouble();
                long plantedAt = in.readLong();
                long lastUpdated = in.readLong();

                long lastWateredDay = -1L;
                Key fertilizerKey = null;
                float yaw = 0.0f;

                if (version >= VERSION_2) {
                    lastWateredDay = in.readLong();
                    boolean hasFertilizer = in.readBoolean();
                    if (hasFertilizer) {
                        fertilizerKey = Key.parse(in.readUTF());
                    }
                }

                if (version >= VERSION_3) {
                    yaw = in.readFloat();
                }

                BlockPosition pos = BlockPosition.of(world, x, y, z);
                list.add(new DefaultCropInstance(speciesKey, pos, stageIndex, progress, plantedAt, lastUpdated, lastWateredDay, fertilizerKey, yaw));
            }
            return list;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize binary crop data", e);
        }
    }
}
