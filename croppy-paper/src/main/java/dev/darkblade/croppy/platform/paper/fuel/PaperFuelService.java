package dev.darkblade.croppy.platform.paper.fuel;

import dev.darkblade.croppy.api.fuel.FuelProperties;
import dev.darkblade.croppy.api.fuel.FuelProvider;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.core.fuel.DefaultFuelRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Paper platform fuel provider bridging pure domain fuel registries with
 * Bukkit ItemStack items, PersistentDataContainer tags, and vanilla fuels.
 */
public final class PaperFuelService implements FuelProvider {

    public static final NamespacedKey KEY_FUEL_TICKS = new NamespacedKey("croppy", "fuel_ticks");
    public static final NamespacedKey KEY_MAX_TEMP = new NamespacedKey("croppy", "fuel_max_temp");
    public static final NamespacedKey KEY_EFFICIENCY = new NamespacedKey("croppy", "fuel_efficiency");
    public static final NamespacedKey KEY_ITEM_ID = new NamespacedKey("croppy", "item_id");

    private final DefaultFuelRegistry coreRegistry;

    public PaperFuelService(DefaultFuelRegistry coreRegistry) {
        this.coreRegistry = Objects.requireNonNull(coreRegistry, "coreRegistry cannot be null");
    }

    public DefaultFuelRegistry getCoreRegistry() {
        return coreRegistry;
    }

    @Override
    public Optional<FuelProperties> resolveFuel(Key itemKey) {
        if (itemKey == null) {
            return Optional.empty();
        }
        return coreRegistry.resolveFuel(itemKey);
    }

    /**
     * Resolves combustion properties for a live Bukkit {@link ItemStack}.
     * Checks custom PDC tags first, then custom Croppy item id, and finally vanilla material.
     *
     * @param item Bukkit item stack
     * @return optional containing fuel properties if valid fuel
     */
    public Optional<FuelProperties> resolveFuel(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            // 1. Explicit PDC fuel duration tag (e.g. from ExecutableItems or custom recipes)
            if (pdc.has(KEY_FUEL_TICKS, PersistentDataType.LONG)) {
                Long ticks = pdc.get(KEY_FUEL_TICKS, PersistentDataType.LONG);
                if (ticks != null && ticks > 0) {
                    Double maxTemp = pdc.get(KEY_MAX_TEMP, PersistentDataType.DOUBLE);
                    Double efficiency = pdc.get(KEY_EFFICIENCY, PersistentDataType.DOUBLE);
                    double resolvedMaxTemp = (maxTemp != null && maxTemp > 0) ? maxTemp : FuelProperties.DEFAULT_MAX_TEMP;
                    double resolvedEfficiency = (efficiency != null && efficiency > 0) ? efficiency : FuelProperties.DEFAULT_EFFICIENCY;
                    return Optional.of(new FuelProperties(ticks, resolvedMaxTemp, resolvedEfficiency));
                }
            }

            // 2. Custom Croppy item id
            if (pdc.has(KEY_ITEM_ID, PersistentDataType.STRING)) {
                String id = pdc.get(KEY_ITEM_ID, PersistentDataType.STRING);
                if (id != null && !id.isBlank()) {
                    Optional<FuelProperties> props = coreRegistry.resolveFuel(Key.parse(id));
                    if (props.isPresent()) {
                        return props;
                    }
                }
            }
        }

        // 3. Vanilla material resolution
        String materialName = item.getType().name().toLowerCase(Locale.ROOT);
        Key vanillaKey = Key.of("minecraft", materialName);
        return coreRegistry.resolveFuel(vanillaKey);
    }

    /**
     * Checks whether an item stack can be used as fuel.
     *
     * @param item Bukkit item stack
     * @return true if item is recognized as fuel
     */
    public boolean isFuel(ItemStack item) {
        return resolveFuel(item).isPresent();
    }
}
