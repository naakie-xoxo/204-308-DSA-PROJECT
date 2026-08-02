package ug.edu.ugmc.optimizer.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** Deterministically generates 100 unique, connected, undirected hospital roads. */
public final class RoadGenerator {
    // Three reproducibility parameters derived from team index numbers.
    private static final long RANDOM_SEED = 22040372L;
    private static final int SPEED_MODIFIER = 7; // Derived from 22121287.
    private static final int CONGESTION_PENALTY = 2; // Derived from 22013390.
    private static final int REQUIRED_ROADS = 100;

    private RoadGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path locationsPath = args.length > 0 ? Path.of(args[0]) : Path.of("data", "locations.csv");
        Path roadsPath = args.length > 1 ? Path.of(args[1]) : Path.of("data", "roads.csv");
        generate(locationsPath, roadsPath);
        System.out.println("Generated " + REQUIRED_ROADS + " roads at " + roadsPath.toAbsolutePath());
    }

    public static void generate(Path locationsPath, Path roadsPath) throws IOException {
        List<Location> locations = readLocations(locationsPath);
        if (locations.size() < 2) {
            throw new IOException("At least two locations are required to generate roads.");
        }

        Random random = new Random(RANDOM_SEED);
        Set<String> pairs = new HashSet<>();
        List<Road> roads = new ArrayList<>();

        // A ring guarantees that all locations are connected in the undirected graph.
        for (int index = 0; index < locations.size(); index++) {
            addRoad(roads, pairs, locations.get(index),
                    locations.get((index + 1) % locations.size()), random);
        }

        while (roads.size() < REQUIRED_ROADS) {
            Location first = locations.get(random.nextInt(locations.size()));
            Location second = locations.get(random.nextInt(locations.size()));
            addRoad(roads, pairs, first, second, random);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(roadsPath, StandardCharsets.UTF_8)) {
            writer.write("fromLocationId,toLocationId,distance,travelTime,roadConditionWeight");
            writer.newLine();
            for (Road road : roads) {
                writer.write(String.format(Locale.ROOT, "%s,%s,%.2f,%d,%d",
                        road.fromId(), road.toId(), road.distance(),
                        road.travelTime(), road.conditionWeight()));
                writer.newLine();
            }
        }
    }

    private static void addRoad(List<Road> roads, Set<String> pairs,
                                Location first, Location second, Random random) {
        if (first.id().equals(second.id())) {
            return;
        }

        Location from = first.id().compareTo(second.id()) < 0 ? first : second;
        Location to = from == first ? second : first;
        String pair = from.id() + "|" + to.id();
        if (!pairs.add(pair)) {
            return;
        }

        double distance = Math.hypot(to.x() - from.x(), to.y() - from.y());
        int travelTime = Math.max(1, (int) (distance / SPEED_MODIFIER));
        int baseWeight = random.nextInt(2) + 1;
        int conditionWeight = baseWeight + random.nextInt(CONGESTION_PENALTY + 1);
        roads.add(new Road(from.id(), to.id(), distance, travelTime, conditionWeight));
    }

    private static List<Location> readLocations(Path path) throws IOException {
        List<Location> locations = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!"locationId,name,area,type,xCoordinate,yCoordinate".equals(header)) {
                throw new IOException("Unexpected locations.csv header: " + header);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                if (values.length != 6) {
                    throw new IOException("Invalid location row: " + line);
                }
                locations.add(new Location(values[0],
                        Double.parseDouble(values[4]), Double.parseDouble(values[5])));
            }
        }
        return locations;
    }

    private record Location(String id, double x, double y) {
    }

    private record Road(String fromId, String toId, double distance,
                        int travelTime, int conditionWeight) {
    }
}
