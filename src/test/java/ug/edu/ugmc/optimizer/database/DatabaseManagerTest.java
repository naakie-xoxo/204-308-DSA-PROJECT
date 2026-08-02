package ug.edu.ugmc.optimizer.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerTest {
    @TempDir
    Path temporaryDirectory;

    private Path database;

    @BeforeEach
    void createDatabase() throws Exception {
        database = temporaryDirectory.resolve("test.db");
        DatabaseManager.initializeDatabase(database, Path.of("schema.sql"), Path.of("data"));
    }

    @Test
    void createsEveryRequiredTable() throws Exception {
        try (Connection connection = connect();
             var result = connection.createStatement().executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            var names = new ArrayList<String>();
            while (result.next()) {
                names.add(result.getString(1));
            }
            Set<String> tables = names.stream().collect(Collectors.toSet());
            assertTrue(tables.containsAll(Set.of(
                    "locations", "roads", "service_requests", "resources",
                    "algorithm_runs", "audit_events")));
        }
    }

    @Test
    void importsTheRequiredSeedCounts() throws Exception {
        try (Connection connection = connect()) {
            assertEquals(50, count(connection, "locations"));
            assertEquals(100, count(connection, "roads"));
            assertEquals(300, count(connection, "service_requests"));
            assertEquals(30, count(connection, "resources"));
            assertEquals(30, count(connection, "algorithm_runs"));
        }
    }

    @Test
    void repeatedInitializationDoesNotDuplicateRoads() throws Exception {
        DatabaseManager.initializeDatabase(database, Path.of("schema.sql"), Path.of("data"));
        try (Connection connection = connect()) {
            assertEquals(100, count(connection, "roads"));
        }
    }

    @Test
    void foreignKeysRejectUnknownLocations() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO roads
                        (fromLocationId, toLocationId, distance, travelTime, roadConditionWeight)
                    VALUES ('UNKNOWN', 'LOC001', 1.0, 1, 1)
                    """));
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    private static int count(Connection connection, String table) throws SQLException {
        try (var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.getInt(1);
        }
    }
}
