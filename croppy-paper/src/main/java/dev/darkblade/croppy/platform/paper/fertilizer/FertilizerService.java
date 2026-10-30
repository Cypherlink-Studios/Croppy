package dev.darkblade.croppy.platform.paper.fertilizer;

import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing fertilizer items, identification, and transient pre-plant soil buffers.
 */
public class FertilizerService {

    private final FertilizerRegistry fertilizerRegistry;
    private final ItemFactory itemFactory;
    private final NamespacedKey fertilizerKey;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey categoryKey;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // 30-second transient buffer for unplanted fertilized soil
    private final Map<BlockPosition, BufferedFertilizer> soilBuffer = new ConcurrentHashMap<>();

    private record BufferedFertilizer(Key fertilizerKey, long expiresAtMillis) {}

    public FertilizerService(Plugin plugin, FertilizerRegistry fertilizerRegistry, ItemFactory itemFactory) {
        this.fertilizerRegistry = Objects.requireNonNull(fertilizerRegistry, "fertilizerRegistry cannot be null");
        this.itemFactory = itemFactory;
        this.fertilizerKey = new NamespacedKey("croppy", "fertilizer");
        this.itemIdKey = new NamespacedKey("croppy", "item_id");
        this.categoryKey = new NamespacedKey("croppy", "category");
    }

    public FertilizerService(Plugin plugin, FertilizerRegistry fertilizerRegistry) {
        this(plugin, fertilizerRegistry, null);
    }

    /**
     * Creates an ItemStack representing the given fertilizer.
     */
    public ItemStack createFertilizerItem(Fertilizer fertilizer) {
        ItemStack item;
        if (itemFactory != null) {
            Optional<ItemDefinition> defOpt = itemFactory.getItemRegistry().get(fertilizer.getKey());
            if (defOpt.isPresent()) {
                item = itemFactory.createItemStack(defOpt.get());
            } else {
                item = createLegacyItemStack(fertilizer);
            }
        } else {
            item = createLegacyItemStack(fertilizer);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(itemIdKey, PersistentDataType.STRING, fertilizer.getKey().asString());
            pdc.set(categoryKey, PersistentDataType.STRING, "fertilizers");
            pdc.set(fertilizerKey, PersistentDataType.STRING, fertilizer.getKey().asString());
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createLegacyItemStack(Fertilizer fertilizer) {
        Material mat = Material.matchMaterial(fertilizer.material());
        if (mat == null) {
            mat = Material.BONE_MEAL;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

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

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1. Check standardized croppy:item_id
        String itemIdStr = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (itemIdStr != null) {
            try {
                Key key = Key.parse(itemIdStr);
                Optional<Fertilizer> fert = fertilizerRegistry.get(key);
                if (fert.isPresent()) return fert;
            } catch (Exception ignored) {}
        }

        // 2. Check legacy PDC key
        String keyStr = pdc.get(fertilizerKey, PersistentDataType.STRING);
        if (keyStr != null) {
            try {
                Key key = Key.parse(keyStr);
                Optional<Fertilizer> fert = fertilizerRegistry.get(key);
                if (fert.isPresent()) return fert;
            } catch (Exception ignored) {}
        }

        // 3. Fallback check by material and custom model data
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
     * Retrieves and removes any active buffered fertilizer at the given position if not expired.
     */
    public Optional<Key> consumeBufferedFertilizer(BlockPosition position) {
        BufferedFertilizer buffered = soilBuffer.remove(position);
        if (buffered == null) {
            return Optional.empty();
        }
        if (System.currentTimeMillis() > buffered.expiresAtMillis()) {
            return Optional.empty();
        }
        return Optional.of(buffered.fertilizerKey());
    }

    public Optional<Key> consumeBufferedSoilFertilizer(BlockPosition position) {
        return consumeBufferedFertilizer(position);
    }

    /**
     * Clears all expired buffered fertilizers.
     */
    public void purgeExpiredBuffers() {
        long now = System.currentTimeMillis();
        soilBuffer.entrySet().removeIf(e -> now > e.getValue().expiresAtMillis());
    }
}
