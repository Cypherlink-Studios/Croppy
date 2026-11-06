package dev.darkblade.croppy.integration.worldguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages custom WorldGuard StateFlags for fine-grained agricultural and culinary region management.
 */
public final class WorldGuardFlags {

    public static StateFlag CROPPY_PLANT;
    public static StateFlag CROPPY_HARVEST;
    public static StateFlag CROPPY_WATER;
    public static StateFlag CROPPY_FERTILIZE;
    public static StateFlag CROPPY_COOK;

    private static boolean registered = false;

    private WorldGuardFlags() {}

    /**
     * Registers custom flags into WorldGuard's FlagRegistry. Must be executed in onLoad().
     *
     * @param logger logger for diagnostic output
     */
    public static synchronized void register(Logger logger) {
        if (registered) {
            return;
        }

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            CROPPY_PLANT = registerStateFlag(registry, "croppy-plant", false, logger);
            CROPPY_HARVEST = registerStateFlag(registry, "croppy-harvest", false, logger);
            CROPPY_WATER = registerStateFlag(registry, "croppy-water", false, logger);
            CROPPY_FERTILIZE = registerStateFlag(registry, "croppy-fertilize", false, logger);
            CROPPY_COOK = registerStateFlag(registry, "croppy-cook", false, logger);
            registered = true;
            logger.info("Successfully registered custom WorldGuard flags for Croppy.");
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to register custom WorldGuard flags: " + t.getMessage(), t);
        }
    }

    private static StateFlag registerStateFlag(FlagRegistry registry, String name, boolean defaultValue, Logger logger) {
        try {
            StateFlag flag = new StateFlag(name, defaultValue);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag stateFlag) {
                return stateFlag;
            }
            logger.warning("Flag conflict detected for existing non-state flag: " + name);
            return null;
        }
    }
}
