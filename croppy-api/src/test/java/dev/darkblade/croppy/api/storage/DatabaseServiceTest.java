package dev.darkblade.croppy.api.storage;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {

    private static final class StubDatabaseService implements DatabaseService {
        private final String prefix;

        StubDatabaseService(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Connection getConnection() {
            return null;
        }

        @Override
        public String getTablePrefix() {
            return prefix;
        }

        @Override
        public DatabaseService scoped(String prefix) {
            if (!VALID_IDENTIFIER.matcher(prefix).matches()) {
                throw new IllegalArgumentException("Invalid prefix: " + prefix);
            }
            return new StubDatabaseService(prefix);
        }
    }

    @Test
    void enforcesTablePrefixAndSanitizesIdentifiers() {
        DatabaseService root = new StubDatabaseService("croppy_");
        assertEquals("croppy_crops", root.table("crops"));

        String prefix = "addon_" + DatabaseService.sanitizeIdentifier("croppy-brewery") + "_";
        DatabaseService brewery = root.scoped(prefix);
        assertEquals("addon_croppy_brewery_", brewery.getTablePrefix());
        assertEquals("addon_croppy_brewery_batches", brewery.table("batches"));

        assertThrows(IllegalArgumentException.class, () -> brewery.table("bad-table; DROP TABLE x;"));
        assertThrows(IllegalArgumentException.class, () -> root.scoped("invalid-prefix!"));
    }
}
