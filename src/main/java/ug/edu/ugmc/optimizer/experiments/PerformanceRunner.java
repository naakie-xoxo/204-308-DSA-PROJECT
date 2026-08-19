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
import java.util.Locale;

import ug.edu.ugmc.optimizer.algorithms.graph.GraphTraversal;
import ug.edu.ugmc.optimizer.algorithms.graph.PathFinder;
import ug.edu.ugmc.optimizer.algorithms.search.CustomSearch;
import ug.edu.ugmc.optimizer.algorithms.sort.InsertionSort;
import ug.edu.ugmc.optimizer.algorithms.sort.MergeSort;
import ug.edu.ugmc.optimizer.algorithms.sort.QuickSort;
import ug.edu.ugmc.optimizer.algorithms.sort.SelectionSort;
import ug.edu.ugmc.optimizer.database.DatabaseManager;
import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.heap.CustomPriorityQueue;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.trees.BinarySearchTree;
import ug.edu.ugmc.optimizer.datastructures.trees.RedBlackTree;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/**
 * Reproducible empirical-efficiency study for the UGMC optimizer.
 *
 * <p>Inputs are prepared before timing, every measured operation uses
 * {@link System#nanoTime()}, individual trials are retained, and summaries use
 * the arithmetic mean rounded to the nearest nanosecond. Warm-up work is never
 * included in the measured evidence.</p>
 */
public final class PerformanceRunner {

    private static final int[] DATA_SCALES = {100, 500, 1_000, 5_000, 10_000, 50_000};
    private static final int[] HASH_SCALES = {100, 500, 1_000, 5_000, 10_000, 20_000};
    private static final double[] HASH_LOAD_FACTORS = {0.50, 0.75, 1.00, 1.50, 2.00};
    private static final int[] TREE_SCALES = {100, 250, 500, 1_000, 2_000};
    private static final int[] PRIORITY_SCALES = {100, 500, 1_000, 5_000, 10_000, 20_000};
    private static final int[] GRAPH_SCALES = {50, 100, 200, 500};
    private static final String[] EXPERIMENT_CATEGORIES = {
        "Sort", "Search", "HashLoadFactor", "TreeComparison",
        "PriorityDispatch", "GraphTraversal", "MST"
    };

    private static final int GRAPH_EDGE_MULTIPLIER = 2;
    private static final int WARM_UP_SIZE = 100;
    private static final int MINIMUM_MEASURED_TRIALS = 3;
    private static final int DEFAULT_TRIALS = 3;
    private static final int SORT_ALGORITHM_COUNT = 4;
    private static final int SEARCH_ALGORITHM_COUNT = 2;
    private static final int TREE_SERIES_COUNT = 4;
    private static final int PRIORITY_SERIES_COUNT = 2;
    private static final int GRAPH_SERIES_COUNT = 3;
    private static final int MST_SERIES_COUNT = 2;

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
    private final DynamicArray<SummaryRecord> summaries;

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
        if (trialCount < MINIMUM_MEASURED_TRIALS) {
            throw new IllegalArgumentException(
                    "At least " + MINIMUM_MEASURED_TRIALS + " measured trials are required.");
        }

        this.requestSeeds = requestSeeds;
        this.roadWeightSeeds = roadWeightSeeds;
        this.trialCount = trialCount;
        this.rawRuns = new DynamicArray<>(512);
        this.summaries = new DynamicArray<>(192);
    }

    /** Runs the complete study against a local SQLite database. */
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

    /** Returns a defensive copy of the retained search/sort scales. */
    public static int[] getDataScales() {
        return copy(DATA_SCALES);
    }

    /** Returns the required 50/100/200/500 graph vertex scales. */
    public static int[] getGraphScales() {
        return copy(GRAPH_SCALES);
    }

    /** Returns the selected 100-to-20,000 hash-table key scales. */
    public static int[] getHashScales() {
        return copy(HASH_SCALES);
    }

    /** Returns the representative load factors selected for this study. */
    public static double[] getHashLoadFactors() {
        double[] result = new double[HASH_LOAD_FACTORS.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = HASH_LOAD_FACTORS[index];
        }
        return result;
    }

    /** Returns the equivalent ascending-input tree scales. */
    public static int[] getTreeScales() {
        return copy(TREE_SCALES);
    }

    /** Returns the priority-dispatch request scales. */
    public static int[] getPriorityScales() {
        return copy(PRIORITY_SCALES);
    }

    /** Returns every experiment category written to the raw and summary CSVs. */
    public static String[] getExperimentCategories() {
        String[] result = new String[EXPERIMENT_CATEGORIES.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = EXPERIMENT_CATEGORIES[index];
        }
        return result;
    }

    /** @return the brief-required minimum number of measured trials */
    public static int getMinimumMeasuredTrials() {
        return MINIMUM_MEASURED_TRIALS;
    }

    /** Executes warm-up, all measured trials, summaries, and evidence export. */
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

        BenchmarkSummary summary = new BenchmarkSummary();
        for (int index = 0; index < DATA_SCALES.length; index++) {
            benchmarkSorts(index, DATA_SCALES[index], summary);
            benchmarkSearches(index, DATA_SCALES[index], summary);
            System.out.println("Completed search/sort scale: " + DATA_SCALES[index]);
        }
        for (int index = 0; index < HASH_SCALES.length; index++) {
            benchmarkHashTables(index, HASH_SCALES[index], summary);
            System.out.println("Completed hash-table scale: " + HASH_SCALES[index]);
        }
        for (int index = 0; index < TREE_SCALES.length; index++) {
            benchmarkTrees(index, TREE_SCALES[index], summary);
            System.out.println("Completed tree scale: " + TREE_SCALES[index]);
        }
        for (int index = 0; index < PRIORITY_SCALES.length; index++) {
            benchmarkPriorityDispatch(index, PRIORITY_SCALES[index], summary);
            System.out.println("Completed priority-dispatch scale: " + PRIORITY_SCALES[index]);
        }
        for (int index = 0; index < GRAPH_SCALES.length; index++) {
            benchmarkGraphAlgorithms(index, GRAPH_SCALES[index], summary);
            System.out.println("Completed graph scale: " + GRAPH_SCALES[index]);
        }

        writeRawCsv(resultsDirectory.resolve("benchmark_raw.csv"));
        writeGenericSummary(resultsDirectory.resolve("benchmark_summary.csv"));
        writeSortSummary(resultsDirectory.resolve("sort_benchmarks.csv"), summary);
        writeSearchSummary(resultsDirectory.resolve("search_benchmarks.csv"), summary);
        writeHashSummary(resultsDirectory.resolve("hash_load_factor_benchmarks.csv"), summary);
        writeTreeSummary(resultsDirectory.resolve("tree_benchmarks.csv"), summary);
        writePrioritySummary(resultsDirectory.resolve("priority_dispatch_benchmarks.csv"), summary);
        writeGraphSummary(resultsDirectory.resolve("graph_benchmarks.csv"), summary);
        writeMstSummary(resultsDirectory.resolve("mst_benchmarks.csv"), summary);
        writeMetadata(resultsDirectory.resolve("benchmark_metadata.csv"));
        writePlots(resultsDirectory, summary);
        writeCanonicalAlgorithmRuns(canonicalAlgorithmRuns);

        System.out.println("Actual benchmark CSVs written to "
                + resultsDirectory.toAbsolutePath().normalize());
        System.out.println("Canonical algorithm runs replaced at "
                + canonicalAlgorithmRuns.toAbsolutePath().normalize());
    }

    private void warmUpJvm() throws Exception {
        DynamicArray<Integer> seed = buildSortData(WARM_UP_SIZE);
        DynamicArray<Integer> mergeInput = copyOf(seed);
        DynamicArray<Integer> quickInput = copyOf(seed);
        DynamicArray<Integer> insertionInput = copyOf(seed);
        DynamicArray<Integer> selectionInput = copyOf(seed);
        MergeSort.sort(mergeInput);
        QuickSort.sort(quickInput);
        InsertionSort.sort(insertionInput);
        SelectionSort.sort(selectionInput);

        DynamicArray<Integer> searchData = buildSearchData(WARM_UP_SIZE);
        resultSink ^= CustomSearch.linearSearch(searchData, WARM_UP_SIZE - 1);
        resultSink ^= CustomSearch.binarySearchPresorted(searchData, WARM_UP_SIZE - 1);

        String[] hashKeys = buildHashKeys(WARM_UP_SIZE);
        CustomHashTable<String, Integer> hashTable = new CustomHashTable<>(200);
        for (int index = 0; index < hashKeys.length; index++) {
            hashTable.put(hashKeys[index], index);
            resultSink ^= hashTable.get(hashKeys[index]);
        }

        BinarySearchTree bst = new BinarySearchTree();
        RedBlackTree<Integer, Integer> balanced = new RedBlackTree<>();
        CustomPriorityQueue queue = new CustomPriorityQueue();
        for (int index = 0; index < WARM_UP_SIZE; index++) {
            bst.insert(index);
            balanced.put(index, index);
            queue.insert(priorityId(index), priorityFor(index));
        }
        resultSink ^= bst.search(WARM_UP_SIZE - 1) ? 1 : 0;
        resultSink ^= balanced.contains(WARM_UP_SIZE - 1) ? 1 : 0;
        for (int index = 0; index < WARM_UP_SIZE; index++) {
            resultSink ^= queue.extractHighestPriority().hashCode();
        }

        GraphFixture graph = buildGraph(50);
        resultSink ^= GraphTraversal.bfsTraversal(graph.graph, graph.startNode).size();
        resultSink ^= GraphTraversal.dfsTraversal(graph.graph, graph.startNode).size();
        resultSink ^= PathFinder.dijkstra(graph.graph, graph.startNode, graph.targetNode);
        resultSink ^= PathFinder.primMST(graph.graph).getTotalCost();
        resultSink ^= PathFinder.kruskalMST(graph.graph).getTotalCost();

        requireSorted(mergeInput, "MergeSort warm-up");
        requireSorted(quickInput, "QuickSort warm-up");
        requireSorted(insertionInput, "InsertionSort warm-up");
        requireSorted(selectionInput, "SelectionSort warm-up");
    }

    private void benchmarkSorts(int scaleIndex, int scale, BenchmarkSummary summary)
            throws Exception {
        long[][] times = new long[SORT_ALGORITHM_COUNT][trialCount];
        long latestPenaltyCost = 0;
        long latestPenaltyApplications = 0;
        long latestFallbacks = 0;
        long latestSelectionComparisons = 0;
        DynamicArray<Integer> source = buildSortData(scale);

        for (int trial = 1; trial <= trialCount; trial++) {
            DynamicArray<Integer> mergeInput = copyOf(source);
            Measurement measurement = measure(() -> {
                MergeSort.sort(mergeInput);
                return endpointChecksum(mergeInput);
            });
            requireSorted(mergeInput, "MergeSort");
            times[0][trial - 1] = measurement.timeNs;
            addRun("Sort", "MergeSort", scale, "", trial, measurement, "", 0);

            DynamicArray<Integer> quickInput = copyOf(source);
            measurement = measure(() -> {
                QuickSort.sort(quickInput);
                return endpointChecksum(quickInput);
            });
            requireSorted(quickInput, "QuickSort");
            latestFallbacks = QuickSort.getInsertionFallbackCount();
            times[1][trial - 1] = measurement.timeNs;
            addRun("Sort", "QuickSort", scale,
                    "subarray_cutoff=" + QuickSort.getSubarrayCutoff(), trial,
                    measurement, "insertion_fallbacks", latestFallbacks);

            DynamicArray<Integer> selectionInput = copyOf(source);
            measurement = measure(() -> {
                SelectionSort.sort(selectionInput);
                return endpointChecksum(selectionInput);
            });
            requireSorted(selectionInput, "SelectionSort");
            latestSelectionComparisons = SelectionSort.getComparisons();
            times[2][trial - 1] = measurement.timeNs;
            addRun("Sort", "SelectionSort", scale, "", trial,
                    measurement, "comparisons", latestSelectionComparisons);

            DynamicArray<Integer> insertionInput = copyOf(source);
            measurement = measure(() -> {
                InsertionSort.sort(insertionInput);
                return endpointChecksum(insertionInput)
                        ^ InsertionSort.getShiftPenaltyAccumulator();
            });
            requireSorted(insertionInput, "InsertionSort");
            latestPenaltyCost = InsertionSort.getWeightedShiftCost();
            latestPenaltyApplications = InsertionSort.getShiftPenaltyApplications();
            times[3][trial - 1] = measurement.timeNs;
            addRun("Sort", "InsertionSort", scale, "shift_weight=749", trial,
                    measurement, "weighted_shift_cost", latestPenaltyCost);
        }

        String[] algorithms = {"MergeSort", "QuickSort", "SelectionSort", "InsertionSort"};
        for (int algorithm = 0; algorithm < algorithms.length; algorithm++) {
            long average = arithmeticMean(times[algorithm]);
            summary.sortTimes[scaleIndex][algorithm] = average;
            String secondary = algorithm == 1
                    ? "subarray_cutoff=" + QuickSort.getSubarrayCutoff()
                    : algorithm == 3 ? "shift_weight=749" : "";
            String metric = algorithm == 1 ? "insertion_fallbacks"
                    : algorithm == 2 ? "comparisons"
                    : algorithm == 3 ? "weighted_shift_cost" : "";
            long metricValue = algorithm == 1 ? latestFallbacks
                    : algorithm == 2 ? latestSelectionComparisons
                    : algorithm == 3 ? latestPenaltyCost : 0;
            addSummary("Sort", algorithms[algorithm], scale, secondary,
                    average, metric, metricValue);
        }
        summary.insertionPenaltyCost[scaleIndex] = latestPenaltyCost;
        summary.insertionPenaltyApplications[scaleIndex] = latestPenaltyApplications;
        summary.quickSortFallbacks[scaleIndex] = latestFallbacks;
    }

    private void benchmarkSearches(int scaleIndex, int scale, BenchmarkSummary summary)
            throws Exception {
        long[] linearTimes = new long[trialCount];
        long[] binaryTimes = new long[trialCount];
        DynamicArray<Integer> sortedIds = buildSearchData(scale);
        int target = scale - 1;

        for (int trial = 1; trial <= trialCount; trial++) {
            Measurement linear = measure(() -> CustomSearch.linearSearch(sortedIds, target));
            Measurement binary = measure(
                    () -> CustomSearch.binarySearchPresorted(sortedIds, target));
            if (linear.result != target || binary.result != target) {
                throw new IllegalStateException("Search benchmark returned the wrong index.");
            }
            linearTimes[trial - 1] = linear.timeNs;
            binaryTimes[trial - 1] = binary.timeNs;
            String secondary = "target=" + target;
            addRun("Search", "LinearSearch", scale, secondary, trial, linear, "", 0);
            addRun("Search", "BinarySearch", scale, secondary, trial, binary, "", 0);
        }

        summary.searchTimes[scaleIndex][0] = arithmeticMean(linearTimes);
        summary.searchTimes[scaleIndex][1] = arithmeticMean(binaryTimes);
        addSummary("Search", "LinearSearch", scale, "target=" + target,
                summary.searchTimes[scaleIndex][0], "", 0);
        addSummary("Search", "BinarySearch", scale, "target=" + target,
                summary.searchTimes[scaleIndex][1], "", 0);
    }

    private void benchmarkHashTables(int scaleIndex, int scale, BenchmarkSummary summary)
            throws Exception {
        String[] keys = buildHashKeys(scale);
        for (int loadIndex = 0; loadIndex < HASH_LOAD_FACTORS.length; loadIndex++) {
            int capacity = Math.max(1,
                    (int) Math.ceil(scale / HASH_LOAD_FACTORS[loadIndex]));
            double actualLoadFactor = (double) scale / capacity;
            String base = "capacity=" + capacity
                    + ";load_factor=" + formatDecimal(actualLoadFactor);
            long[] insertTimes = new long[trialCount];
            long[] lookupTimes = new long[trialCount];
            int collisionCount = 0;

            for (int trial = 1; trial <= trialCount; trial++) {
                CustomHashTable<String, Integer> table = new CustomHashTable<>(capacity);
                Measurement insert = measure(() -> {
                    for (int index = 0; index < keys.length; index++) {
                        table.put(keys[index], index);
                    }
                    return table.getSize();
                });
                if (insert.result != scale || table.getCapacity() != capacity) {
                    throw new IllegalStateException("Hash-table insertion benchmark is incomplete.");
                }
                collisionCount = table.getCollisionCount();
                insertTimes[trial - 1] = insert.timeNs;
                addRun("HashLoadFactor", "CustomHashTable", scale,
                        base + ";operation=insert_all", trial, insert,
                        "collisions", collisionCount);

                Measurement lookup = measure(() -> {
                    long checksum = 0;
                    for (int index = 0; index < keys.length; index++) {
                        Integer value = table.get(keys[index]);
                        if (value == null) {
                            return -1;
                        }
                        checksum += value;
                    }
                    return checksum;
                });
                if (lookup.result < 0) {
                    throw new IllegalStateException("Hash-table lookup missed a seeded key.");
                }
                lookupTimes[trial - 1] = lookup.timeNs;
                addRun("HashLoadFactor", "CustomHashTable", scale,
                        base + ";operation=lookup_all", trial, lookup,
                        "collisions", collisionCount);
            }

            summary.hashInsertTimes[scaleIndex][loadIndex] = arithmeticMean(insertTimes);
            summary.hashLookupTimes[scaleIndex][loadIndex] = arithmeticMean(lookupTimes);
            summary.hashCollisions[scaleIndex][loadIndex] = collisionCount;
            addSummary("HashLoadFactor", "CustomHashTable", scale,
                    base + ";operation=insert_all",
                    summary.hashInsertTimes[scaleIndex][loadIndex],
                    "collisions", collisionCount);
            addSummary("HashLoadFactor", "CustomHashTable", scale,
                    base + ";operation=lookup_all",
                    summary.hashLookupTimes[scaleIndex][loadIndex],
                    "collisions", collisionCount);
        }
    }

    private void benchmarkTrees(int scaleIndex, int scale, BenchmarkSummary summary)
            throws Exception {
        long[][] times = new long[TREE_SERIES_COUNT][trialCount];
        for (int trial = 1; trial <= trialCount; trial++) {
            BinarySearchTree bst = new BinarySearchTree();
            Measurement bstInsert = measure(() -> {
                for (int key = 0; key < scale; key++) {
                    bst.insert(key);
                }
                return scale;
            });
            if (!bst.search(scale - 1)) {
                throw new IllegalStateException("BST insertion benchmark lost a key.");
            }
            times[0][trial - 1] = bstInsert.timeNs;
            addRun("TreeComparison", "BinarySearchTree", scale,
                    "order=ascending;operation=insert_all", trial, bstInsert, "", 0);

            RedBlackTree<Integer, Integer> balanced = new RedBlackTree<>();
            Measurement balancedInsert = measure(() -> {
                for (int key = 0; key < scale; key++) {
                    balanced.put(key, key);
                }
                return balanced.size();
            });
            if (balancedInsert.result != scale || !balanced.validate()) {
                throw new IllegalStateException("Red-black insertion benchmark is invalid.");
            }
            times[1][trial - 1] = balancedInsert.timeNs;
            addRun("TreeComparison", "RedBlackTree", scale,
                    "order=ascending;operation=insert_all", trial, balancedInsert,
                    "rotations", balanced.getLeftRotations() + balanced.getRightRotations());

            Measurement bstSearch = measure(() -> {
                int found = 0;
                for (int key = 0; key < scale; key++) {
                    if (bst.search(key)) {
                        found++;
                    }
                }
                return found;
            });
            Measurement balancedSearch = measure(() -> {
                int found = 0;
                for (int key = 0; key < scale; key++) {
                    if (balanced.contains(key)) {
                        found++;
                    }
                }
                return found;
            });
            if (bstSearch.result != scale || balancedSearch.result != scale) {
                throw new IllegalStateException("Tree search benchmark missed a key.");
            }
            times[2][trial - 1] = bstSearch.timeNs;
            times[3][trial - 1] = balancedSearch.timeNs;
            addRun("TreeComparison", "BinarySearchTree", scale,
                    "order=ascending;operation=search_all", trial, bstSearch, "", 0);
            addRun("TreeComparison", "RedBlackTree", scale,
                    "order=ascending;operation=search_all", trial, balancedSearch,
                    "comparisons", balanced.getComparisons());
        }

        String[] algorithms = {
            "BinarySearchTree", "RedBlackTree", "BinarySearchTree", "RedBlackTree"
        };
        String[] operations = {"insert_all", "insert_all", "search_all", "search_all"};
        for (int series = 0; series < TREE_SERIES_COUNT; series++) {
            summary.treeTimes[scaleIndex][series] = arithmeticMean(times[series]);
            addSummary("TreeComparison", algorithms[series], scale,
                    "order=ascending;operation=" + operations[series],
                    summary.treeTimes[scaleIndex][series], "", 0);
        }
    }

    private void benchmarkPriorityDispatch(
            int scaleIndex, int scale, BenchmarkSummary summary) throws Exception {
        long[] insertTimes = new long[trialCount];
        long[] extractTimes = new long[trialCount];
        String[] ids = new String[scale];
        int[] priorities = new int[scale];
        for (int index = 0; index < scale; index++) {
            ids[index] = priorityId(index);
            priorities[index] = priorityFor(index);
        }

        for (int trial = 1; trial <= trialCount; trial++) {
            CustomPriorityQueue queue = new CustomPriorityQueue();
            Measurement insert = measure(() -> {
                for (int index = 0; index < ids.length; index++) {
                    queue.insert(ids[index], priorities[index]);
                }
                return ids.length;
            });
            insertTimes[trial - 1] = insert.timeNs;
            addRun("PriorityDispatch", "CustomPriorityQueue", scale,
                    "operation=insert_all", trial, insert, "", 0);

            Measurement extract = measure(() -> {
                long checksum = 0;
                for (int index = 0; index < ids.length; index++) {
                    checksum ^= queue.extractHighestPriority().hashCode();
                }
                return checksum;
            });
            extractTimes[trial - 1] = extract.timeNs;
            addRun("PriorityDispatch", "CustomPriorityQueue", scale,
                    "operation=extract_all", trial, extract, "", 0);
        }

        summary.priorityTimes[scaleIndex][0] = arithmeticMean(insertTimes);
        summary.priorityTimes[scaleIndex][1] = arithmeticMean(extractTimes);
        addSummary("PriorityDispatch", "CustomPriorityQueue", scale,
                "operation=insert_all", summary.priorityTimes[scaleIndex][0], "", 0);
        addSummary("PriorityDispatch", "CustomPriorityQueue", scale,
                "operation=extract_all", summary.priorityTimes[scaleIndex][1], "", 0);
    }

    private void benchmarkGraphAlgorithms(
            int scaleIndex, int vertices, BenchmarkSummary summary) throws Exception {
        GraphFixture fixture = buildGraph(vertices);
        long[] bfsTimes = new long[trialCount];
        long[] dfsTimes = new long[trialCount];
        long[] dijkstraTimes = new long[trialCount];
        long[] primTimes = new long[trialCount];
        long[] kruskalTimes = new long[trialCount];
        int mstCost = -1;
        String secondary = "vertices=" + vertices + ";edges=" + fixture.edgeCount
                + ";source=" + fixture.startNode + ";target=" + fixture.targetNode;

        for (int trial = 1; trial <= trialCount; trial++) {
            Measurement bfs = measure(
                    () -> GraphTraversal.bfsTraversal(fixture.graph, fixture.startNode).size());
            Measurement dfs = measure(
                    () -> GraphTraversal.dfsTraversal(fixture.graph, fixture.startNode).size());
            Measurement dijkstra = measure(() -> PathFinder.dijkstra(
                    fixture.graph, fixture.startNode, fixture.targetNode));
            if (bfs.result != vertices || dfs.result != vertices || dijkstra.result < 0) {
                throw new IllegalStateException("Graph traversal/routing benchmark failed sanity checks.");
            }
            bfsTimes[trial - 1] = bfs.timeNs;
            dfsTimes[trial - 1] = dfs.timeNs;
            dijkstraTimes[trial - 1] = dijkstra.timeNs;
            addRun("GraphTraversal", "BFS", vertices, secondary, trial, bfs,
                    "edges", fixture.edgeCount);
            addRun("GraphTraversal", "DFS", vertices, secondary, trial, dfs,
                    "edges", fixture.edgeCount);
            addRun("GraphTraversal", "Dijkstra", vertices, secondary, trial, dijkstra,
                    "edges", fixture.edgeCount);

            PathFinder.MstResult[] primResult = new PathFinder.MstResult[1];
            Measurement prim = measure(() -> {
                primResult[0] = PathFinder.primMST(fixture.graph);
                return primResult[0].getTotalCost();
            });
            PathFinder.MstResult[] kruskalResult = new PathFinder.MstResult[1];
            Measurement kruskal = measure(() -> {
                kruskalResult[0] = PathFinder.kruskalMST(fixture.graph);
                return kruskalResult[0].getTotalCost();
            });
            if (!primResult[0].isConnected() || !kruskalResult[0].isConnected()
                    || primResult[0].getEdges().length != vertices - 1
                    || kruskalResult[0].getEdges().length != vertices - 1
                    || prim.result != kruskal.result) {
                throw new IllegalStateException("Prim/Kruskal benchmark produced inconsistent MSTs.");
            }
            mstCost = (int) prim.result;
            primTimes[trial - 1] = prim.timeNs;
            kruskalTimes[trial - 1] = kruskal.timeNs;
            addRun("MST", "Prim", vertices, secondary, trial, prim,
                    "mst_cost", mstCost);
            addRun("MST", "Kruskal", vertices, secondary, trial, kruskal,
                    "mst_cost", mstCost);
        }

        summary.graphEdges[scaleIndex] = fixture.edgeCount;
        summary.graphTimes[scaleIndex][0] = arithmeticMean(bfsTimes);
        summary.graphTimes[scaleIndex][1] = arithmeticMean(dfsTimes);
        summary.graphTimes[scaleIndex][2] = arithmeticMean(dijkstraTimes);
        summary.mstTimes[scaleIndex][0] = arithmeticMean(primTimes);
        summary.mstTimes[scaleIndex][1] = arithmeticMean(kruskalTimes);
        summary.mstCosts[scaleIndex] = mstCost;
        addSummary("GraphTraversal", "BFS", vertices, secondary,
                summary.graphTimes[scaleIndex][0], "edges", fixture.edgeCount);
        addSummary("GraphTraversal", "DFS", vertices, secondary,
                summary.graphTimes[scaleIndex][1], "edges", fixture.edgeCount);
        addSummary("GraphTraversal", "Dijkstra", vertices, secondary,
                summary.graphTimes[scaleIndex][2], "edges", fixture.edgeCount);
        addSummary("MST", "Prim", vertices, secondary,
                summary.mstTimes[scaleIndex][0], "mst_cost", mstCost);
        addSummary("MST", "Kruskal", vertices, secondary,
                summary.mstTimes[scaleIndex][1], "mst_cost", mstCost);
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

    private static String[] buildHashKeys(int size) {
        String[] keys = new String[size];
        for (int index = 0; index < size; index++) {
            keys[index] = "REQ-BENCH-" + index;
        }
        return keys;
    }

    private int priorityFor(int index) {
        ServiceRequest request = requestSeeds.get(index % requestSeeds.size());
        return -request.getUrgency();
    }

    private static String priorityId(int index) {
        return "REQ-PRIORITY-" + index;
    }

    private GraphFixture buildGraph(int vertexCount) {
        if (vertexCount < 4) {
            throw new IllegalArgumentException("Synthetic graph requires at least four vertices.");
        }
        CustomGraph graph = new CustomGraph(vertexCount);
        for (int index = 0; index < vertexCount; index++) {
            graph.addNode(benchmarkLocationId(index));
        }

        int targetEdges = vertexCount * GRAPH_EDGE_MULTIPLIER;
        int edgeCount = 0;
        for (int index = 0; index < vertexCount - 1; index++) {
            graph.addEdge(
                    benchmarkLocationId(index), benchmarkLocationId(index + 1),
                    roadWeight(edgeCount));
            edgeCount++;
        }
        for (int source = 1; source < vertexCount - 2 && edgeCount < targetEdges; source++) {
            for (int destination = source + 2;
                    destination < vertexCount - 1 && edgeCount < targetEdges;
                    destination++) {
                graph.addEdge(
                        benchmarkLocationId(source), benchmarkLocationId(destination),
                        roadWeight(edgeCount));
                edgeCount++;
            }
        }
        if (edgeCount != targetEdges) {
            throw new IllegalStateException(
                    "Unable to construct the requested deterministic graph density.");
        }
        return new GraphFixture(
                graph, benchmarkLocationId(0), benchmarkLocationId(vertexCount - 1), edgeCount);
    }

    private int roadWeight(int edgeIndex) {
        return Math.max(1, roadWeightSeeds.get(edgeIndex % roadWeightSeeds.size()));
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

    private static int[] copy(int[] source) {
        int[] result = new int[source.length];
        for (int index = 0; index < source.length; index++) {
            result[index] = source[index];
        }
        return result;
    }

    private static long endpointChecksum(DynamicArray<Integer> values) {
        return values.size() == 0 ? 0
                : ((long) values.get(0) << 32) ^ values.get(values.size() - 1);
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

    /** Computes the arithmetic mean rounded to the nearest nanosecond. */
    static long arithmeticMean(long[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one measurement is required.");
        }
        double sum = 0;
        for (long value : values) {
            if (value < 0) {
                throw new IllegalArgumentException("Runtime measurements cannot be negative.");
            }
            sum += value;
        }
        return Math.round(sum / values.length);
    }

    private void addRun(
            String experiment, String algorithm, int inputSize, String secondaryParameter,
            int trial, Measurement measurement, String metricName, long metricValue) {
        rawRuns.insert(new RunRecord(
                experiment, algorithm, inputSize, secondaryParameter, trial,
                measurement.timeNs, measurement.memoryKb,
                metricName, metricValue, measurement.result));
    }

    private void addSummary(
            String experiment, String algorithm, int inputSize, String secondaryParameter,
            long averageRuntimeNs, String metricName, long metricValue) {
        summaries.insert(new SummaryRecord(
                experiment, algorithm, inputSize, secondaryParameter, trialCount,
                averageRuntimeNs, metricName, metricValue));
    }

    private void writeRawCsv(Path output) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("experiment,algorithm,input_size,secondary_parameter,trial,runtime_ns,"
                    + "memory_kb,metric_name,metric_value,result_value");
            writer.newLine();
            for (int index = 0; index < rawRuns.size(); index++) {
                RunRecord run = rawRuns.get(index);
                writer.write(csv(run.experiment) + "," + csv(run.algorithm) + "," + run.inputSize
                        + "," + csv(run.secondaryParameter) + "," + run.trial + ","
                        + run.timeNs + "," + run.memoryKb + "," + csv(run.metricName) + ","
                        + run.metricValue + "," + run.resultValue);
                writer.newLine();
            }
        }
    }

    private void writeGenericSummary(Path output) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("experiment,algorithm,input_size,secondary_parameter,trials,"
                    + "average_runtime_ns,metric_name,metric_value");
            writer.newLine();
            for (int index = 0; index < summaries.size(); index++) {
                SummaryRecord summary = summaries.get(index);
                writer.write(csv(summary.experiment) + "," + csv(summary.algorithm) + ","
                        + summary.inputSize + "," + csv(summary.secondaryParameter) + ","
                        + summary.trials + "," + summary.averageRuntimeNs + ","
                        + csv(summary.metricName) + "," + summary.metricValue);
                writer.newLine();
            }
        }
    }

    private void writeSortSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("scale,merge_sort_average_ns,quick_sort_average_ns,"
                    + "selection_sort_average_ns,insertion_sort_average_ns,"
                    + "insertion_weighted_shift_cost,insertion_penalty_applications,"
                    + "quick_sort_cutoff,quick_sort_fallbacks,trials");
            writer.newLine();
            for (int index = 0; index < DATA_SCALES.length; index++) {
                writer.write(DATA_SCALES[index] + "," + summary.sortTimes[index][0] + ","
                        + summary.sortTimes[index][1] + "," + summary.sortTimes[index][2] + ","
                        + summary.sortTimes[index][3] + ","
                        + summary.insertionPenaltyCost[index] + ","
                        + summary.insertionPenaltyApplications[index] + ","
                        + QuickSort.getSubarrayCutoff() + ","
                        + summary.quickSortFallbacks[index] + "," + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeSearchSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("scale,linear_search_average_ns,binary_search_average_ns,trials");
            writer.newLine();
            for (int index = 0; index < DATA_SCALES.length; index++) {
                writer.write(DATA_SCALES[index] + "," + summary.searchTimes[index][0] + ","
                        + summary.searchTimes[index][1] + "," + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeHashSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("input_size,capacity,load_factor,operation,trials,"
                    + "average_runtime_ns,collision_count");
            writer.newLine();
            for (int scaleIndex = 0; scaleIndex < HASH_SCALES.length; scaleIndex++) {
                int size = HASH_SCALES[scaleIndex];
                for (int loadIndex = 0; loadIndex < HASH_LOAD_FACTORS.length; loadIndex++) {
                    int capacity = Math.max(1,
                            (int) Math.ceil(size / HASH_LOAD_FACTORS[loadIndex]));
                    String load = formatDecimal((double) size / capacity);
                    writer.write(size + "," + capacity + "," + load + ",insert_all,"
                            + trialCount + "," + summary.hashInsertTimes[scaleIndex][loadIndex]
                            + "," + summary.hashCollisions[scaleIndex][loadIndex]);
                    writer.newLine();
                    writer.write(size + "," + capacity + "," + load + ",lookup_all,"
                            + trialCount + "," + summary.hashLookupTimes[scaleIndex][loadIndex]
                            + "," + summary.hashCollisions[scaleIndex][loadIndex]);
                    writer.newLine();
                }
            }
        }
    }

    private void writeTreeSummary(Path output, BenchmarkSummary summary) throws IOException {
        String[] types = {"BinarySearchTree", "RedBlackTree", "BinarySearchTree", "RedBlackTree"};
        String[] operations = {"insert_all", "insert_all", "search_all", "search_all"};
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("tree_type,input_size,input_order,operation,trials,average_runtime_ns");
            writer.newLine();
            for (int scaleIndex = 0; scaleIndex < TREE_SCALES.length; scaleIndex++) {
                for (int series = 0; series < TREE_SERIES_COUNT; series++) {
                    writer.write(types[series] + "," + TREE_SCALES[scaleIndex]
                            + ",ascending," + operations[series] + "," + trialCount + ","
                            + summary.treeTimes[scaleIndex][series]);
                    writer.newLine();
                }
            }
        }
    }

    private void writePrioritySummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("queue_size,operation,trials,average_runtime_ns");
            writer.newLine();
            for (int index = 0; index < PRIORITY_SCALES.length; index++) {
                writer.write(PRIORITY_SCALES[index] + ",insert_all," + trialCount + ","
                        + summary.priorityTimes[index][0]);
                writer.newLine();
                writer.write(PRIORITY_SCALES[index] + ",extract_all," + trialCount + ","
                        + summary.priorityTimes[index][1]);
                writer.newLine();
            }
        }
    }

    private void writeGraphSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("vertices,edges,bfs_average_ns,dfs_average_ns,"
                    + "dijkstra_average_ns,trials");
            writer.newLine();
            for (int index = 0; index < GRAPH_SCALES.length; index++) {
                writer.write(GRAPH_SCALES[index] + "," + summary.graphEdges[index] + ","
                        + summary.graphTimes[index][0] + "," + summary.graphTimes[index][1]
                        + "," + summary.graphTimes[index][2] + "," + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeMstSummary(Path output, BenchmarkSummary summary) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("vertices,edges,prim_average_ns,kruskal_average_ns,"
                    + "mst_cost,trials");
            writer.newLine();
            for (int index = 0; index < GRAPH_SCALES.length; index++) {
                writer.write(GRAPH_SCALES[index] + "," + summary.graphEdges[index] + ","
                        + summary.mstTimes[index][0] + "," + summary.mstTimes[index][1]
                        + "," + summary.mstCosts[index] + "," + trialCount);
                writer.newLine();
            }
        }
    }

    private void writeMetadata(Path output) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
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
            writeMetadataRow(writer, "minimum_required_trials",
                    Integer.toString(MINIMUM_MEASURED_TRIALS));
            writeMetadataRow(writer, "aggregation", "arithmetic_mean_rounded_to_nearest_ns");
            writeMetadataRow(writer, "warm_up_records", Integer.toString(WARM_UP_SIZE));
            writeMetadataRow(writer, "warm_up_recorded", "false");
            writeMetadataRow(writer, "timing_clock", "System.nanoTime");
            writeMetadataRow(writer, "search_sort_scales", join(DATA_SCALES));
            writeMetadataRow(writer, "hash_scales", join(HASH_SCALES));
            writeMetadataRow(writer, "hash_load_factors", join(HASH_LOAD_FACTORS));
            writeMetadataRow(writer, "tree_scales", join(TREE_SCALES));
            writeMetadataRow(writer, "priority_scales", join(PRIORITY_SCALES));
            writeMetadataRow(writer, "graph_vertex_scales", join(GRAPH_SCALES));
            writeMetadataRow(writer, "graph_edge_rule", "E=2V");
            writeMetadataRow(writer, "graph_input_kind", "deterministic_synthetic_scaling_graphs");
            writeMetadataRow(writer, "graph_source_target", "BENCH-LOC-0_to_BENCH-LOC-(V-1)");
            writeMetadataRow(writer, "request_seed_source", "SQLite service_requests");
            writeMetadataRow(writer, "road_weight_seed_source", "SQLite roads.travel_time");
        }
    }

    private static void writeMetadataRow(BufferedWriter writer, String property, String value)
            throws IOException {
        writer.write(csv(property) + "," + csv(value));
        writer.newLine();
    }

    private void writePlots(Path resultsDirectory, BenchmarkSummary summary) throws IOException {
        writeSvgPlot(resultsDirectory.resolve("sort_runtimes.svg"),
                "UGMC Sorting Arithmetic-Mean Runtime",
                new String[] {"Merge Sort", "Quick Sort", "Selection Sort", "Insertion Sort"},
                new String[] {"#1565c0", "#00897b", "#ef6c00", "#c62828"},
                summary.sortTimes, DATA_SCALES, "Requests (log scale)",
                "Average runtime (ms)", 1_000_000.0);
        writeSvgPlot(resultsDirectory.resolve("search_runtimes.svg"),
                "UGMC Search Arithmetic-Mean Runtime",
                new String[] {"Linear Search", "Binary Search"},
                new String[] {"#6a1b9a", "#2e7d32"},
                summary.searchTimes, DATA_SCALES, "Records (log scale)",
                "Average runtime (ms)", 1_000_000.0);
        writeSvgPlot(resultsDirectory.resolve("hash_lookup_runtimes.svg"),
                "CustomHashTable Lookup Runtime by Load Factor",
                loadFactorNames(),
                new String[] {"#1565c0", "#00897b", "#ef6c00", "#c62828", "#6a1b9a"},
                summary.hashLookupTimes, HASH_SCALES, "Keys (log scale)",
                "Average lookup-all runtime (ms)", 1_000_000.0);
        writeSvgPlot(resultsDirectory.resolve("hash_collisions.svg"),
                "CustomHashTable Collisions by Load Factor",
                loadFactorNames(),
                new String[] {"#1565c0", "#00897b", "#ef6c00", "#c62828", "#6a1b9a"},
                summary.hashCollisions, HASH_SCALES, "Keys (log scale)",
                "Collision count", 1.0);
        writeSvgPlot(resultsDirectory.resolve("tree_runtimes.svg"),
                "BST vs Red-Black Tree on Ascending Keys",
                new String[] {"BST insert", "RBT insert", "BST search", "RBT search"},
                new String[] {"#c62828", "#1565c0", "#ef6c00", "#00897b"},
                summary.treeTimes, TREE_SCALES, "Keys (log scale)",
                "Average batch runtime (ms)", 1_000_000.0);
        writeSvgPlot(resultsDirectory.resolve("priority_dispatch_runtimes.svg"),
                "CustomPriorityQueue Dispatch Runtime",
                new String[] {"Insert all", "Extract all"},
                new String[] {"#283593", "#ad1457"},
                summary.priorityTimes, PRIORITY_SCALES, "Requests (log scale)",
                "Average batch runtime (ms)", 1_000_000.0);
        writeSvgPlot(resultsDirectory.resolve("graph_runtimes.svg"),
                "Graph Traversal and Dijkstra Runtime (E = 2V)",
                new String[] {"BFS", "DFS", "Dijkstra"},
                new String[] {"#ad1457", "#ef6c00", "#283593"},
                summary.graphTimes, GRAPH_SCALES, "Vertices (log scale)",
                "Average runtime (ms)", 1_000_000.0);
        writeSvgPlot(resultsDirectory.resolve("mst_runtimes.svg"),
                "Prim vs Kruskal Runtime (E = 2V)",
                new String[] {"Prim", "Kruskal"},
                new String[] {"#1565c0", "#00897b"},
                summary.mstTimes, GRAPH_SCALES, "Vertices (log scale)",
                "Average runtime (ms)", 1_000_000.0);
    }

    private static String[] loadFactorNames() {
        String[] names = new String[HASH_LOAD_FACTORS.length];
        for (int index = 0; index < names.length; index++) {
            names[index] = "Load " + formatDecimal(HASH_LOAD_FACTORS[index]);
        }
        return names;
    }

    private static void writeSvgPlot(
            Path output, String title, String[] seriesNames, String[] colors,
            long[][] values, int[] scales, String xLabel, String yLabel, double divisor)
            throws IOException {
        final int width = 1_200;
        final int height = 680;
        final int left = 110;
        final int right = 45;
        final int top = 100;
        final int bottom = 95;
        final int plotWidth = width - left - right;
        final int plotHeight = height - top - bottom;

        long maximum = 1;
        for (long[] row : values) {
            for (long value : row) {
                maximum = Math.max(maximum, value);
            }
        }
        double minLog = Math.log10(scales[0]);
        double maxLog = Math.log10(scales[scales.length - 1]);
        StringBuilder svg = new StringBuilder(16_000);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\">\n<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n")
                .append("<style>text{font-family:Arial,sans-serif;fill:#263238}")
                .append(".grid{stroke:#cfd8dc;stroke-width:1}.axis{stroke:#37474f;stroke-width:2}")
                .append(".label{font-size:13px}.title{font-size:24px;font-weight:bold}")
                .append(".legend{font-size:14px}</style>\n")
                .append("<text class=\"title\" x=\"").append(width / 2)
                .append("\" y=\"38\" text-anchor=\"middle\">").append(title)
                .append("</text>\n");

        for (int tick = 0; tick <= 5; tick++) {
            int y = top + plotHeight - (plotHeight * tick / 5);
            double axisValue = ((double) maximum * tick / 5.0) / divisor;
            svg.append("<line class=\"grid\" x1=\"").append(left).append("\" y1=\"")
                    .append(y).append("\" x2=\"").append(left + plotWidth)
                    .append("\" y2=\"").append(y).append("\"/>\n")
                    .append("<text class=\"label\" x=\"").append(left - 12)
                    .append("\" y=\"").append(y + 5).append("\" text-anchor=\"end\">")
                    .append(formatAxisValue(axisValue)).append("</text>\n");
        }

        for (int index = 0; index < scales.length; index++) {
            int x = left + (int) Math.round((Math.log10(scales[index]) - minLog)
                    / (maxLog - minLog) * plotWidth);
            svg.append("<line class=\"grid\" x1=\"").append(x).append("\" y1=\"")
                    .append(top).append("\" x2=\"").append(x).append("\" y2=\"")
                    .append(top + plotHeight).append("\"/>\n")
                    .append("<text class=\"label\" x=\"").append(x).append("\" y=\"")
                    .append(top + plotHeight + 25).append("\" text-anchor=\"middle\">")
                    .append(scales[index]).append("</text>\n");
        }

        svg.append("<line class=\"axis\" x1=\"").append(left).append("\" y1=\"")
                .append(top).append("\" x2=\"").append(left).append("\" y2=\"")
                .append(top + plotHeight).append("\"/>\n")
                .append("<line class=\"axis\" x1=\"").append(left).append("\" y1=\"")
                .append(top + plotHeight).append("\" x2=\"").append(left + plotWidth)
                .append("\" y2=\"").append(top + plotHeight).append("\"/>\n")
                .append("<text class=\"label\" x=\"").append(left + plotWidth / 2)
                .append("\" y=\"").append(height - 25)
                .append("\" text-anchor=\"middle\">").append(xLabel).append("</text>\n")
                .append("<text class=\"label\" transform=\"translate(28 ")
                .append(top + plotHeight / 2)
                .append(") rotate(-90)\" text-anchor=\"middle\">")
                .append(yLabel).append("</text>\n");

        for (int series = 0; series < seriesNames.length; series++) {
            svg.append("<polyline fill=\"none\" stroke=\"").append(colors[series])
                    .append("\" stroke-width=\"3\" points=\"");
            for (int scaleIndex = 0; scaleIndex < scales.length; scaleIndex++) {
                int x = left + (int) Math.round((Math.log10(scales[scaleIndex]) - minLog)
                        / (maxLog - minLog) * plotWidth);
                int y = top + plotHeight - (int) Math.round(
                        (double) values[scaleIndex][series] / maximum * plotHeight);
                svg.append(x).append(',').append(y).append(' ');
            }
            svg.append("\"/>\n");
            for (int scaleIndex = 0; scaleIndex < scales.length; scaleIndex++) {
                int x = left + (int) Math.round((Math.log10(scales[scaleIndex]) - minLog)
                        / (maxLog - minLog) * plotWidth);
                int y = top + plotHeight - (int) Math.round(
                        (double) values[scaleIndex][series] / maximum * plotHeight);
                svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                        .append("\" r=\"4\" fill=\"").append(colors[series])
                        .append("\"/>\n");
            }
            int legendX = 70 + series * 220;
            svg.append("<line x1=\"").append(legendX).append("\" y1=\"68\" x2=\"")
                    .append(legendX + 28).append("\" y2=\"68\" stroke=\"")
                    .append(colors[series]).append("\" stroke-width=\"4\"/>\n")
                    .append("<text class=\"legend\" x=\"").append(legendX + 35)
                    .append("\" y=\"73\">").append(seriesNames[series]).append("</text>\n");
        }
        svg.append("</svg>\n");
        Files.writeString(output, svg.toString(), StandardCharsets.UTF_8);
    }

    private void writeCanonicalAlgorithmRuns(Path output) throws IOException {
        String date = LocalDate.now().toString();
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
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

    private static String join(int[] values) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                result.append('|');
            }
            result.append(values[index]);
        }
        return result.toString();
    }

    private static String join(double[] values) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                result.append('|');
            }
            result.append(formatDecimal(values[index]));
        }
        return result.toString();
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String formatAxisValue(double value) {
        if (value >= 100) {
            return Long.toString(Math.round(value));
        }
        if (value >= 10) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
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
        private final String experiment;
        private final String algorithm;
        private final int inputSize;
        private final String secondaryParameter;
        private final int trial;
        private final long timeNs;
        private final long memoryKb;
        private final String metricName;
        private final long metricValue;
        private final long resultValue;

        private RunRecord(
                String experiment, String algorithm, int inputSize, String secondaryParameter,
                int trial, long timeNs, long memoryKb, String metricName,
                long metricValue, long resultValue) {
            this.experiment = experiment;
            this.algorithm = algorithm;
            this.inputSize = inputSize;
            this.secondaryParameter = secondaryParameter;
            this.trial = trial;
            this.timeNs = timeNs;
            this.memoryKb = memoryKb;
            this.metricName = metricName;
            this.metricValue = metricValue;
            this.resultValue = resultValue;
        }
    }

    private static final class SummaryRecord {
        private final String experiment;
        private final String algorithm;
        private final int inputSize;
        private final String secondaryParameter;
        private final int trials;
        private final long averageRuntimeNs;
        private final String metricName;
        private final long metricValue;

        private SummaryRecord(
                String experiment, String algorithm, int inputSize, String secondaryParameter,
                int trials, long averageRuntimeNs, String metricName, long metricValue) {
            this.experiment = experiment;
            this.algorithm = algorithm;
            this.inputSize = inputSize;
            this.secondaryParameter = secondaryParameter;
            this.trials = trials;
            this.averageRuntimeNs = averageRuntimeNs;
            this.metricName = metricName;
            this.metricValue = metricValue;
        }
    }

    private static final class GraphFixture {
        private final CustomGraph graph;
        private final String startNode;
        private final String targetNode;
        private final int edgeCount;

        private GraphFixture(
                CustomGraph graph, String startNode, String targetNode, int edgeCount) {
            this.graph = graph;
            this.startNode = startNode;
            this.targetNode = targetNode;
            this.edgeCount = edgeCount;
        }
    }

    private static final class BenchmarkSummary {
        private final long[][] sortTimes = new long[DATA_SCALES.length][SORT_ALGORITHM_COUNT];
        private final long[][] searchTimes = new long[DATA_SCALES.length][SEARCH_ALGORITHM_COUNT];
        private final long[][] hashInsertTimes =
                new long[HASH_SCALES.length][HASH_LOAD_FACTORS.length];
        private final long[][] hashLookupTimes =
                new long[HASH_SCALES.length][HASH_LOAD_FACTORS.length];
        private final long[][] hashCollisions =
                new long[HASH_SCALES.length][HASH_LOAD_FACTORS.length];
        private final long[][] treeTimes = new long[TREE_SCALES.length][TREE_SERIES_COUNT];
        private final long[][] priorityTimes =
                new long[PRIORITY_SCALES.length][PRIORITY_SERIES_COUNT];
        private final long[][] graphTimes = new long[GRAPH_SCALES.length][GRAPH_SERIES_COUNT];
        private final long[][] mstTimes = new long[GRAPH_SCALES.length][MST_SERIES_COUNT];
        private final long[] insertionPenaltyCost = new long[DATA_SCALES.length];
        private final long[] insertionPenaltyApplications = new long[DATA_SCALES.length];
        private final long[] quickSortFallbacks = new long[DATA_SCALES.length];
        private final int[] graphEdges = new int[GRAPH_SCALES.length];
        private final int[] mstCosts = new int[GRAPH_SCALES.length];
    }
}
