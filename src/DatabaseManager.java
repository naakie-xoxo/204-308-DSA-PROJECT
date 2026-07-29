import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.FileReader;

public class DatabaseManager {
    
    private static final String DB_URL = "jdbc:sqlite:hospital_system.db";

    public static void main(String[] args) {
        System.out.println("Starting UGMC Data Import...");
        
        setupTables();
        loadLocations();
        loadRoads();
        loadResources();
        loadServiceRequests();
        loadAlgorithmRuns();
        
        System.out.println("Import Complete! The database is ready for algorithms.");
    }

    public static void setupTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Create Locations Table
            stmt.execute("CREATE TABLE IF NOT EXISTS locations (" +
                    "locationId TEXT PRIMARY KEY, name TEXT, area TEXT, " +
                    "type TEXT, xCoordinate REAL, yCoordinate REAL)");

            // Create Roads Table
            stmt.execute("CREATE TABLE IF NOT EXISTS roads (" +
                    "fromLocationId TEXT, toLocationId TEXT, distance REAL, " +
                    "travelTime INTEGER, roadConditionWeight INTEGER)");

            // Create Resources Table
            stmt.execute("CREATE TABLE IF NOT EXISTS resources (" +
                    "resourceId TEXT PRIMARY KEY, type TEXT, homeLocation TEXT, " +
                    "capacity INTEGER, availabilityStatus TEXT)");

            // Create Service Requests Table
            stmt.execute("CREATE TABLE IF NOT EXISTS service_requests (" +
                    "requestId TEXT PRIMARY KEY, sourceId TEXT, destinationId TEXT, " +
                    "category TEXT, urgency INTEGER, timeSubmitted TEXT, " +
                    "deadline TEXT, status TEXT)");

            // Create Algorithm Runs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS algorithm_runs (" +
                    "runId INTEGER PRIMARY KEY, algorithmName TEXT, inputSize INTEGER, " +
                    "timeNs BIGINT, memoryKb INTEGER, dateRun TEXT)");
                    
            System.out.println("Tables verified and ready.");
            
        } catch (Exception e) {
            System.out.println("Error setting up tables: " + e.getMessage());
        }
    }

    public static void loadLocations() {
        String sql = "INSERT OR IGNORE INTO locations (locationId, name, area, type, xCoordinate, yCoordinate) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("data/locations.csv"))) {
            
            String line;
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 6) {
                    pstmt.setString(1, data[0]);
                    pstmt.setString(2, data[1]);
                    pstmt.setString(3, data[2]);
                    pstmt.setString(4, data[3]);
                    pstmt.setDouble(5, Double.parseDouble(data[4]));
                    pstmt.setDouble(6, Double.parseDouble(data[5]));
                    pstmt.executeUpdate();
                }
            }
            System.out.println("Locations loaded.");
        } catch (Exception e) { System.out.println("Error loading locations: " + e.getMessage()); }
    }

    public static void loadRoads() {
        String sql = "INSERT OR IGNORE INTO roads (fromLocationId, toLocationId, distance, travelTime, roadConditionWeight) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("data/roads.csv"))) {
            
            String line;
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 5) {
                    pstmt.setString(1, data[0]);
                    pstmt.setString(2, data[1]);
                    pstmt.setDouble(3, Double.parseDouble(data[2]));
                    pstmt.setInt(4, Integer.parseInt(data[3]));
                    pstmt.setInt(5, Integer.parseInt(data[4]));
                    pstmt.executeUpdate();
                }
            }
            System.out.println("Roads loaded.");
        } catch (Exception e) { System.out.println("Error loading roads: " + e.getMessage()); }
    }

    public static void loadResources() {
        String sql = "INSERT OR IGNORE INTO resources (resourceId, type, homeLocation, capacity, availabilityStatus) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("data/resources.csv"))) {
            
            String line;
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 5) {
                    pstmt.setString(1, data[0]);
                    pstmt.setString(2, data[1]);
                    pstmt.setString(3, data[2]);
                    pstmt.setInt(4, Integer.parseInt(data[3]));
                    pstmt.setString(5, data[4]);
                    pstmt.executeUpdate();
                }
            }
            System.out.println("Resources loaded.");
        } catch (Exception e) { System.out.println("Error loading resources: " + e.getMessage()); }
    }

    public static void loadServiceRequests() {
        String sql = "INSERT OR IGNORE INTO service_requests (requestId, sourceId, destinationId, category, urgency, timeSubmitted, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("data/service_requests.csv"))) {
            
            String line;
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 8) {
                    pstmt.setString(1, data[0]);
                    pstmt.setString(2, data[1]);
                    pstmt.setString(3, data[2]);
                    pstmt.setString(4, data[3]);
                    pstmt.setInt(5, Integer.parseInt(data[4]));
                    pstmt.setString(6, data[5]);
                    pstmt.setString(7, data[6]);
                    pstmt.setString(8, data[7]);
                    pstmt.executeUpdate();
                }
            }
            System.out.println("Service requests loaded.");
        } catch (Exception e) { System.out.println("Error loading service requests: " + e.getMessage()); }
    }

    public static void loadAlgorithmRuns() {
        String sql = "INSERT OR IGNORE INTO algorithm_runs (runId, algorithmName, inputSize, timeNs, memoryKb, dateRun) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("data/algorithm_runs.csv"))) {
            
            String line;
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 6) {
                    pstmt.setInt(1, Integer.parseInt(data[0]));
                    pstmt.setString(2, data[1]);
                    pstmt.setInt(3, Integer.parseInt(data[2]));
                    pstmt.setLong(4, Long.parseLong(data[3]));
                    pstmt.setInt(5, Integer.parseInt(data[4]));
                    pstmt.setString(6, data[5]);
                    pstmt.executeUpdate();
                }
            }
            System.out.println("Algorithm runs loaded.");
        } catch (Exception e) { System.out.println("Error loading algorithm runs: " + e.getMessage()); }
    }
}