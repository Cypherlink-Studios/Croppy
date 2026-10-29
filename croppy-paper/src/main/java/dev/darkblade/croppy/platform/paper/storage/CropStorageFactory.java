package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.storage.CropStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Factory creating and configuring the appropriate CropStorage backend based on plugin configuration.
 */
public final class CropStorageFactory {

    private CropStorageFactory() {}

    public static CropStorage create(Plugin plugin, LibbyDependencyLoader libbyLoader) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Logger logger = plugin.getLogger();
        FileConfiguration config = plugin.getConfig();

        String type = config.getString("storage.type", "pdc").trim().toLowerCase();

        if ("sqlite".equals(type)) {
            logger.info("Selected storage backend: SQLite with HikariCP");
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
            logger.info("Selected storage backend: Native Paper Chunk PersistentDataContainer (PDC)");
            PdcCropStorage storage = new PdcCropStorage(plugin);
            storage.initialize();
            return storage;
        }
    }
}
