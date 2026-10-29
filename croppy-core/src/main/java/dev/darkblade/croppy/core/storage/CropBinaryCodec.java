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
 * Compact binary serializer/deserializer for spatial crop instances.
 */
public final class CropBinaryCodec {

    private static final int MAGIC = 0x43525059; // 'CRPY'
    private static final int VERSION = 1;

    private CropBinaryCodec() {}

    /**
     * Serializes a collection of crop instances into a compact byte array.
     */
    public static byte[] serialize(Collection<CropInstance> crops) {
        if (crops == null || crops.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
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
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize crop collection", e);
        }

        return baos.toByteArray();
    }

    /**
     * Deserializes a byte array into a list of crop instances.
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
            if (version != VERSION) {
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

                BlockPosition pos = BlockPosition.of(world, x, y, z);
                list.add(new DefaultCropInstance(speciesKey, pos, stageIndex, progress, plantedAt, lastUpdated));
            }
            return list;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize binary crop data", e);
        }
    }
}
