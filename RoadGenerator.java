import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RoadGenerator {
    public static void main(String[] args) {
        // The three index numbers driving the algorithm parameters
        long randomSeed = 22040372L;
        int speedModifier = 7; // Extracted from 22121287
        int congestionPenalty = 2; // Extracted from 22013390 (mapped +2)

        List<Location> locations = new ArrayList<>();
        Random rand = new Random(randomSeed);

        // 1. Read the 50 locations
        try (BufferedReader br = new BufferedReader(new FileReader("data/locations.csv"))) {
            String line;
            br.readLine(); // Skip the header row
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    locations.add(new Location(parts[0], Double.parseDouble(parts[4]), Double.parseDouble(parts[5])));
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading locations: " + e.getMessage());
            return;
        }

        // 2. Generate 95 random edges to complete the 100 required roads
        // The 'true' parameter in FileWriter turns on append mode
        try (PrintWriter pw = new PrintWriter(new FileWriter("data/roads.csv", true))) { 
            int generated = 0;
            
            while (generated < 95) {
                Location locA = locations.get(rand.nextInt(locations.size()));
                Location locB = locations.get(rand.nextInt(locations.size()));
                
                // Prevent routing a location to itself
                if (locA.id.equals(locB.id)) continue;

                // Calculate exact Euclidean distance
                double distance = Math.sqrt(Math.pow(locB.x - locA.x, 2) + Math.pow(locB.y - locA.y, 2));
                
                // Travel time = distance / speedModifier (minimum of 1)
                int travelTime = Math.max(1, (int) (distance / speedModifier));
                
                // Traffic weight = base route condition + congestion penalty
                int baseWeight = rand.nextInt(2) + 1; // Produces 1 or 2
                int weight = baseWeight + rand.nextInt(congestionPenalty + 1);

                // Write the mathematical edge directly to the CSV
                pw.printf("%s,%s,%.2f,%d,%d\n", locA.id, locB.id, distance, travelTime, weight);
                generated++;
            }
            System.out.println("Successfully appended 95 mathematically sound roads to roads.csv!");
        } catch (Exception e) {
            System.out.println("Error writing roads: " + e.getMessage());
        }
    }

    // Helper class to store the location coordinates in memory
    static class Location {
        String id;
        double x, y;
        Location(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}