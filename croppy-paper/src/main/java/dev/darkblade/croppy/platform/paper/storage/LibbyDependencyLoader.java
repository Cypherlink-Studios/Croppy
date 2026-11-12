package dev.darkblade.croppy.platform.paper.storage;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import net.byteflux.libby.Library;
import net.byteflux.libby.PaperLibraryManager;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Manages runtime downloading and classloader injection of database drivers using Libby.
 */
public class LibbyDependencyLoader {

    private final Plugin plugin;
    private final CroppyLogger logger;
    private boolean loaded = false;

    public LibbyDependencyLoader(Plugin plugin) {
        this(plugin,
                Objects.requireNonNull(plugin, "plugin cannot be null").getLogger() != null
                        ? new JulCroppyLogger(plugin.getLogger())
                        : NoOpCroppyLogger.INSTANCE);
    }

    public LibbyDependencyLoader(Plugin plugin, CroppyLogger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    /**
     * Downloads and loads HikariCP 7.1.0 and SQLite JDBC into the plugin classloader.
     */
    public synchronized void loadDatabaseDrivers() {
        if (loaded) {
            return;
        }

        logger.debug(LogChannel.STORAGE, "Initializing Libby Paper library manager to load database drivers...");
        PaperLibraryManager libraryManager = new PaperLibraryManager(plugin);
        libraryManager.addMavenCentral();

        Library hikari = Library.builder()
                .groupId("com.zaxxer")
                .artifactId("HikariCP")
                .version("7.1.0")
                .build();

        Library sqlite = Library.builder()
                .groupId("org.xerial")
                .artifactId("sqlite-jdbc")
                .version("3.49.1.0")
                .build();

        logger.debug(LogChannel.STORAGE, "Loading HikariCP 7.1.0...");
        libraryManager.loadLibrary(hikari);

        logger.debug(LogChannel.STORAGE, "Loading SQLite JDBC 3.49.1.0...");
        libraryManager.loadLibrary(sqlite);

        loaded = true;
        logger.debug(LogChannel.STORAGE, "Database drivers successfully loaded into plugin classloader.");
    }

    public boolean areDriversLoaded() {
        return loaded;
    }
}
