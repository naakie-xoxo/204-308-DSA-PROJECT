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

    InitializationResult initializeData() throws Exception;

    RequestView findRequest(String requestId);

    QueueView viewPendingQueue();

    RequestView dispatchNextPriority();

    TraversalResult traverse(String startLocation, TraversalMethod method);

    SearchResult searchRequestNumber(int requestNumber, SearchMethod method);

    SortResult sortUrgencies(SortMethod method);

    PathResult shortestPath(String startLocation, String targetLocation);

    MstResult minimumSpanningTree(MstMethod method);

    OptimizationResult optimizeRequests(int capacity);

    TestSummary latestTestSummary();

    void runExperiments(int trials) throws Exception;

    record InitializationResult(int requestCount, int queueCapacity, int locationCount) {
    }

    record RequestView(String id, int urgency, int weight, int value) {
    }

    record QueueView(int size, int capacity, RequestView nextRequest) {
    }

    record TraversalResult(TraversalMethod method, String[] locations) {
        public TraversalResult {
            locations = locations.clone();
        }

        @Override
        public String[] locations() {
            return locations.clone();
        }
    }

    record SearchResult(SearchMethod method, int requestNumber, int index) {
        public boolean found() {
            return index >= 0;
        }
    }

    record SortResult(SortMethod method, int totalCount, int[] sample) {
        public SortResult {
            sample = sample.clone();
        }

        @Override
        public int[] sample() {
            return sample.clone();
        }
    }

    record PathResult(boolean reachable, int distance, String[] route) {
        public PathResult {
            route = route.clone();
        }

        @Override
        public String[] route() {
            return route.clone();
        }
    }

    record MstResult(MstMethod method, boolean connected, int totalCost, String[] edges) {
        public MstResult {
            edges = edges.clone();
        }

        @Override
        public String[] edges() {
            return edges.clone();
        }
    }

    record OptimizationResult(
            int greedyValue,
            int optimalValue,
            int selectedWeight,
            String[] selectedRequestIds) {
        public OptimizationResult {
            selectedRequestIds = selectedRequestIds.clone();
        }

        @Override
        public String[] selectedRequestIds() {
            return selectedRequestIds.clone();
        }
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
