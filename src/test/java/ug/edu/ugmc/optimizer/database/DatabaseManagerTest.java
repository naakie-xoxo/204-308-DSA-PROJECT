package ug.edu.ugmc.optimizer.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;
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
        try (Connection connection = connect()) {
            assertTrue(tableExists(connection, "locations"));
            assertTrue(tableExists(connection, "roads"));
            assertTrue(tableExists(connection, "service_requests"));
            assertTrue(tableExists(connection, "resources"));
            assertTrue(tableExists(connection, "algorithm_runs"));
            assertTrue(tableExists(connection, "audit_events"));
        }
    }

    @Test
    void importsTheRequiredSeedCounts() throws Exception {
        try (Connection connection = connect()) {
            assertEquals(50, count(connection, "locations"));
            assertEquals(100, count(connection, "roads"));
            assertEquals(300, count(connection, "service_requests"));
            assertEquals(30, count(connection, "resources"));
            // 8 algorithms x 6 assessed scales x 3 measured trials.
            assertEquals(144, count(connection, "algorithm_runs"));
        }
    }

    @Test
    void repeatedInitializationDoesNotDuplicateRoads() throws Exception {
        DatabaseManager.initializeDatabase(database, Path.of("schema.sql"), Path.of("data"));
        try (Connection connection = connect()) {
            assertEquals(100, count(connection, "roads"));
            assertEquals(300, count(connection, "service_requests"));
        }
    }

    @Test
    void foreignKeysRejectUnknownLocations() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO roads
                        (source_id, destination_id, distance, travel_time, road_condition_weight)
                    VALUES ('UNKNOWN', 'LOC001', 1.0, 1, 1)
                    """));
        }
    }

    @Test
    void loaderBuildsTheRamStructuresExactlyOnce() {
        DatabaseLoader loader = new DatabaseLoader("jdbc:sqlite:" + database.toAbsolutePath());
        CustomHashTable<String, ServiceRequest> requests = new CustomHashTable<>();
        CircularQueue<ServiceRequest> queue = new CircularQueue<>(300);
        CustomGraph graph = new CustomGraph(50);

        loader.loadServiceRequests(requests, queue);
        loader.loadGraph(graph);

        assertEquals(300, requests.getSize());
        assertEquals(300, queue.size());
        assertEquals(50, graph.getNumNodes());
        assertEquals("REQ-001", requests.get("REQ-001").getId());
        assertThrows(IllegalStateException.class,
                () -> loader.loadServiceRequests(requests, queue));
        assertThrows(IllegalStateException.class, () -> loader.loadGraph(graph));
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    private static int count(Connection connection, String table) throws SQLException {
        try (var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.getInt(1);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (var result = statement.executeQuery()) {
                return result.getInt(1) == 1;
            }
        }
    }
}
