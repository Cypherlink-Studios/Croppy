package dev.darkblade.croppy.platform.paper.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.storage.DatabaseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * HikariCP-backed SQLite {@link DatabaseService} supporting table-prefix-scoped views
 * for core and addon relational schemas.
 */
public final class SqliteDatabaseService implements DatabaseService {

    private final HikariDataSource dataSource;
    private final String tablePrefix;
    private final boolean owner;
    private final CroppyLogger logger;

    public SqliteDatabaseService(Path databaseFile, int maxPoolSize, long connectionTimeoutMs, CroppyLogger logger) {
        this(createDataSource(databaseFile, maxPoolSize, connectionTimeoutMs), "croppy_", true,
                logger != null ? logger : NoOpCroppyLogger.INSTANCE);
    }

    private SqliteDatabaseService(HikariDataSource dataSource, String tablePrefix, boolean owner, CroppyLogger logger) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.tablePrefix = validatePrefix(tablePrefix);
        this.owner = owner;
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    private static HikariDataSource createDataSource(Path databaseFile, int maxPoolSize, long connectionTimeoutMs) {
        Objects.requireNonNull(databaseFile, "databaseFile cannot be null");
        try {
            if (databaseFile.getParent() != null) {
                Files.createDirectories(databaseFile.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create database directory for " + databaseFile, e);
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("Croppy-Shared-SQLite");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(Math.max(1, maxPoolSize));
        config.setConnectionTimeout(Math.max(1000L, connectionTimeoutMs));
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("foreign_keys", "ON");

        HikariDataSource ds = new HikariDataSource(config);
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            ds.close();
            throw new IllegalStateException("Failed to initialize SQLite database service", e);
        }
        return ds;
    }

    private static String validatePrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        if (!VALID_IDENTIFIER.matcher(prefix).matches()) {
            throw new IllegalArgumentException("Invalid table prefix '" + prefix + "': must match [a-zA-Z0-9_]+");
        }
        return prefix;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public String getTablePrefix() {
        return tablePrefix;
    }

    @Override
    public DatabaseService scoped(String prefix) {
        return new SqliteDatabaseService(dataSource, validatePrefix(prefix), false, logger);
    }

    public void shutdown() {
        if (owner && !dataSource.isClosed()) {
            logger.debug(LogChannel.STORAGE, "Closing shared SQLite database pool...");
            dataSource.close();
        }
    }
}
