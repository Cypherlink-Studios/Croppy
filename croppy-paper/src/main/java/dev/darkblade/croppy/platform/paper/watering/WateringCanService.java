package dev.darkblade.croppy.platform.paper.watering;

import dev.darkblade.croppy.api.world.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing watering can items, capacity, lore rendering, and AoE calculations.
 */
public class WateringCanService {

    private final NamespacedKey tierKey;
    private final NamespacedKey waterLevelKey;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public WateringCanService(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        this.tierKey = new NamespacedKey(plugin, "watering_can_tier");
        this.waterLevelKey = new NamespacedKey(plugin, "water_level");
    }

    public ItemStack createWateringCan(WateringCanTier tier) {
        ItemStack item = new ItemStack(tier.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.id());
        meta.getPersistentDataContainer().set(waterLevelKey, PersistentDataType.INTEGER, tier.capacity());
        if (tier.customModelData() > 0) {
            meta.setCustomModelData(tier.customModelData());
        }

        updateItemVisuals(meta, tier, tier.capacity());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWateringCan(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(tierKey, PersistentDataType.STRING);
    }

    public Optional<WateringCanTier> getTier(ItemStack item) {
        if (!isWateringCan(item)) return Optional.empty();
        String tierId = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.STRING);
        return Optional.ofNullable(WateringCanTier.parse(tierId));
    }

    public int getWaterLevel(ItemStack item) {
        if (!isWateringCan(item)) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(waterLevelKey, PersistentDataType.INTEGER);
        return level != null ? level : 0;
    }

    public boolean setWaterLevel(ItemStack item, int level) {
        if (!isWateringCan(item)) return false;
        Optional<WateringCanTier> tierOpt = getTier(item);
        if (tierOpt.isEmpty()) return false;
        WateringCanTier tier = tierOpt.get();

        int clamped = Math.clamp(level, 0, tier.capacity());
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(waterLevelKey, PersistentDataType.INTEGER, clamped);
        updateItemVisuals(meta, tier, clamped);
        item.setItemMeta(meta);
        return true;
    }

    public boolean refill(ItemStack item) {
        Optional<WateringCanTier> tierOpt = getTier(item);
        if (tierOpt.isEmpty()) return false;
        return setWaterLevel(item, tierOpt.get().capacity());
    }

    public boolean consumeWater(ItemStack item, int amount) {
        int current = getWaterLevel(item);
        if (current < amount) return false;
        return setWaterLevel(item, current - amount);
    }

    public List<BlockPosition> calculateAoe(BlockPosition center, BlockFace facing, WateringCanTier tier) {
        List<BlockPosition> result = new ArrayList<>();
        int cx = center.x();
        int cy = center.y();
        int cz = center.z();
        String world = center.worldName();

        int w = tier.aoeWidth();
        int l = tier.aoeLength();

        if (w == 1 && l == 1) {
            result.add(center);
            return result;
        }

        if (w == 1 && l == 3) {
            // Line along facing
            int dx = facing.getModX();
            int dz = facing.getModZ();
            if (dx == 0 && dz == 0) {
                dx = 0;
                dz = 1;
            }
            result.add(BlockPosition.of(world, cx - dx, cy, cz - dz));
            result.add(center);
            result.add(BlockPosition.of(world, cx + dx, cy, cz + dz));
            return result;
        }

        // Square area centered on target (3x3 or 5x5)
        int radius = w / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                result.add(BlockPosition.of(world, cx + dx, cy, cz + dz));
            }
        }

        return result;
    }

    private void updateItemVisuals(ItemMeta meta, WateringCanTier tier, int currentLevel) {
        Component name = miniMessage.deserialize(tier.displayName()).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        int max = tier.capacity();
        int filledBars = (int) Math.round(((double) currentLevel / max) * 10);
        String bar = "█".repeat(filledBars) + "░".repeat(Math.max(0, 10 - filledBars));

        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize("<gray>Área: <white>" + tier.aoeWidth() + "x" + tier.aoeLength() + "</white></gray>")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(miniMessage.deserialize("<gray>Agua: <aqua>[" + bar + "] " + currentLevel + "/" + max + "</aqua></gray>")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<dark_gray>Click derecho en agua para recargar</dark_gray>")
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
    }
}
