package dev.darkblade.croppy.platform.paper.effect;

import dev.darkblade.croppy.api.effect.ActiveStatusEffect;
import dev.darkblade.croppy.api.effect.PlayerEffectStorage;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists {@code persistent: true} player status effects in the player's
 * PersistentDataContainer using a compact binary payload.
 */
public class PdcPlayerEffectStorage implements PlayerEffectStorage {

    private static final int CODEC_VERSION = 1;
    private static final NamespacedKey STORAGE_KEY = new NamespacedKey("croppy", "status_effects");

    private final CroppyLogger logger;

    public PdcPlayerEffectStorage(CroppyLogger logger) {
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    @Override
    public List<ActiveStatusEffect> load(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return List.of();
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        byte[] bytes = pdc.get(STORAGE_KEY, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }
        return decode(bytes);
    }

    @Override
    public void save(UUID playerId, List<ActiveStatusEffect> effects) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (effects == null || effects.isEmpty()) {
            pdc.remove(STORAGE_KEY);
            return;
        }
        byte[] encoded = encode(effects);
        if (encoded.length > 0) {
            pdc.set(STORAGE_KEY, PersistentDataType.BYTE_ARRAY, encoded);
        }
    }

    @Override
    public void clear(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.getPersistentDataContainer().remove(STORAGE_KEY);
        }
    }

    public byte[] encode(List<ActiveStatusEffect> effects) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(CODEC_VERSION);
            out.writeInt(effects.size());
            for (ActiveStatusEffect effect : effects) {
                out.writeUTF(effect.effectKey().asString());
                out.writeInt(effect.amplifier());
                out.writeDouble(effect.intensity());
                out.writeLong(effect.remainingTicks());
                out.writeLong(effect.maxTicks());
                out.writeLong(effect.appliedAtEpochMillis());
                out.writeLong(effect.lastTickEpochMillis());
            }
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            logger.warn(LogChannel.STORAGE, "Failed to encode player status effects: " + e.getMessage());
            return new byte[0];
        }
    }

    public List<ActiveStatusEffect> decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int version = in.readInt();
            if (version != CODEC_VERSION) {
                return List.of();
            }
            int count = in.readInt();
            List<ActiveStatusEffect> result = new ArrayList<>(Math.max(0, count));
            for (int i = 0; i < count; i++) {
                Key key = Key.parse(in.readUTF());
                int amplifier = in.readInt();
                double intensity = in.readDouble();
                long remaining = in.readLong();
                long maxTicks = in.readLong();
                long appliedAt = in.readLong();
                long lastTick = in.readLong();
                result.add(new ActiveStatusEffect(key, amplifier, intensity, remaining, maxTicks, appliedAt, lastTick, Map.of()));
            }
            return result;
        } catch (Exception e) {
            logger.warn(LogChannel.STORAGE, "Failed to decode player status effects: " + e.getMessage());
            return List.of();
        }
    }
}
