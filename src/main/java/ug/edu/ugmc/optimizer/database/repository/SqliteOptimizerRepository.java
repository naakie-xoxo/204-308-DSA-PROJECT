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
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/** SQLite adapter for the application startup and reload port. */
public final class SqliteOptimizerRepository implements OptimizerRepository {

    private static final String REQUEST_COUNT_SQL = "SELECT COUNT(*) FROM service_requests";
    private static final String LOCATION_COUNT_SQL = "SELECT COUNT(*) FROM locations";

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
    public void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> lookup,
            CircularQueue<ServiceRequest> queue) {
        new DatabaseLoader(connectionString).loadServiceRequests(lookup, queue);
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
}
