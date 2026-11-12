package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.storage.CropStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory creating and configuring the appropriate CropStorage backend based on plugin configuration.
 */
public final class CropStorageFactory {

    private CropStorageFactory() {}

    public static CropStorage create(Plugin plugin, LibbyDependencyLoader libbyLoader) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return create(plugin, libbyLoader,
                plugin.getLogger() != null ? new JulCroppyLogger(plugin.getLogger()) : NoOpCroppyLogger.INSTANCE);
    }

    public static CropStorage create(Plugin plugin, LibbyDependencyLoader libbyLoader, CroppyLogger logger) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(logger, "logger cannot be null");
        FileConfiguration config = plugin.getConfig();

        String type = config.getString("storage.type", "pdc").trim().toLowerCase();

        if ("sqlite".equals(type)) {
            logger.debug(LogChannel.STORAGE, "Selected storage backend: SQLite with HikariCP");
            if (libbyLoader != null) {
                libbyLoader.loadDatabaseDrivers();
            }

            String fileName = config.getString("storage.sqlite.file", "crops.db");
            Path dbPath = plugin.getDataFolder().toPath().resolve(fileName);
            int maxPoolSize = config.getInt("storage.sqlite.pool.maximum-pool-size", 5);
            long timeout = config.getLong("storage.sqlite.pool.connection-timeout", 30000L);

            SqliteCropStorage storage = new SqliteCropStorage(dbPath, maxPoolSize, timeout, logger);
            storage.initialize();
            return storage;
        } else {
            logger.debug(LogChannel.STORAGE, "Selected storage backend: Native Paper Chunk PersistentDataContainer (PDC)");
            PdcCropStorage storage = new PdcCropStorage(plugin);
            storage.initialize();
            return storage;
        }
    }
}
