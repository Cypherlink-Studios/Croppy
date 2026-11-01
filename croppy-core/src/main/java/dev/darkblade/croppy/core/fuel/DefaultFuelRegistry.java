package dev.darkblade.croppy.core.fuel;

import dev.darkblade.croppy.api.fuel.FuelProperties;
import dev.darkblade.croppy.api.fuel.FuelProvider;
import dev.darkblade.croppy.api.identifier.Key;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory registry mapping item {@link Key} identifiers to their {@link FuelProperties}.
 */
public final class DefaultFuelRegistry implements FuelProvider {

    private final Map<Key, FuelProperties> fuels = new ConcurrentHashMap<>();

    public DefaultFuelRegistry() {
        registerDefaultVanillaFuels();
    }

    /**
     * Registers fuel properties for a given item key.
     *
     * @param itemKey    namespaced item identifier
     * @param properties combustion properties
     */
    public void registerFuel(Key itemKey, FuelProperties properties) {
        Objects.requireNonNull(itemKey, "itemKey cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
        fuels.put(itemKey, properties);
    }

    /**
     * Unregisters a fuel item.
     *
     * @param itemKey item key to unregister
     * @return true if an entry was removed
     */
    public boolean unregisterFuel(Key itemKey) {
        if (itemKey == null) {
            return false;
        }
        return fuels.remove(itemKey) != null;
    }

    @Override
    public Optional<FuelProperties> resolveFuel(Key itemKey) {
        if (itemKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fuels.get(itemKey));
    }

    /**
     * @return unmodifiable snapshot of all registered fuels
     */
    public Map<Key, FuelProperties> getAllFuels() {
        return Map.copyOf(fuels);
    }

    /**
     * Clears all registered fuels.
     */
    public void clear() {
        fuels.clear();
    }

    /**
     * Registers baseline Minecraft vanilla fuel items and durations.
     */
    public void registerDefaultVanillaFuels() {
        // High density fuels
        registerFuel(Key.of("minecraft", "lava_bucket"), new FuelProperties(20000L, 1500.0, 1.2));
        registerFuel(Key.of("minecraft", "blaze_rod"), new FuelProperties(2400L, 1200.0, 1.1));
        registerFuel(Key.of("minecraft", "coal_block"), new FuelProperties(16000L, 800.0, 1.0));
        registerFuel(Key.of("minecraft", "dried_kelp_block"), new FuelProperties(4000L, 600.0, 1.0));

        // Standard solid fuels
        registerFuel(Key.of("minecraft", "coal"), new FuelProperties(1600L, 600.0, 1.0));
        registerFuel(Key.of("minecraft", "charcoal"), new FuelProperties(1600L, 600.0, 1.0));

        // Wood logs & planks
        FuelProperties logFuel = new FuelProperties(300L, 400.0, 0.9);
        FuelProperties plankFuel = new FuelProperties(300L, 400.0, 0.9);

        String[] logTypes = {
                "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log",
                "dark_oak_log", "mangrove_log", "cherry_log"
        };
        for (String log : logTypes) {
            registerFuel(Key.of("minecraft", log), logFuel);
        }

        String[] plankTypes = {
                "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
                "dark_oak_planks", "mangrove_planks", "cherry_planks", "bamboo_planks"
        };
        for (String plank : plankTypes) {
            registerFuel(Key.of("minecraft", plank), plankFuel);
        }

        // Low energy fuels
        registerFuel(Key.of("minecraft", "stick"), new FuelProperties(100L, 300.0, 0.8));
    }
}
