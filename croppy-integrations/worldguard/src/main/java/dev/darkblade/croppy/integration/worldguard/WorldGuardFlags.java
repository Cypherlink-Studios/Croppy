package dev.darkblade.croppy.integration.worldguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;

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
        register(logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE);
    }

    /**
     * Registers custom flags into WorldGuard's FlagRegistry. Must be executed in onLoad().
     *
     * @param logger logger for diagnostic output
     */
    public static synchronized void register(CroppyLogger logger) {
        if (registered) {
            return;
        }
        CroppyLogger log = logger != null ? logger : NoOpCroppyLogger.INSTANCE;

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            CROPPY_PLANT = registerStateFlag(registry, "croppy-plant", false, log);
            CROPPY_HARVEST = registerStateFlag(registry, "croppy-harvest", false, log);
            CROPPY_WATER = registerStateFlag(registry, "croppy-water", false, log);
            CROPPY_FERTILIZE = registerStateFlag(registry, "croppy-fertilize", false, log);
            CROPPY_COOK = registerStateFlag(registry, "croppy-cook", false, log);
            registered = true;
            log.debug(LogChannel.INTEGRATION, "Successfully registered custom WorldGuard flags for Croppy.");
        } catch (Throwable t) {
            log.warn(LogChannel.INTEGRATION, "Failed to register custom WorldGuard flags: " + t.getMessage(), t);
        }
    }

    private static StateFlag registerStateFlag(FlagRegistry registry, String name, boolean defaultValue, CroppyLogger logger) {
        try {
            StateFlag flag = new StateFlag(name, defaultValue);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag stateFlag) {
                return stateFlag;
            }
            logger.warn(LogChannel.INTEGRATION, "Flag conflict detected for existing non-state flag: " + name);
            return null;
        }
    }
}
