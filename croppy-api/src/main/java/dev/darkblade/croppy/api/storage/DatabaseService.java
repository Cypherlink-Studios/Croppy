package dev.darkblade.croppy.api.storage;

import dev.darkblade.croppy.api.registry.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared relational database service port providing pooled SQL connections and
 * table-prefix isolation for core and addon schemas.
 */
public interface DatabaseService extends Service {

    Pattern VALID_IDENTIFIER = Pattern.compile("[a-zA-Z0-9_]+");

    /**
     * Functional consumer receiving a managed {@link Connection}.
     */
    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    /**
     * Functional query receiving a managed {@link Connection} and returning a result.
     */
    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    /**
     * Acquires a JDBC connection from the underlying pool.
     * Callers must close the returned connection (e.g., via try-with-resources).
     *
     * @return open JDBC connection
     * @throws SQLException if connection acquisition fails
     */
    Connection getConnection() throws SQLException;

    /**
     * @return the table prefix enforced by this service instance (e.g. {@code "croppy_"} or {@code "addon_brewery_"})
     */
    String getTablePrefix();

    /**
     * Resolves a logical table name into its prefixed physical table name.
     *
     * @param tableName logical table name matching {@code [a-zA-Z0-9_]+}
     * @return prefixed physical table name
     * @throws IllegalArgumentException if {@code tableName} is invalid
     */
    default String table(String tableName) {
        Objects.requireNonNull(tableName, "tableName cannot be null");
        if (!VALID_IDENTIFIER.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name '" + tableName + "': must match [a-zA-Z0-9_]+");
        }
        return getTablePrefix() + tableName;
    }

    /**
     * Returns a scoped view sharing the same underlying connection pool but enforcing {@code prefix}.
     *
     * @param prefix table prefix matching {@code [a-zA-Z0-9_]+}
     * @return scoped {@link DatabaseService}
     * @throws IllegalArgumentException if {@code prefix} is invalid
     */
    DatabaseService scoped(String prefix);

    /**
     * Executes a SQL unit of work with an automatically closed connection.
     *
     * @param action SQL work
     * @throws SQLException if execution fails
     */
    default void execute(SqlConsumer action) throws SQLException {
        Objects.requireNonNull(action, "action cannot be null");
        try (Connection conn = getConnection()) {
            action.accept(conn);
        }
    }

    /**
     * Executes a SQL query returning a result with an automatically closed connection.
     *
     * @param query SQL query
     * @param <T>   result type
     * @return query result
     * @throws SQLException if execution fails
     */
    default <T> T query(SqlFunction<T> query) throws SQLException {
        Objects.requireNonNull(query, "query cannot be null");
        try (Connection conn = getConnection()) {
            return query.apply(conn);
        }
    }

    /**
     * Sanitizes an arbitrary identifier (such as an addon ID) into a valid SQL prefix segment.
     *
     * @param raw raw identifier
     * @return sanitized lowercase alphanumeric/underscore string
     */
    static String sanitizeIdentifier(String raw) {
        Objects.requireNonNull(raw, "raw cannot be null");
        String sanitized = raw.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty after sanitization: " + raw);
        }
        return sanitized;
    }
}
