package ug.edu.ugmc.optimizer.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/**
 * Loads the persistent hospital data into the application's custom in-memory
 * data structures during startup.
 *
 * <p>Each loading operation may be invoked only once on a loader instance.
 * A database failure is logged and converted to an application-startup
 * failure so that algorithms cannot run against partially loaded data.</p>
 */
public final class DatabaseLoader {

    private static final Logger LOGGER = Logger.getLogger(DatabaseLoader.class.getName());

    private static final String SERVICE_REQUESTS_SQL = """
            SELECT request_id, urgency_level, weight, value
            FROM service_requests
            """;

    private static final String LOCATIONS_SQL = """
            SELECT location_id
            FROM locations
            """;

    private static final String ROADS_SQL = """
            SELECT source_id, destination_id, travel_time
            FROM roads
            """;

    private final String connectionString;
    private boolean serviceRequestsLoadAttempted;
    private boolean graphLoadAttempted;

    /**
     * Creates a startup loader for an SQLite database.
     *
     * @param connectionString JDBC connection string, for example
     *                         {@code jdbc:sqlite:ugmc_optimizer.db}
     * @throws IllegalArgumentException if the connection string is null or blank
     */
    public DatabaseLoader(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("The SQLite JDBC connection string cannot be blank.");
        }
        this.connectionString = connectionString;
    }

    /**
     * Loads every service request into both the lookup table and processing
     * queue. This method can be called only once per loader instance.
     *
     * @param map custom request lookup table keyed by request ID
     * @param queue custom queue used for request processing
     * @throws IllegalArgumentException if either destination is null
     * @throws IllegalStateException if loading has already been attempted or
     *                               the database cannot be read
     */
    public synchronized void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> map,
            CircularQueue<ServiceRequest> queue) {
        requireDestination(map, "map");
        requireDestination(queue, "queue");
        ensureNotAttempted(serviceRequestsLoadAttempted, "Service requests");
        serviceRequestsLoadAttempted = true;

        try (Connection connection = DriverManager.getConnection(connectionString);
                PreparedStatement statement = connection.prepareStatement(SERVICE_REQUESTS_SQL);
                ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                String requestId = results.getString("request_id");
                int urgency = results.getInt("urgency_level");
                int weight = results.getInt("weight");
                int value = results.getInt("value");

                ServiceRequest request = new ServiceRequest(requestId, urgency, weight, value);
                map.put(requestId, request);
                queue.enqueue(request);
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Failed to load service requests from SQLite.", exception);
            throw new IllegalStateException("Unable to load service requests at startup.", exception);
        }
    }

    /**
     * Loads all locations before loading their weighted road connections into
     * the supplied graph. This method can be called only once per loader
     * instance.
     *
     * @param graph custom graph that will receive all nodes and edges
     * @throws IllegalArgumentException if the graph is null
     * @throws IllegalStateException if loading has already been attempted or
     *                               the database cannot be read
     */
    public synchronized void loadGraph(CustomGraph graph) {
        requireDestination(graph, "graph");
        ensureNotAttempted(graphLoadAttempted, "Graph");
        graphLoadAttempted = true;

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            try (PreparedStatement statement = connection.prepareStatement(LOCATIONS_SQL);
                    ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    graph.addNode(results.getString("location_id"));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(ROADS_SQL);
                    ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    graph.addEdge(
                            results.getString("source_id"),
                            results.getString("destination_id"),
                            results.getInt("travel_time"));
                }
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Failed to load the hospital graph from SQLite.", exception);
            throw new IllegalStateException("Unable to load the hospital graph at startup.", exception);
        }
    }

    private static void ensureNotAttempted(boolean alreadyAttempted, String dataSet) {
        if (alreadyAttempted) {
            throw new IllegalStateException(dataSet + " loading may be attempted only once at startup.");
        }
    }

    private static void requireDestination(Object destination, String name) {
        if (destination == null) {
            throw new IllegalArgumentException(name + " cannot be null.");
        }
    }
}
