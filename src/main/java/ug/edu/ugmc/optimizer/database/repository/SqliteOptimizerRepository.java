package ug.edu.ugmc.optimizer.database.repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import ug.edu.ugmc.optimizer.application.ports.OptimizerRepository;
import ug.edu.ugmc.optimizer.database.DatabaseLoader;
import ug.edu.ugmc.optimizer.database.DatabaseManager;
import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;
import ug.edu.ugmc.optimizer.models.ServiceRequest.Status;

/** SQLite adapter for the application startup and reload port. */
public final class SqliteOptimizerRepository implements OptimizerRepository {

    private static final String REQUEST_COUNT_SQL = "SELECT COUNT(*) FROM service_requests";
    private static final String LOCATION_COUNT_SQL = "SELECT COUNT(*) FROM locations";
    private static final String REQUEST_STATUS_COUNT_SQL =
            "SELECT COUNT(*) FROM service_requests WHERE status = ?";
    private static final String UPDATE_REQUEST_STATUS_SQL = """
            UPDATE service_requests
            SET status = ?
            WHERE request_id = ? AND status = ?
            """;

    private final Path databasePath;
    private final Path schemaPath;
    private final Path dataDirectory;
    private final String connectionString;

    public SqliteOptimizerRepository(Path databasePath, Path schemaPath, Path dataDirectory) {
        if (databasePath == null || schemaPath == null || dataDirectory == null) {
            throw new IllegalArgumentException("Database, schema, and data paths are required.");
        }
        this.databasePath = databasePath.toAbsolutePath().normalize();
        this.schemaPath = schemaPath;
        this.dataDirectory = dataDirectory;
        this.connectionString = "jdbc:sqlite:" + this.databasePath;
    }

    @Override
    public void initialize() throws Exception {
        DatabaseManager.initializeDatabase(databasePath, schemaPath, dataDirectory);
    }

    @Override
    public int countServiceRequests() throws Exception {
        return count(REQUEST_COUNT_SQL);
    }

    @Override
    public int countLocations() throws Exception {
        return count(LOCATION_COUNT_SQL);
    }

    @Override
    public int countServiceRequestsByStatus(Status status) throws Exception {
        requireStatus(status);
        try (Connection connection = DriverManager.getConnection(connectionString);
                PreparedStatement statement = connection.prepareStatement(REQUEST_STATUS_COUNT_SQL)) {
            statement.setString(1, status.databaseValue());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Database status count query returned no result.");
                }
                return result.getInt(1);
            }
        }
    }

    @Override
    public void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> lookup,
            CircularQueue<ServiceRequest> queue) {
        new DatabaseLoader(connectionString).loadServiceRequests(lookup, queue);
    }

    @Override
    public void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> lookup,
            CircularQueue<ServiceRequest> queue,
            DynamicArray<ServiceRequest> allRequests) {
        new DatabaseLoader(connectionString).loadServiceRequests(lookup, queue, allRequests);
    }

    @Override
    public boolean updateRequestStatus(
            String requestId, Status expectedStatus, Status newStatus) throws Exception {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request ID cannot be blank.");
        }
        requireStatus(expectedStatus);
        requireStatus(newStatus);
        try (Connection connection = DriverManager.getConnection(connectionString);
                PreparedStatement statement = connection.prepareStatement(UPDATE_REQUEST_STATUS_SQL)) {
            statement.setString(1, newStatus.databaseValue());
            statement.setString(2, requestId);
            statement.setString(3, expectedStatus.databaseValue());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public void loadGraph(CustomGraph graph) {
        new DatabaseLoader(connectionString).loadGraph(graph);
    }

    private int count(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString);
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new IllegalStateException("Database count query returned no result.");
            }
            return result.getInt(1);
        }
    }

    private static void requireStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Request status is required.");
        }
    }
}
