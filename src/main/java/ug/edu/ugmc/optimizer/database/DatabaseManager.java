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
import java.util.ArrayList;
import java.util.List;

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
                    (locationId, name, area, type, xCoordinate, yCoordinate)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(locationId) DO UPDATE SET
                    name=excluded.name, area=excluded.area, type=excluded.type,
                    xCoordinate=excluded.xCoordinate, yCoordinate=excluded.yCoordinate
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] row : readCsv(csvPath,
                    "locationId,name,area,type,xCoordinate,yCoordinate", 6)) {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.setString(4, row[3]);
                statement.setDouble(5, Double.parseDouble(row[4]));
                statement.setDouble(6, Double.parseDouble(row[5]));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void loadRoads(Connection connection, Path csvPath) throws IOException, SQLException {
        String sql = """
                INSERT INTO roads
                    (fromLocationId, toLocationId, distance, travelTime, roadConditionWeight)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(fromLocationId, toLocationId) DO UPDATE SET
                    distance=excluded.distance, travelTime=excluded.travelTime,
                    roadConditionWeight=excluded.roadConditionWeight
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] row : readCsv(csvPath,
                    "fromLocationId,toLocationId,distance,travelTime,roadConditionWeight", 5)) {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setDouble(3, Double.parseDouble(row[2]));
                statement.setInt(4, Integer.parseInt(row[3]));
                statement.setInt(5, Integer.parseInt(row[4]));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void loadResources(Connection connection, Path csvPath) throws IOException, SQLException {
        String sql = """
                INSERT INTO resources
                    (resourceId, type, homeLocation, capacity, availabilityStatus)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(resourceId) DO UPDATE SET
                    type=excluded.type, homeLocation=excluded.homeLocation,
                    capacity=excluded.capacity, availabilityStatus=excluded.availabilityStatus
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] row : readCsv(csvPath,
                    "resourceId,type,homeLocation,capacity,availabilityStatus", 5)) {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.setInt(4, Integer.parseInt(row[3]));
                statement.setString(5, row[4]);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void loadServiceRequests(Connection connection, Path csvPath)
            throws IOException, SQLException {
        String sql = """
                INSERT INTO service_requests
                    (requestId, sourceId, destinationId, category, urgency,
                     timeSubmitted, deadline, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(requestId) DO UPDATE SET
                    sourceId=excluded.sourceId, destinationId=excluded.destinationId,
                    category=excluded.category, urgency=excluded.urgency,
                    timeSubmitted=excluded.timeSubmitted, deadline=excluded.deadline,
                    status=excluded.status
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] row : readCsv(csvPath,
                    "requestId,sourceId,destinationId,category,urgency,timeSubmitted,deadline,status", 8)) {
                statement.setString(1, row[0]);
                statement.setString(2, row[1]);
                statement.setString(3, row[2]);
                statement.setString(4, row[3]);
                statement.setInt(5, Integer.parseInt(row[4]));
                statement.setString(6, row[5]);
                statement.setString(7, row[6]);
                statement.setString(8, row[7]);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void loadAlgorithmRuns(Connection connection, Path csvPath)
            throws IOException, SQLException {
        String sql = """
                INSERT INTO algorithm_runs
                    (runId, algorithmName, inputSize, timeNs, memoryKb, dateRun)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(runId) DO UPDATE SET
                    algorithmName=excluded.algorithmName, inputSize=excluded.inputSize,
                    timeNs=excluded.timeNs, memoryKb=excluded.memoryKb,
                    dateRun=excluded.dateRun
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] row : readCsv(csvPath,
                    "runId,algorithmName,inputSize,timeNs,memoryKb,dateRun", 6)) {
                statement.setInt(1, Integer.parseInt(row[0]));
                statement.setString(2, row[1]);
                statement.setInt(3, Integer.parseInt(row[2]));
                statement.setLong(4, Long.parseLong(row[3]));
                statement.setLong(5, Long.parseLong(row[4]));
                statement.setString(6, row[5]);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static List<String[]> readCsv(Path path, String expectedHeader, int expectedColumns)
            throws IOException {
        List<String[]> rows = new ArrayList<>();
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
                rows.add(row);
            }
        }
        return rows;
    }

    /** Small RFC-4180-compatible parser sufficient for the project's seed files. */
    private static String[] parseCsvLine(String line) throws IOException {
        List<String> values = new ArrayList<>();
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
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }

        if (quoted) {
            throw new IOException("Unclosed quoted field in CSV row: " + line);
        }
        values.add(value.toString());
        return values.toArray(String[]::new);
    }
}
