package ug.edu.ugmc.optimizer.application.services;

/**
 * Examiner-facing use cases exposed to the console UI. The UI depends only on
 * this contract and its presentation-safe result records.
 */
public interface OptimizerService {

    enum TraversalMethod { BFS, DFS }
    enum MstMethod { PRIM, KRUSKAL }
    enum SearchMethod { LINEAR, BINARY }
    enum SortMethod { SELECTION, INSERTION, MERGE, QUICK }
    enum CompletionOutcome { COMPLETED, NOT_FOUND, MUST_DISPATCH_FIRST, ALREADY_COMPLETED }

    InitializationResult initializeData() throws Exception;
    RequestView findRequest(String requestId);
    QueueView viewPendingQueue();
    RequestView dispatchNextPriority() throws Exception;
    default CompletionResult completeRequest(String requestId) throws Exception {
        throw new UnsupportedOperationException("Request completion is not supported.");
    }
    TraversalResult traverse(String startLocation, TraversalMethod method);
    SearchResult searchRequestNumber(int requestNumber, SearchMethod method);
    SortResult sortUrgencies(SortMethod method);
    PathResult shortestPath(String startLocation, String targetLocation);
    MstResult minimumSpanningTree(MstMethod method);
    OptimizationResult optimizeRequests(int capacity);
    TestSummary latestTestSummary();
    void runExperiments(int trials) throws Exception;

    record InitializationResult(
            int requestCount,
            int pendingCount,
            int inProgressCount,
            int completedCount,
            int queueCapacity,
            int locationCount) {
        public InitializationResult(int requestCount, int queueCapacity, int locationCount) {
            this(requestCount, requestCount, 0, 0, queueCapacity, locationCount);
        }
    }

    record RequestView(
            String id,
            String patientLabel,
            String category,
            String sourceLocation,
            String destinationLocation,
            int urgency,
            String status,
            String submitted,
            String deadline,
            int weight,
            int value) {
        public RequestView(String id, int urgency, int weight, int value) {
            this(id, "", "", "", "", urgency, "Pending", "", "", weight, value);
        }
    }

    record QueueView(int size, int capacity, RequestView nextRequest) {
    }

    record CompletionResult(CompletionOutcome outcome, RequestView request) {
    }

    record TraversalResult(TraversalMethod method, String[] locations) {
        public TraversalResult { locations = locations.clone(); }
        @Override public String[] locations() { return locations.clone(); }
    }

    record SearchResult(SearchMethod method, int requestNumber, int index) {
        public boolean found() { return index >= 0; }
    }

    record SortResult(
            SortMethod method,
            int totalCount,
            int[] beforeSample,
            int[] lowestSample,
            int[] highestSample,
            int[] distribution) {
        public SortResult {
            beforeSample = beforeSample.clone();
            lowestSample = lowestSample.clone();
            highestSample = highestSample.clone();
            distribution = distribution.clone();
        }

        public SortResult(SortMethod method, int totalCount, int[] sample) {
            this(method, totalCount, sample, sample, sample, distributionFor(sample));
        }

        @Override public int[] beforeSample() { return beforeSample.clone(); }
        @Override public int[] lowestSample() { return lowestSample.clone(); }
        @Override public int[] highestSample() { return highestSample.clone(); }
        @Override public int[] distribution() { return distribution.clone(); }

        /** Compatibility alias for the former first-values sample. */
        public int[] sample() { return lowestSample(); }

        private static int[] distributionFor(int[] values) {
            int[] result = new int[5];
            for (int value : values) {
                if (value >= 1 && value <= 5) { result[value - 1]++; }
            }
            return result;
        }
    }

    record PathResult(boolean reachable, int distance, String[] route) {
        public PathResult { route = route.clone(); }
        @Override public String[] route() { return route.clone(); }
    }

    record MstResult(MstMethod method, boolean connected, int totalCost, String[] edges) {
        public MstResult { edges = edges.clone(); }
        @Override public String[] edges() { return edges.clone(); }
    }

    record OptimizationResult(
            int capacity,
            int greedyValue,
            int greedyWeight,
            String[] greedySelectedRequestIds,
            int optimalValue,
            int selectedWeight,
            String[] selectedRequestIds) {
        public OptimizationResult {
            greedySelectedRequestIds = greedySelectedRequestIds.clone();
            selectedRequestIds = selectedRequestIds.clone();
        }

        public OptimizationResult(
                int greedyValue, int optimalValue, int selectedWeight, String[] selectedRequestIds) {
            this(selectedWeight, greedyValue, 0, new String[0],
                    optimalValue, selectedWeight, selectedRequestIds);
        }

        @Override public String[] greedySelectedRequestIds() {
            return greedySelectedRequestIds.clone();
        }
        @Override public String[] selectedRequestIds() { return selectedRequestIds.clone(); }
        public int greedyRemainingCapacity() { return capacity - greedyWeight; }
        public int optimalRemainingCapacity() { return capacity - selectedWeight; }
    }

    record TestSummary(
            boolean available,
            int tests,
            int failures,
            int errors,
            int skipped,
            String message) {
    }
}
