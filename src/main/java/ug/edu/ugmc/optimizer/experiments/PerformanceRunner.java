package ug.edu.ugmc.optimizer.experiments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import ug.edu.ugmc.optimizer.algorithms.graph.GraphTraversal;
import ug.edu.ugmc.optimizer.algorithms.graph.PathFinder;
import ug.edu.ugmc.optimizer.algorithms.search.CustomSearch;
import ug.edu.ugmc.optimizer.algorithms.sort.InsertionSort;
import ug.edu.ugmc.optimizer.algorithms.sort.MergeSort;
import ug.edu.ugmc.optimizer.algorithms.sort.QuickSort;
import ug.edu.ugmc.optimizer.algorithms.sort.SelectionSort;
import ug.edu.ugmc.optimizer.database.DatabaseManager;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/**
 * Reproducible empirical-efficiency lab for the UGMC optimizer.
 *
 * <p>SQLite is read and every custom structure is populated before timing.
 * Timed regions contain only the algorithm call and O(1) result capture. CSV
 * output and console reporting happen after all timers have stopped.</p>
 */
public final class PerformanceRunner {

    private static final int[] DATA_SCALES = {100, 500, 1_000, 5_000, 10_000, 50_000};
    private static final int WARM_UP_SIZE = 100;
    private static final int MAX_GRAPH_LOCATIONS = 500;
    private static final int DEFAULT_TRIALS = 3;
    private static final int SORT_ALGORITHM_COUNT = 4;
    private static final int SEARCH_ALGORITHM_COUNT = 2;
    private static final int GRAPH_ALGORITHM_COUNT = 2;

    private static final String REQUEST_SQL = """
            SELECT request_id, urgency_level, weight, value
            FROM service_requests
            ORDER BY request_id
            """;

    private static final String ROAD_WEIGHT_SQL = """
            SELECT travel_time
            FROM roads
            ORDER BY source_id, destination_id
            """;

    private static volatile long resultSink;

    private final DynamicArray<ServiceRequest> requestSeeds;
    private final DynamicArray<Integer> roadWeightSeeds;
    private final int trialCount;
    private final DynamicArray<RunRecord> rawRuns;

    private PerformanceRunner(
            DynamicArray<ServiceRequest> requestSeeds,
            DynamicArray<Integer> roadWeightSeeds,
            int trialCount) {
        if (requestSeeds == null || requestSeeds.size() == 0) {
            throw new IllegalArgumentException("At least one SQLite service request is required.");
        }
        if (roadWeightSeeds == null || roadWeightSeeds.size() == 0) {
            throw new IllegalArgumentException("At least one SQLite road is required.");
        }
        if (trialCount <= 0) {
            throw new IllegalArgumentException("Trial count must be greater than zero.");
        }

        this.requestSeeds = requestSeeds;
        this.roadWeightSeeds = roadWeightSeeds;
        this.trialCount = trialCount;
        this.rawRuns = new DynamicArray<>(DATA_SCALES.length * 8 * trialCount);
    }

    /**
     * Runs the complete lab against a local SQLite database.
     *
     * <p>Arguments: database path, results directory, and trial count. Defaults
     * are {@code hospital_system.db}, {@code results}, and three trials.</p>
     */
    public static void main(String[] args) throws Exception {
        Path databasePath = args.length > 0 ? Path.of(args[0]) : Path.of("hospital_system.db");
        Path resultsDirectory = args.length > 1 ? Path.of(args[1]) : Path.of("results");
        int trials = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_TRIALS;

        if (!Files.exists(databasePath)) {
            DatabaseManager.initializeDatabase(databasePath, Path.of("schema.sql"), Path.of("data"));
        }

        PerformanceRunner runner = fromDatabase(
                "jdbc:sqlite:" + databasePath.toAbsolutePath().normalize(), trials);
        runner.run(resultsDirectory, Path.of("data", "algorithm_runs.csv"));
    }

    /** Returns a defensive copy of the exact assessed benchmark scales. */
    public static int[] getDataScales() {
        int[] copy = new int[DATA_SCALES.length];
        for (int index = 0; index < DATA_SCALES.length; index++) {
            copy[index] = DATA_SCALES[index];
        }
        return copy;
    }

    /** Loads canonical seed values from SQLite before any measurement begins. */
    public static PerformanceRunner fromDatabase(String connectionString, int trials)
            throws SQLException {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("SQLite connection string cannot be blank.");
        }

        DynamicArray<ServiceRequest> requests = new DynamicArray<>(500);
        DynamicArray<Integer> roadWeights = new DynamicArray<>(128);

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            try (PreparedStatement statement = connection.prepareStatement(REQUEST_SQL);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    requests.insert(new ServiceRequest(
                            rows.getString("request_id"),
                            rows.getInt("urgency_level"),
                            rows.getInt("weight"),
                            rows.getInt("value")));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(ROAD_WEIGHT_SQL);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    roadWeights.insert(rows.getInt("travel_time"));
                }
            }
        }

        return new PerformanceRunner(requests, roadWeights, trials);
    }

    /** Executes warm-up, all trials, and final evidence export. */
    public void run(Path resultsDirectory, Path canonicalAlgorithmRuns) throws Exception {
        if (resultsDirectory == null || canonicalAlgorithmRuns == null) {
            throw new IllegalArgumentException("Output paths cannot be null.");
        }

        Files.createDirectories(resultsDirectory);
        Path canonicalParent = canonicalAlgorithmRuns.toAbsolutePath().normalize().getParent();
        if (canonicalParent != null) {
            Files.createDirectories(canonicalParent);
        }

        warmUpJvm();
        System.out.println("JVM warm-up complete (100 records, not recorded).");

        BenchmarkSummary summary = new BenchmarkSummary(DATA_SCALES.length);
        for (int scaleIndex = 0; scaleIndex < DATA_SCALES.length; scaleIndex++) {
            int scale = DATA_SCALES[scaleIndex];
            benchmarkSorts(scaleIndex, scale, summary);
            benchmarkSearches(scaleIndex, scale, summary);
            benchmarkGraph(scaleIndex, scale, summary);
            System.out.println("Completed measured scale: " + scale);
        }

        writeRawCsv(resultsDirectory.resolve("benchmark_raw.csv"));
        writeSortSummary(resultsDirectory.resolve("sort_benchmarks.csv"), summary);
        writeSearchSummary(resultsDirectory.resolve("search_benchmarks.csv"), summary);
        writeGraphSummary(resultsDirectory.resolve("graph_benchmarks.csv"), summary);
        writeMetadata(resultsDirectory.resolve("benchmark_metadata.csv"));
        writeSvgPlot(
                resultsDirectory.resolve("sort_runtimes.svg"),
                "UGMC Triage Sorting Runtime",
                new String[] {"Merge Sort", "Quick Sort", "Selection Sort", "Insertion Sort"},
                new String[] {"#1565c0", "#00897b", "#ef6c00", "#c62828"},
                summary.sortTimes,
                DATA_SCALES);
        writeSvgPlot(
                resultsDirectory.resolve("search_runtimes.svg"),
                "UGMC Patient ID Lookup Runtime",
                new String[] {"Linear Search", "Binary Search"},
                new String[] {"#6a1b9a", "#2e7d32"},
                summary.searchTimes,
                DATA_SCALES);
        writeSvgPlot(
                resultsDirectory.resolve("graph_runtimes.svg"),
                "UGMC Routing Runtime by Road Records",
                new String[] {"Dijkstra", "BFS"},
                new String[] {"#283593", "#ad1457"},
                summary.graphTimes,
                DATA_SCALES);
        writeCanonicalAlgorithmRuns(canonicalAlgorithmRuns);

        System.out.println("Actual benchmark CSVs written to "
                + resultsDirectory.toAbsolutePath().normalize());
        System.out.println("Canonical algorithm runs replaced at "
                + canonicalAlgorithmRuns.toAbsolutePath().normalize());
    }

    private void warmUpJvm() throws Exception {
        DynamicArray<Integer> sortSeed = buildSortData(WARM_UP_SIZE);
        DynamicArray<Integer> mergeInput = copyOf(sortSeed);
        DynamicArray<Integer> quickInput = copyOf(sortSeed);
        DynamicArray<Integer> insertionInput = copyOf(sortSeed);
        DynamicArray<Integer> selectionInput = copyOf(sortSeed);

        MergeSort.sort(mergeInput);
        QuickSort.sort(quickInput);
        InsertionSort.sort(insertionInput);
        SelectionSort.sort(selectionInput);

        DynamicArray<Integer> searchData = buildSearchData(WARM_UP_SIZE);
        resultSink ^= CustomSearch.linearSearch(searchData, WARM_UP_SIZE - 1);
        resultSink ^= CustomSearch.binarySearchPresorted(searchData, WARM_UP_SIZE - 1);

        GraphFixture graphFixture = buildGraph(WARM_UP_SIZE);
        resultSink ^= PathFinder.dijkstra(
                graphFixture.graph, graphFixture.startNode, graphFixture.targetNode);
        resultSink ^= GraphTraversal.bfsTraversal(graphFixture.graph, graphFixture.startNode).size();

        requireSorted(mergeInput, "MergeSort warm-up");
        requireSorted(quickInput, "QuickSort warm-up");
        requireSorted(insertionInput, "InsertionSort warm-up");
        requireSorted(selectionInput, "SelectionSort warm-up");
    }

    private void benchmarkSorts(int scaleIndex, int scale, BenchmarkSummary summary)
            throws Exception {
        long[] mergeTimes = new long[trialCount];
        long[] quickTimes = new long[trialCount];
        long[] selectionTimes = new long[trialCount];
        long[] insertionTimes = new long[trialCount];
        long latestPenaltyCost = 0;
        long latestPenaltyApplications = 0;
        long latestFallbacks = 0;

        DynamicArray<Integer> source = buildSortData(scale);

        for (int trial = 1; trial <= trialCount; trial++) {
            DynamicArray<Integer> input = copyOf(source);
            DynamicArray<Integer> mergeInput = input;
            Measurement measurement = measure(() -> {
                MergeSort.sort(mergeInput);
                return endpointChecksum(mergeInput);
            });
            requireSorted(mergeInput, "MergeSort");
            mergeTimes[trial - 1] = measurement.timeNs;
            addRun("Sort", "MergeSort", scale, trial, measurement, "", 0, 0);

            input = copyOf(source);
            DynamicArray<Integer> quickInput = input;
            measurement = measure(() -> {
                QuickSort.sort(quickInput);
                return endpointChecksum(quickInput);
            });
            requireSorted(quickInput, "QuickSort");
            latestFallbacks = QuickSort.getInsertionFallbackCount();
            if (latestFallbacks <= 0) {
                throw new IllegalStateException("QuickSort cutoff did not trigger.");
            }
            quickTimes[trial - 1] = measurement.timeNs;
            addRun(
                    "Sort", "QuickSort", scale, trial, measurement,
                    "subarray_cutoff", QuickSort.getSubarrayCutoff(), latestFallbacks);

            input = copyOf(source);
            DynamicArray<Integer> selectionInput = input;
            measurement = measure(() -> {
                SelectionSort.sort(selectionInput);
                return endpointChecksum(selectionInput);
            });
            requireSorted(selectionInput, "SelectionSort");
            selectionTimes[trial - 1] = measurement.timeNs;
            addRun(
                    "Sort", "SelectionSort", scale, trial, measurement,
                    "comparisons", SelectionSort.getComparisons(), SelectionSort.getSwaps());

            input = copyOf(source);
            DynamicArray<Integer> insertionInput = input;
            measurement = measure(() -> {
                InsertionSort.sort(insertionInput);
                return endpointChecksum(insertionInput)
                        ^ InsertionSort.getShiftPenaltyAccumulator();
            });
            requireSorted(insertionInput, "InsertionSort");
            latestPenaltyCost = InsertionSort.getWeightedShiftCost();
            latestPenaltyApplications = InsertionSort.getShiftPenaltyApplications();
            if (latestPenaltyApplications <= 0 || latestPenaltyCost <= 0) {
                throw new IllegalStateException("InsertionSort shift penalty did not trigger.");
            }
            insertionTimes[trial - 1] = measurement.timeNs;
            addRun(
                    "Sort", "InsertionSort", scale, trial, measurement,
                    "weighted_shift_penalty", latestPenaltyCost, latestPenaltyApplications);
        }

        summary.sortTimes[scaleIndex][0] = median(mergeTimes);
        summary.sortTimes[scaleIndex][1] = median(quickTimes);
        summary.sortTimes[scaleIndex][2] = median(selectionTimes);
        summary.sortTimes[scaleIndex][3] = median(insertionTimes);
        summary.insertionPenaltyCost[scaleIndex] = latestPenaltyCost;
        summary.insertionPenaltyApplications[scaleIndex] = latestPenaltyApplications;
        summary.quickSortFallbacks[scaleIndex] = latestFallbacks;
    }

    private void benchmarkSearches(int scaleIndex, int scale, BenchmarkSummary summary)
            throws Exception {
        long[] linearTimes = new long[trialCount];
        long[] binaryTimes = new long[trialCount];
        DynamicArray<Integer> sortedIds = buildSearchData(scale);
        requireSorted(sortedIds, "Search input");
        int target = scale - 1;

        for (int trial = 1; trial <= trialCount; trial++) {
            Measurement linear = measure(() -> CustomSearch.linearSearch(sortedIds, target));
            if (linear.result != target) {
                throw new IllegalStateException("LinearSearch returned the wrong patient index.");
            }
            linearTimes[trial - 1] = linear.timeNs;
            addRun("Search", "LinearSearch", scale, trial, linear, "target_id", target, 0);

            Measurement binary = measure(
                    () -> CustomSearch.binarySearchPresorted(sortedIds, target));
            if (binary.result != target) {
                throw new IllegalStateException("BinarySearch returned the wrong patient index.");
            }
            binaryTimes[trial - 1] = binary.timeNs;
            addRun("Search", "BinarySearch", scale, trial, binary, "target_id", target, 0);
        }

        summary.searchTimes[scaleIndex][0] = median(linearTimes);
        summary.searchTimes[scaleIndex][1] = median(binaryTimes);
    }

    private void benchmarkGraph(int scaleIndex, int roadRecordCount, BenchmarkSummary summary)
            throws Exception {
        long[] dijkstraTimes = new long[trialCount];
        long[] bfsTimes = new long[trialCount];
        GraphFixture fixture = buildGraph(roadRecordCount);

        for (int trial = 1; trial <= trialCount; trial++) {
            Measurement dijkstra = measure(() -> PathFinder.dijkstra(
                    fixture.graph, fixture.startNode, fixture.targetNode));
            if (dijkstra.result < 0) {
                throw new IllegalStateException("Dijkstra did not reach the benchmark target.");
            }
            dijkstraTimes[trial - 1] = dijkstra.timeNs;
            addRun(
                    "Graph", "Dijkstra", roadRecordCount, trial, dijkstra,
                    "locations", fixture.locationCount, 0);

            Measurement bfs = measure(() ->
                    GraphTraversal.bfsTraversal(fixture.graph, fixture.startNode).size());
            if (bfs.result != fixture.locationCount) {
                throw new IllegalStateException("BFS did not traverse the connected benchmark graph.");
            }
            bfsTimes[trial - 1] = bfs.timeNs;
            addRun(
                    "Graph", "BFS", roadRecordCount, trial, bfs,
                    "locations", fixture.locationCount, 0);
        }

        summary.graphTimes[scaleIndex][0] = median(dijkstraTimes);
        summary.graphTimes[scaleIndex][1] = median(bfsTimes);
        summary.graphLocationCounts[scaleIndex] = fixture.locationCount;
    }

    private DynamicArray<Integer> buildSortData(int size) {
        DynamicArray<Integer> values = new DynamicArray<>(size);
        int state = 22040372 ^ size;

        for (int index = 0; index < size; index++) {
            state = state * 1103515245 + 12345;
            int seedIndex = (state & Integer.MAX_VALUE) % requestSeeds.size();
            ServiceRequest request = requestSeeds.get(seedIndex);
            int key = request.getValue() * 100_000
                    + request.getWeight() * 1_000
                    + request.getUrgency() * 100
                    + ((state >>> 16) & 0x7fff);
            values.insert(key);
        }
        return values;
    }

    private static DynamicArray<Integer> buildSearchData(int size) {
        DynamicArray<Integer> ids = new DynamicArray<>(size);
        for (int index = 0; index < size; index++) {
            ids.insert(index);
        }
        return ids;
    }

    private GraphFixture buildGraph(int roadRecordCount) {
        int locationCount = Math.min(MAX_GRAPH_LOCATIONS, roadRecordCount + 1);
        CustomGraph graph = new CustomGraph(locationCount);
        for (int index = 0; index < locationCount; index++) {
            graph.addNode(benchmarkLocationId(index));
        }

        int edge = 0;
        for (; edge < locationCount - 1 && edge < roadRecordCount; edge++) {
            graph.addEdge(
                    benchmarkLocationId(0),
                    benchmarkLocationId(edge + 1),
                    roadWeightSeeds.get(edge % roadWeightSeeds.size()));
        }

        for (int source = 1; source < locationCount && edge < roadRecordCount; source++) {
            for (int destination = source + 1;
                    destination < locationCount && edge < roadRecordCount;
                    destination++) {
                graph.addEdge(
                        benchmarkLocationId(source),
                        benchmarkLocationId(destination),
                        roadWeightSeeds.get(edge % roadWeightSeeds.size()));
                edge++;
            }
        }

        if (edge != roadRecordCount) {
            throw new IllegalStateException(
                    "Not enough unique hospital location pairs for " + roadRecordCount + " roads.");
        }

        return new GraphFixture(
                graph,
                benchmarkLocationId(0),
                benchmarkLocationId(locationCount - 1),
                locationCount);
    }

    private static String benchmarkLocationId(int index) {
        return "BENCH-LOC-" + index;
    }

    private static DynamicArray<Integer> copyOf(DynamicArray<Integer> source) {
        DynamicArray<Integer> copy = new DynamicArray<>(Math.max(1, source.size()));
        for (int index = 0; index < source.size(); index++) {
            copy.insert(source.get(index));
        }
        return copy;
    }

    private static long endpointChecksum(DynamicArray<Integer> values) {
        if (values.size() == 0) {
            return 0;
        }
        return ((long) values.get(0) << 32) ^ values.get(values.size() - 1);
    }

    private static void requireSorted(DynamicArray<Integer> values, String algorithmName) {
        for (int index = 1; index < values.size(); index++) {
            if (values.get(index - 1) > values.get(index)) {
                throw new IllegalStateException(algorithmName + " produced unsorted output.");
            }
        }
    }

    private static Measurement measure(TimedAction action) throws Exception {
        long memoryBefore = usedMemoryKb();
        long startedAt = System.nanoTime();
        long result = action.run();
        long stoppedAt = System.nanoTime();
        long memoryAfter = usedMemoryKb();

        resultSink ^= result;
        return new Measurement(
                stoppedAt - startedAt,
                Math.max(0, memoryAfter - memoryBefore),
                result);
    }

    private static long usedMemoryKb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024;
    }

    private void addRun(
            String category,
            String algorithm,
            int inputSize,
            int trial,
            Measurement measurement,
            String parameterName,
            long parameterValue,
            long parameterApplications) {
        rawRuns.insert(new RunRecord(
                category,
                algorithm,
                inputSize,
                trial,
                measurement.timeNs,
                measurement.memoryKb,
                parameterName,
                parameterValue,
                parameterApplications));
    }

    private void writeRawCsv(Path output) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write("category,algorithm,input_size,trial,time_ns,memory_kb,"
                    + "parameter_name,parameter_value,parameter_applications");
            writer.newLine();
            for (int index = 0; index < rawRuns.size(); index++) {
                RunRecord run = rawRuns.get(index);
                writer.write(run.category + "," + run.algorithm + "," + run.inputSize + ","
                        + run.trial + "," + run.timeNs + "," + run.memoryKb + ","
                        + run.parameterName + "," + run.parameterValue + ","
                        + run.parameterApplications);
                writer.newLine();
            }
        }
    }

    private void writeSortSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write("scale,merge_sort_ns,quick_sort_ns,selection_sort_ns,insertion_sort_ns,"
                    + "insertion_weighted_shift_cost,insertion_penalty_applications,"
                    + "quick_sort_cutoff,quick_sort_fallbacks,trials");
            writer.newLine();
            for (int scaleIndex = 0; scaleIndex < DATA_SCALES.length; scaleIndex++) {
                writer.write(DATA_SCALES[scaleIndex] + ","
                        + summary.sortTimes[scaleIndex][0] + ","
                        + summary.sortTimes[scaleIndex][1] + ","
                        + summary.sortTimes[scaleIndex][2] + ","
                        + summary.sortTimes[scaleIndex][3] + ","
                        + summary.insertionPenaltyCost[scaleIndex] + ","
                        + summary.insertionPenaltyApplications[scaleIndex] + ","
                        + QuickSort.getSubarrayCutoff() + ","
                        + summary.quickSortFallbacks[scaleIndex] + ","
                        + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeSearchSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write("scale,linear_search_ns,binary_search_ns,trials");
            writer.newLine();
            for (int scaleIndex = 0; scaleIndex < DATA_SCALES.length; scaleIndex++) {
                writer.write(DATA_SCALES[scaleIndex] + ","
                        + summary.searchTimes[scaleIndex][0] + ","
                        + summary.searchTimes[scaleIndex][1] + "," + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeGraphSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write("road_records,locations,dijkstra_ns,bfs_ns,trials");
            writer.newLine();
            for (int scaleIndex = 0; scaleIndex < DATA_SCALES.length; scaleIndex++) {
                writer.write(DATA_SCALES[scaleIndex] + ","
                        + summary.graphLocationCounts[scaleIndex] + ","
                        + summary.graphTimes[scaleIndex][0] + ","
                        + summary.graphTimes[scaleIndex][1] + "," + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeCanonicalAlgorithmRuns(Path output) throws IOException {
        String date = LocalDate.now().toString();
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write("runId,algorithmName,inputSize,timeNs,memoryKb,dateRun");
            writer.newLine();
            for (int index = 0; index < rawRuns.size(); index++) {
                RunRecord run = rawRuns.get(index);
                writer.write((index + 1) + "," + run.algorithm + "," + run.inputSize + ","
                        + run.timeNs + "," + run.memoryKb + "," + date);
                writer.newLine();
            }
        }
    }

    private void writeMetadata(Path output) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write("property,value");
            writer.newLine();
            writeMetadataRow(writer, "date_run", LocalDate.now().toString());
            writeMetadataRow(writer, "java_version", System.getProperty("java.version"));
            writeMetadataRow(writer, "java_vm", System.getProperty("java.vm.name"));
            writeMetadataRow(writer, "operating_system", System.getProperty("os.name"));
            writeMetadataRow(writer, "os_architecture", System.getProperty("os.arch"));
            writeMetadataRow(writer, "available_processors",
                    Integer.toString(Runtime.getRuntime().availableProcessors()));
            writeMetadataRow(writer, "maximum_jvm_memory_bytes",
                    Long.toString(Runtime.getRuntime().maxMemory()));
            writeMetadataRow(writer, "recorded_trials_per_algorithm_scale",
                    Integer.toString(trialCount));
            writeMetadataRow(writer, "warm_up_records", Integer.toString(WARM_UP_SIZE));
            writeMetadataRow(writer, "timing_clock", "System.nanoTime");
            writeMetadataRow(writer, "input_scales", "100|500|1000|5000|10000|50000");
            writeMetadataRow(writer, "graph_max_locations",
                    Integer.toString(MAX_GRAPH_LOCATIONS));
            writeMetadataRow(writer, "graph_scale_unit", "unique_undirected_road_records");
            writeMetadataRow(writer, "request_seed_source", "SQLite service_requests");
            writeMetadataRow(writer, "road_weight_seed_source", "SQLite roads.travel_time");
        }
    }

    private static void writeMetadataRow(BufferedWriter writer, String property, String value)
            throws IOException {
        writer.write(property + ",\"" + value.replace("\"", "\"\"") + "\"");
        writer.newLine();
    }

    /** Writes a dependency-free, report-ready SVG from measured median times. */
    private static void writeSvgPlot(
            Path output,
            String title,
            String[] seriesNames,
            String[] colors,
            long[][] values,
            int[] scales) throws IOException {
        final int width = 1_000;
        final int height = 620;
        final int left = 95;
        final int right = 40;
        final int top = 80;
        final int bottom = 90;
        final int plotWidth = width - left - right;
        final int plotHeight = height - top - bottom;

        long maximumNs = 1;
        for (int scaleIndex = 0; scaleIndex < values.length; scaleIndex++) {
            for (int seriesIndex = 0; seriesIndex < values[scaleIndex].length; seriesIndex++) {
                if (values[scaleIndex][seriesIndex] > maximumNs) {
                    maximumNs = values[scaleIndex][seriesIndex];
                }
            }
        }

        double minimumLogScale = Math.log10(scales[0]);
        double maximumLogScale = Math.log10(scales[scales.length - 1]);
        StringBuilder svg = new StringBuilder(12_000);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\">\n")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n")
                .append("<style>text{font-family:Arial,sans-serif;fill:#263238}"
                        + ".grid{stroke:#cfd8dc;stroke-width:1}.axis{stroke:#37474f;stroke-width:2}"
                        + ".label{font-size:13px}.title{font-size:24px;font-weight:bold}"
                        + ".legend{font-size:14px}</style>\n")
                .append("<text class=\"title\" x=\"").append(width / 2)
                .append("\" y=\"38\" text-anchor=\"middle\">").append(title)
                .append("</text>\n");

        for (int tick = 0; tick <= 5; tick++) {
            int y = top + plotHeight - (plotHeight * tick / 5);
            double tickMs = (maximumNs / 1_000_000.0) * tick / 5.0;
            svg.append("<line class=\"grid\" x1=\"").append(left).append("\" y1=\"")
                    .append(y).append("\" x2=\"").append(left + plotWidth)
                    .append("\" y2=\"").append(y).append("\"/>\n")
                    .append("<text class=\"label\" x=\"").append(left - 12)
                    .append("\" y=\"").append(y + 5).append("\" text-anchor=\"end\">")
                    .append(formatMilliseconds(tickMs)).append("</text>\n");
        }

        for (int scaleIndex = 0; scaleIndex < scales.length; scaleIndex++) {
            int x = left + (int) Math.round(
                    (Math.log10(scales[scaleIndex]) - minimumLogScale)
                            / (maximumLogScale - minimumLogScale) * plotWidth);
            svg.append("<line class=\"grid\" x1=\"").append(x).append("\" y1=\"")
                    .append(top).append("\" x2=\"").append(x).append("\" y2=\"")
                    .append(top + plotHeight).append("\"/>\n")
                    .append("<text class=\"label\" x=\"").append(x).append("\" y=\"")
                    .append(top + plotHeight + 25).append("\" text-anchor=\"middle\">")
                    .append(scales[scaleIndex]).append("</text>\n");
        }

        svg.append("<line class=\"axis\" x1=\"").append(left).append("\" y1=\"")
                .append(top).append("\" x2=\"").append(left).append("\" y2=\"")
                .append(top + plotHeight).append("\"/>\n")
                .append("<line class=\"axis\" x1=\"").append(left).append("\" y1=\"")
                .append(top + plotHeight).append("\" x2=\"").append(left + plotWidth)
                .append("\" y2=\"").append(top + plotHeight).append("\"/>\n")
                .append("<text class=\"label\" x=\"").append(left + plotWidth / 2)
                .append("\" y=\"").append(height - 25)
                .append("\" text-anchor=\"middle\">Input scale (logarithmic spacing)</text>\n")
                .append("<text class=\"label\" transform=\"translate(25 ")
                .append(top + plotHeight / 2)
                .append(") rotate(-90)\" text-anchor=\"middle\">Median runtime (ms)</text>\n");

        for (int seriesIndex = 0; seriesIndex < seriesNames.length; seriesIndex++) {
            svg.append("<polyline fill=\"none\" stroke=\"").append(colors[seriesIndex])
                    .append("\" stroke-width=\"3\" points=\"");
            for (int scaleIndex = 0; scaleIndex < scales.length; scaleIndex++) {
                int x = left + (int) Math.round(
                        (Math.log10(scales[scaleIndex]) - minimumLogScale)
                                / (maximumLogScale - minimumLogScale) * plotWidth);
                int y = top + plotHeight - (int) Math.round(
                        (double) values[scaleIndex][seriesIndex] / maximumNs * plotHeight);
                svg.append(x).append(',').append(y).append(' ');
            }
            svg.append("\"/>\n");

            for (int scaleIndex = 0; scaleIndex < scales.length; scaleIndex++) {
                int x = left + (int) Math.round(
                        (Math.log10(scales[scaleIndex]) - minimumLogScale)
                                / (maximumLogScale - minimumLogScale) * plotWidth);
                int y = top + plotHeight - (int) Math.round(
                        (double) values[scaleIndex][seriesIndex] / maximumNs * plotHeight);
                svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                        .append("\" r=\"4\" fill=\"").append(colors[seriesIndex])
                        .append("\"/>\n");
            }

            int legendX = left + seriesIndex * 205;
            svg.append("<line x1=\"").append(legendX).append("\" y1=\"60\" x2=\"")
                    .append(legendX + 28).append("\" y2=\"60\" stroke=\"")
                    .append(colors[seriesIndex]).append("\" stroke-width=\"4\"/>\n")
                    .append("<text class=\"legend\" x=\"").append(legendX + 35)
                    .append("\" y=\"65\">").append(seriesNames[seriesIndex])
                    .append("</text>\n");
        }

        svg.append("</svg>\n");
        Files.writeString(output, svg.toString(), StandardCharsets.UTF_8);
    }

    private static String formatMilliseconds(double value) {
        if (value >= 100) {
            return Long.toString(Math.round(value));
        }
        if (value >= 10) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static long median(long[] values) {
        long[] sorted = new long[values.length];
        for (int index = 0; index < values.length; index++) {
            sorted[index] = values[index];
        }

        for (int index = 1; index < sorted.length; index++) {
            long current = sorted[index];
            int position = index - 1;
            while (position >= 0 && sorted[position] > current) {
                sorted[position + 1] = sorted[position];
                position--;
            }
            sorted[position + 1] = current;
        }

        return sorted[sorted.length / 2];
    }

    @FunctionalInterface
    private interface TimedAction {
        long run() throws Exception;
    }

    private static final class Measurement {
        private final long timeNs;
        private final long memoryKb;
        private final long result;

        private Measurement(long timeNs, long memoryKb, long result) {
            this.timeNs = timeNs;
            this.memoryKb = memoryKb;
            this.result = result;
        }
    }

    private static final class RunRecord {
        private final String category;
        private final String algorithm;
        private final int inputSize;
        private final int trial;
        private final long timeNs;
        private final long memoryKb;
        private final String parameterName;
        private final long parameterValue;
        private final long parameterApplications;

        private RunRecord(
                String category,
                String algorithm,
                int inputSize,
                int trial,
                long timeNs,
                long memoryKb,
                String parameterName,
                long parameterValue,
                long parameterApplications) {
            this.category = category;
            this.algorithm = algorithm;
            this.inputSize = inputSize;
            this.trial = trial;
            this.timeNs = timeNs;
            this.memoryKb = memoryKb;
            this.parameterName = parameterName;
            this.parameterValue = parameterValue;
            this.parameterApplications = parameterApplications;
        }
    }

    private static final class GraphFixture {
        private final CustomGraph graph;
        private final String startNode;
        private final String targetNode;
        private final int locationCount;

        private GraphFixture(
                CustomGraph graph, String startNode, String targetNode, int locationCount) {
            this.graph = graph;
            this.startNode = startNode;
            this.targetNode = targetNode;
            this.locationCount = locationCount;
        }
    }

    private static final class BenchmarkSummary {
        private final long[][] sortTimes;
        private final long[][] searchTimes;
        private final long[][] graphTimes;
        private final long[] insertionPenaltyCost;
        private final long[] insertionPenaltyApplications;
        private final long[] quickSortFallbacks;
        private final int[] graphLocationCounts;

        private BenchmarkSummary(int scaleCount) {
            sortTimes = new long[scaleCount][SORT_ALGORITHM_COUNT];
            searchTimes = new long[scaleCount][SEARCH_ALGORITHM_COUNT];
            graphTimes = new long[scaleCount][GRAPH_ALGORITHM_COUNT];
            insertionPenaltyCost = new long[scaleCount];
            insertionPenaltyApplications = new long[scaleCount];
            quickSortFallbacks = new long[scaleCount];
            graphLocationCounts = new int[scaleCount];
        }
    }
}
