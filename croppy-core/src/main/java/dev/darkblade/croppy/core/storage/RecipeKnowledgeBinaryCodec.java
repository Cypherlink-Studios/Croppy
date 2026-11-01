package dev.darkblade.croppy.core.storage;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.PlayerRecipeKnowledge;

import java.io.*;
import java.time.Instant;
import java.util.*;

/**
 * Compact binary serializer/deserializer for player recipe knowledge records.
 */
public final class RecipeKnowledgeBinaryCodec {

    private static final int MAGIC = 0x5243504B; // 'RCPK'
    public static final int CURRENT_VERSION = 1;

    private RecipeKnowledgeBinaryCodec() {}

    /**
     * Serializes a collection of player recipe knowledge records to bytes.
     *
     * @param knowledgeRecords collection of records to serialize
     * @return binary byte array
     */
    public static byte[] serialize(Collection<PlayerRecipeKnowledge> knowledgeRecords) {
        if (knowledgeRecords == null || knowledgeRecords.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(MAGIC);
            out.writeInt(CURRENT_VERSION);
            out.writeInt(knowledgeRecords.size());

            for (PlayerRecipeKnowledge record : knowledgeRecords) {
                out.writeUTF(record.recipeKey().asString());
                out.writeByte(record.state().ordinal());
                out.writeLong(record.discoveredAt() != null ? record.discoveredAt().toEpochMilli() : -1L);
                out.writeUTF(record.discoveryMethodKey() != null ? record.discoveryMethodKey().asString() : "");

                Map<String, String> meta = record.metadata();
                out.writeInt(meta.size());
                for (Map.Entry<String, String> entry : meta.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize recipe knowledge records", e);
        }

        return baos.toByteArray();
    }

    /**
     * Deserializes a binary payload into a map of recipe keys to recipe knowledge records.
     *
     * @param bytes    binary byte array
     * @param playerId player UUID to assign to the deserialized records
     * @return map of recipe keys to knowledge records
     */
    public static Map<Key, PlayerRecipeKnowledge> deserialize(byte[] bytes, UUID playerId) {
        if (bytes == null || bytes.length == 0) {
            return Map.of();
        }

        Map<Key, PlayerRecipeKnowledge> map = new HashMap<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                return Map.of();
            }

            int version = in.readInt();
            if (version != CURRENT_VERSION) {
                return Map.of();
            }

            int count = in.readInt();
            KnowledgeState[] states = KnowledgeState.values();

            for (int i = 0; i < count; i++) {
                String recipeKeyStr = in.readUTF();
                Key recipeKey = Key.parse(recipeKeyStr);

                int stateOrdinal = in.readByte();
                KnowledgeState state = (stateOrdinal >= 0 && stateOrdinal < states.length)
                        ? states[stateOrdinal]
                        : KnowledgeState.UNKNOWN;

                long epochMilli = in.readLong();
                Instant discoveredAt = (epochMilli >= 0) ? Instant.ofEpochMilli(epochMilli) : null;

                String methodStr = in.readUTF();
                Key methodKey = (!methodStr.isBlank()) ? Key.parse(methodStr) : null;

                int metaSize = in.readInt();
                Map<String, String> metadata = new HashMap<>();
                for (int m = 0; m < metaSize; m++) {
                    String k = in.readUTF();
                    String v = in.readUTF();
                    metadata.put(k, v);
                }

                PlayerRecipeKnowledge record = new PlayerRecipeKnowledge(
                        playerId,
                        recipeKey,
                        state,
                        discoveredAt,
                        methodKey,
                        metadata
                );
                map.put(recipeKey, record);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize recipe knowledge records", e);
        }

        return Collections.unmodifiableMap(map);
    }
}
