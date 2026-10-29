package dev.darkblade.croppy.platform.paper.storage;

import net.byteflux.libby.Library;
import net.byteflux.libby.PaperLibraryManager;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Manages runtime downloading and classloader injection of database drivers using Libby.
 */
public class LibbyDependencyLoader {

    private final Plugin plugin;
    private final Logger logger;
    private boolean loaded = false;

    public LibbyDependencyLoader(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.logger = plugin.getLogger();
    }

    /**
     * Downloads and loads HikariCP 7.1.0 and SQLite JDBC into the plugin classloader.
     */
    public synchronized void loadDatabaseDrivers() {
        if (loaded) {
            return;
        }

        logger.info("Initializing Libby Paper library manager to load database drivers...");
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

        logger.info("Loading HikariCP 7.1.0...");
        libraryManager.loadLibrary(hikari);

        logger.info("Loading SQLite JDBC 3.49.1.0...");
        libraryManager.loadLibrary(sqlite);

        loaded = true;
        logger.info("Database drivers successfully loaded into plugin classloader.");
    }

    public boolean areDriversLoaded() {
        return loaded;
    }
}
