import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequestGenerator {
    public static void main(String[] args) {
        // Seed parameter derived from team index number (22013390)
        long randomSeed = 22013390L; 
        Random rand = new Random(randomSeed);

        List<Location> clinicalAreas = new ArrayList<>();   // Wards, OPD, Clinics, Emergency
        List<Location> diagnostics = new ArrayList<>();      // Labs, Imaging, MRI, CT
        List<Location> criticalUnits = new ArrayList<>();     // ICU, HDU, Theatres, NICU
        List<Location> supportUnits = new ArrayList<>();      // Pharmacy, CSSD, Laundry, Stores

        // 1. Read and Categorize Locations into Logical Operational Buckets
        try (BufferedReader br = new BufferedReader(new FileReader("data/locations.csv"))) {
            String line;
            br.readLine(); // Skip CSV header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    Location loc = new Location(parts[0], parts[1], parts[3]);
                    String type = loc.type.toLowerCase();
                    
                    if (type.contains("critical") || type.contains("surgery") || type.contains("recovery")) {
                        criticalUnits.add(loc);
                    } else if (type.contains("diagnostic")) {
                        diagnostics.add(loc);
                    } else if (type.contains("support") || type.contains("facility") || type.contains("office")) {
                        supportUnits.add(loc);
                    } else {
                        // Wards, Clinics, Emergency, Maternity, Therapy
                        clinicalAreas.add(loc);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading locations: " + e.getMessage());
            return;
        }

        // 2. Generate 300 Context-Aware Service Requests
        try (PrintWriter pw = new PrintWriter(new FileWriter("data/service_requests.csv"))) {
            pw.println("requestId,sourceId,destinationId,category,urgency,timeSubmitted,deadline,status");
            
            for (int i = 1; i <= 300; i++) {
                String reqId = String.format("REQ-%03d", i);
                Location source;
                Location dest;
                String category;
                int urgency;
                
                int taskScenario = rand.nextInt(4);
                
                if (taskScenario == 0) {
                    // Scenario A: Lab Sample Transport (Clinical -> Diagnostic)
                    source = clinicalAreas.get(rand.nextInt(clinicalAreas.size()));
                    dest = diagnostics.get(rand.nextInt(diagnostics.size()));
                    category = "Lab Sample Transport";
                    urgency = rand.nextInt(3) + 2; // 2 to 4
                    
                } else if (taskScenario == 1) {
                    // Scenario B: Critical Patient Transfer (Clinical/Emergency -> Critical Unit)
                    source = clinicalAreas.get(rand.nextInt(clinicalAreas.size()));
                    dest = criticalUnits.get(rand.nextInt(criticalUnits.size()));
                    category = "Critical Patient Transfer";
                    urgency = rand.nextInt(2) + 4; // 4 to 5 (High Priority)
                    
                } else if (taskScenario == 2) {
                    // Scenario C: Pharmaceutical & Sterile Supply (Support -> Clinical/Critical)
                    source = supportUnits.get(rand.nextInt(supportUnits.size()));
                    dest = rand.nextBoolean() ? clinicalAreas.get(rand.nextInt(clinicalAreas.size())) 
                                             : criticalUnits.get(rand.nextInt(criticalUnits.size()));
                    category = "Medical Supply Dispatch";
                    urgency = rand.nextInt(3) + 1; // 1 to 3
                    
                } else {
                    // Scenario D: Ward Maintenance & Sanitation (Support -> Wards/Clinics)
                    source = supportUnits.get(rand.nextInt(supportUnits.size()));
                    dest = clinicalAreas.get(rand.nextInt(clinicalAreas.size()));
                    category = "Ward Maintenance";
                    urgency = rand.nextInt(2) + 1; // 1 to 2 (Low Priority)
                }

                // Fallback check: Ensure source and destination are never identical
                if (source.id.equals(dest.id)) {
                    dest = clinicalAreas.get((clinicalAreas.indexOf(source) + 1) % clinicalAreas.size());
                }

                // Generate 24-hour timestamp (00:00 to 23:59)
                int startHour = rand.nextInt(24);
                int startMin = rand.nextInt(60);
                String timeSubmitted = String.format("%02d:%02d", startHour, startMin);
                
                // Deadline window is inversely proportional to urgency:
                // Urgency 5 = 20-minute deadline window
                // Urgency 1 = 100-minute deadline window
                int durationLimit = (6 - urgency) * 20; 
                int totalEndMin = startMin + durationLimit;
                int endHour = (startHour + (totalEndMin / 60)) % 24; // Properly handles midnight wraparound
                int endMin = totalEndMin % 60;
                String deadline = String.format("%02d:%02d", endHour, endMin);

                pw.printf("%s,%s,%s,%s,%d,%s,%s,%s\n", 
                    reqId, source.id, dest.id, category, urgency, timeSubmitted, deadline, "Pending");
            }
            
            System.out.println("Success! 300 operationally verified service requests generated in data/service_requests.csv.");
            
        } catch (Exception e) {
            System.out.println("Error writing requests: " + e.getMessage());
        }
    }

    static class Location {
        String id, name, type;
        Location(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }
}