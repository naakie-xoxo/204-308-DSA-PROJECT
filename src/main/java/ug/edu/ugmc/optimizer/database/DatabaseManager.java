package ug.edu.ugmc.optimizer.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates and seeds a local SQLite database from the repository's canonical inputs. */
public final class DatabaseManager {
    private static final Path DEFAULT_DATABASE = Path.of("hospital_system.db");
    private static final Path DEFAULT_SCHEMA = Path.of("schema.sql");
    private static final Path DEFAULT_DATA_DIRECTORY = Path.of("data");

    private DatabaseManager() {
    }

    public static void main(String[] args) throws Exception {
        Path database = args.length > 0 ? Path.of(args[0]) : DEFAULT_DATABASE;
        Path schema = args.length > 1 ? Path.of(args[1]) : DEFAULT_SCHEMA;
        Path dataDirectory = args.length > 2 ? Path.of(args[2]) : DEFAULT_DATA_DIRECTORY;

        initializeDatabase(database, schema, dataDirectory);
        System.out.println("Database ready: " + database.toAbsolutePath().normalize());
    }

    /**
     * Applies the schema and upserts every seed file in one transaction.
     * Re-running this method is safe and does not duplicate road records.
     */
    public static void initializeDatabase(Path database, Path schema, Path dataDirectory)
            throws IOException, SQLException {
        Path absoluteDatabase = database.toAbsolutePath().normalize();
        Path parent = absoluteDatabase.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + absoluteDatabase)) {
            enableForeignKeys(connection);
            connection.setAutoCommit(false);
            try {
                applySchema(connection, schema);
                loadLocations(connection, dataDirectory.resolve("locations.csv"));
                loadRoads(connection, dataDirectory.resolve("roads.csv"));
                loadResources(connection, dataDirectory.resolve("resources.csv"));
                loadServiceRequests(connection, dataDirectory.resolve("service_requests.csv"));
                loadAlgorithmRuns(connection, dataDirectory.resolve("algorithm_runs.csv"));
                connection.commit();
            } catch (IOException | SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void enableForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static void applySchema(Connection connection, Path schemaPath) throws IOException, SQLException {
        String schema = Files.readString(schemaPath, StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .replaceAll("(?m)^\\s*--.*$", "");
        try (Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql.trim());
                }
            }
        }
    }

    private static void loadLocations(Connection connection, Path csvPath) throws IOException, SQLException {
        String sql = """
                INSERT INTO locations
                    (location_id, location_name, area, type, x_coordinate, y_coordinate)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(location_id) DO UPDATE SET
                    location_name=excluded.location_name, area=excluded.area, type=excluded.type,
                    x_coordinate=excluded.x_coordinate, y_coordinate=excluded.y_coordinate
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            readCsv(csvPath, "locationId,name,area,type,xCoordinate,yCoordinate", 6, row -> {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.setString(4, row[3]);
                statement.setDouble(5, Double.parseDouble(row[4]));
                statement.setDouble(6, Double.parseDouble(row[5]));
                statement.addBatch();
            });
            statement.executeBatch();
        }
    }

    private static void loadRoads(Connection connection, Path csvPath) throws IOException, SQLException {
        String sql = """
                INSERT INTO roads
                    (source_id, destination_id, distance, travel_time, road_condition_weight)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(source_id, destination_id) DO UPDATE SET
                    distance=excluded.distance, travel_time=excluded.travel_time,
                    road_condition_weight=excluded.road_condition_weight
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            readCsv(csvPath,
                    "fromLocationId,toLocationId,distance,travelTime,roadConditionWeight", 5, row -> {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setDouble(3, Double.parseDouble(row[2]));
                statement.setInt(4, Integer.parseInt(row[3]));
                statement.setInt(5, Integer.parseInt(row[4]));
                statement.addBatch();
            });
            statement.executeBatch();
        }
    }

    private static void loadResources(Connection connection, Path csvPath) throws IOException, SQLException {
        String sql = """
                INSERT INTO resources
                    (resource_id, type, home_location, capacity, availability_status)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(resource_id) DO UPDATE SET
                    type=excluded.type, home_location=excluded.home_location,
                    capacity=excluded.capacity, availability_status=excluded.availability_status
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            readCsv(csvPath, "resourceId,type,homeLocation,capacity,availabilityStatus", 5, row -> {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.setInt(4, Integer.parseInt(row[3]));
                statement.setString(5, row[4]);
                statement.addBatch();
            });
            statement.executeBatch();
        }
    }

    private static void loadServiceRequests(Connection connection, Path csvPath)
            throws IOException, SQLException {
        String sql = """
                INSERT INTO service_requests
                    (request_id, patient_name, source_id, destination_id, category,
                     urgency_level, weight, value, time_submitted, deadline, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(request_id) DO UPDATE SET
                    patient_name=excluded.patient_name, source_id=excluded.source_id,
                    destination_id=excluded.destination_id, category=excluded.category,
                    urgency_level=excluded.urgency_level, weight=excluded.weight,
                    value=excluded.value, time_submitted=excluded.time_submitted,
                    deadline=excluded.deadline, status=excluded.status
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            readCsv(csvPath,
                    "request_id,patient_name,source_id,destination_id,category,urgency_level,weight,value,time_submitted,deadline,status",
                    11, row -> {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.setString(4, row[3]);
                statement.setString(5, row[4]);
                statement.setInt(6, Integer.parseInt(row[5]));
                statement.setInt(7, Integer.parseInt(row[6]));
                statement.setInt(8, Integer.parseInt(row[7]));
                statement.setString(9, row[8]);
                statement.setString(10, row[9]);
                statement.setString(11, row[10]);
                statement.addBatch();
            });
            statement.executeBatch();
        }
    }

    private static void loadAlgorithmRuns(Connection connection, Path csvPath)
            throws IOException, SQLException {
        String sql = """
                INSERT INTO algorithm_runs
                    (run_id, algorithm_name, input_size, time_ns, memory_kb, date_run)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(run_id) DO UPDATE SET
                    algorithm_name=excluded.algorithm_name, input_size=excluded.input_size,
                    time_ns=excluded.time_ns, memory_kb=excluded.memory_kb,
                    date_run=excluded.date_run
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            readCsv(csvPath, "runId,algorithmName,inputSize,timeNs,memoryKb,dateRun", 6, row -> {
                statement.setInt(1, Integer.parseInt(row[0]));
                statement.setString(2, row[1]);
                statement.setInt(3, Integer.parseInt(row[2]));
                statement.setLong(4, Long.parseLong(row[3]));
                statement.setLong(5, Long.parseLong(row[4]));
                statement.setString(6, row[5]);
                statement.addBatch();
            });
            statement.executeBatch();
        }
    }

    private static void readCsv(Path path, String expectedHeader, int expectedColumns,
                                CsvRowConsumer consumer) throws IOException, SQLException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.replace("\uFEFF", "").equals(expectedHeader)) {
                throw new IOException("Unexpected CSV header in " + path + ". Expected: " + expectedHeader);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] row = parseCsvLine(line);
                if (row.length != expectedColumns) {
                    throw new IOException("Invalid column count in " + path + " at line " + lineNumber);
                }
                consumer.accept(row);
            }
        }
    }

    /** Small RFC-4180-compatible parser sufficient for the project's seed files. */
    private static String[] parseCsvLine(String line) throws IOException {
        String[] values = new String[line.length() + 1];
        int valueCount = 0;
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values[valueCount++] = value.toString();
                value.setLength(0);
            } else {
                value.append(character);
            }
        }

        if (quoted) {
            throw new IOException("Unclosed quoted field in CSV row: " + line);
        }
        values[valueCount++] = value.toString();

        String[] result = new String[valueCount];
        for (int index = 0; index < valueCount; index++) {
            result[index] = values[index];
        }
        return result;
    }

    @FunctionalInterface
    private interface CsvRowConsumer {
        void accept(String[] row) throws SQLException;
    }
}
