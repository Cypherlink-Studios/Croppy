package dev.darkblade.croppy.platform.paper.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.storage.CropStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.crop.DefaultCropInstance;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage implementation persisting crop instances to an indexed SQLite database using HikariCP.
 */
public class SqliteCropStorage implements CropStorage {

    private final HikariDataSource dataSource;
    private final Logger logger;

    public SqliteCropStorage(Path databaseFile, int maxPoolSize, long connectionTimeoutMs, Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        Objects.requireNonNull(databaseFile, "databaseFile cannot be null");

        HikariConfig config = new HikariConfig();
        config.setPoolName("Croppy-SQLite-Pool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        config.setMaximumPoolSize(Math.max(1, maxPoolSize));
        config.setConnectionTimeout(Math.max(1000L, connectionTimeoutMs));

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void initialize() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS croppy_crops (
                    world VARCHAR(64) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    chunk_x INT NOT NULL,
                    chunk_z INT NOT NULL,
                    species VARCHAR(128) NOT NULL,
                    stage_index INT NOT NULL,
                    progress DOUBLE NOT NULL,
                    planted_at BIGINT NOT NULL,
                    last_updated BIGINT NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_croppy_chunk ON croppy_crops (world, chunk_x, chunk_z);");
            logger.info("SQLite database storage initialized and schema verified.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize SQLite crop database schema", e);
            throw new IllegalStateException("Failed to initialize SQLite storage", e);
        }
    }

    @Override
    public Collection<CropInstance> loadChunk(String worldName, int chunkX, int chunkZ) {
        String sql = "SELECT species, x, y, z, stage_index, progress, planted_at, last_updated FROM croppy_crops WHERE world = ? AND chunk_x = ? AND chunk_z = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worldName);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);

            try (ResultSet rs = ps.executeQuery()) {
                List<CropInstance> list = new ArrayList<>();
                while (rs.next()) {
                    Key speciesKey = Key.parse(rs.getString("species"));
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    int stageIndex = rs.getInt("stage_index");
                    double progress = rs.getDouble("progress");
                    long plantedAt = rs.getLong("planted_at");
                    long lastUpdated = rs.getLong("last_updated");

                    BlockPosition pos = BlockPosition.of(worldName, x, y, z);
                    list.add(new DefaultCropInstance(speciesKey, pos, stageIndex, progress, plantedAt, lastUpdated));
                }
                return list;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load crops for chunk [" + chunkX + ", " + chunkZ + "] in world " + worldName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void saveChunk(String worldName, int chunkX, int chunkZ, Collection<CropInstance> crops) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete previous records for this chunk
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM croppy_crops WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
                    del.setString(1, worldName);
                    del.setInt(2, chunkX);
                    del.setInt(3, chunkZ);
                    del.executeUpdate();
                }

                // Batch insert current crops
                if (crops != null && !crops.isEmpty()) {
                    String insertSql = """
                        INSERT INTO croppy_crops (world, x, y, z, chunk_x, chunk_z, species, stage_index, progress, planted_at, last_updated)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        for (CropInstance crop : crops) {
                            BlockPosition pos = crop.getPosition();
                            ins.setString(1, pos.worldName());
                            ins.setInt(2, pos.x());
                            ins.setInt(3, pos.y());
                            ins.setInt(4, pos.z());
                            ins.setInt(5, pos.x() >> 4);
                            ins.setInt(6, pos.z() >> 4);
                            ins.setString(7, crop.getSpeciesKey().asString());
                            ins.setInt(8, crop.getStageIndex());
                            ins.setDouble(9, crop.getProgress());
                            ins.setLong(10, crop.getPlantedAtMillis());
                            ins.setLong(11, crop.getLastUpdatedMillis());
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save chunk [" + chunkX + ", " + chunkZ + "] to SQLite", e);
        }
    }

    @Override
    public void saveCrop(CropInstance crop) {
        Objects.requireNonNull(crop, "crop cannot be null");
        BlockPosition pos = crop.getPosition();

        String sql = """
            INSERT INTO croppy_crops (world, x, y, z, chunk_x, chunk_z, species, stage_index, progress, planted_at, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(world, x, y, z) DO UPDATE SET
                stage_index = excluded.stage_index,
                progress = excluded.progress,
                last_updated = excluded.last_updated
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pos.worldName());
            ps.setInt(2, pos.x());
            ps.setInt(3, pos.y());
            ps.setInt(4, pos.z());
            ps.setInt(5, pos.x() >> 4);
            ps.setInt(6, pos.z() >> 4);
            ps.setString(7, crop.getSpeciesKey().asString());
            ps.setInt(8, crop.getStageIndex());
            ps.setDouble(9, crop.getProgress());
            ps.setLong(10, crop.getPlantedAtMillis());
            ps.setLong(11, crop.getLastUpdatedMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save crop at " + pos + " to SQLite", e);
        }
    }

    @Override
    public void deleteCrop(BlockPosition position) {
        Objects.requireNonNull(position, "position cannot be null");
        String sql = "DELETE FROM croppy_crops WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, position.worldName());
            ps.setInt(2, position.x());
            ps.setInt(3, position.y());
            ps.setInt(4, position.z());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete crop at " + position + " from SQLite", e);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing SQLite HikariCP connection pool...");
            dataSource.close();
            logger.info("SQLite HikariCP pool closed.");
        }
    }
}
