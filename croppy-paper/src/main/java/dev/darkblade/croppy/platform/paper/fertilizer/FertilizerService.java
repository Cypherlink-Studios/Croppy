package dev.darkblade.croppy.platform.paper.fertilizer;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.world.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing fertilizer items, identification, and transient pre-plant soil buffers.
 */
public class FertilizerService {

    private final FertilizerRegistry fertilizerRegistry;
    private final NamespacedKey fertilizerKey;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // 30-second transient buffer for unplanted fertilized soil
    private final Map<BlockPosition, BufferedFertilizer> soilBuffer = new ConcurrentHashMap<>();

    private record BufferedFertilizer(Key fertilizerKey, long expiresAtMillis) {}

    public FertilizerService(Plugin plugin, FertilizerRegistry fertilizerRegistry) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        this.fertilizerRegistry = Objects.requireNonNull(fertilizerRegistry, "fertilizerRegistry cannot be null");
        this.fertilizerKey = new NamespacedKey(plugin, "fertilizer");
    }

    /**
     * Creates an ItemStack representing the given fertilizer.
     */
    public ItemStack createFertilizerItem(Fertilizer fertilizer) {
        Material mat = Material.matchMaterial(fertilizer.material());
        if (mat == null) {
            mat = Material.BONE_MEAL;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(fertilizerKey, PersistentDataType.STRING, fertilizer.getKey().asString());
        if (fertilizer.customModelData() > 0) {
            meta.setCustomModelData(fertilizer.customModelData());
        }

        Component name = miniMessage.deserialize(fertilizer.name()).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        for (String line : fertilizer.description()) {
            lore.add(miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Identifies the fertilizer associated with an ItemStack, if any.
     */
    public Optional<Fertilizer> identifyFertilizer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();

        // 1. Check PDC
        String keyStr = meta.getPersistentDataContainer().get(fertilizerKey, PersistentDataType.STRING);
        if (keyStr != null) {
            try {
                Key key = Key.parse(keyStr);
                return fertilizerRegistry.get(key);
            } catch (Exception ignored) {}
        }

        // 2. Fallback check by material and custom model data
        int cmd = meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
        String matName = item.getType().name();
        for (Fertilizer f : fertilizerRegistry.values()) {
            if (f.material().equalsIgnoreCase(matName) && f.customModelData() == cmd && cmd > 0) {
                return Optional.of(f);
            }
        }

        return Optional.empty();
    }

    /**
     * Buffers a fertilizer application on empty tilled farmland for up to 30 seconds.
     */
    public void bufferSoilFertilizer(BlockPosition position, Key fertilizer) {
        soilBuffer.put(position, new BufferedFertilizer(fertilizer, System.currentTimeMillis() + 30000L));
    }

    /**
     * Consumes and returns a buffered fertilizer if present and not expired.
     */
    public Optional<Key> consumeBufferedSoilFertilizer(BlockPosition position) {
        BufferedFertilizer buffered = soilBuffer.remove(position);
        if (buffered == null) {
            return Optional.empty();
        }

        if (System.currentTimeMillis() > buffered.expiresAtMillis()) {
            return Optional.empty();
        }

        return Optional.of(buffered.fertilizerKey());
    }

    /**
     * Cleans up expired buffer entries.
     */
    public void cleanExpiredBuffer() {
        long now = System.currentTimeMillis();
        soilBuffer.entrySet().removeIf(entry -> now > entry.getValue().expiresAtMillis());
    }
}
