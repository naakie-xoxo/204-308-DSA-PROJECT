package ug.edu.ugmc.optimizer.application.services;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import ug.edu.ugmc.optimizer.algorithms.graph.GraphTraversal;
import ug.edu.ugmc.optimizer.algorithms.graph.PathFinder;
import ug.edu.ugmc.optimizer.algorithms.optimization.DPOptimizer;
import ug.edu.ugmc.optimizer.algorithms.optimization.GreedyOptimizer;
import ug.edu.ugmc.optimizer.algorithms.search.CustomSearch;
import ug.edu.ugmc.optimizer.algorithms.sort.InsertionSort;
import ug.edu.ugmc.optimizer.algorithms.sort.MergeSort;
import ug.edu.ugmc.optimizer.algorithms.sort.QuickSort;
import ug.edu.ugmc.optimizer.algorithms.sort.SelectionSort;
import ug.edu.ugmc.optimizer.application.ports.ExperimentGateway;
import ug.edu.ugmc.optimizer.application.ports.OptimizerRepository;
import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.heap.CustomPriorityQueue;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/**
 * Minimal application coordinator for examiner-facing use cases. Algorithms
 * and custom structures remain in their assessed packages; this class only
 * prepares inputs, invokes their public APIs, and maps results for the UI.
 */
public final class OptimizerApplicationService implements OptimizerService {

    private static final int SORT_SAMPLE_LIMIT = 20;

    private final OptimizerRepository repository;
    private final ExperimentGateway experimentGateway;
    private final Path testReportDirectory;

    private CustomHashTable<String, ServiceRequest> requestLookup;
    private CircularQueue<ServiceRequest> pendingQueue;
    private CustomPriorityQueue priorityQueue;
    private DynamicArray<ServiceRequest> requests;
    private CustomGraph graph;
    private boolean initialized;

    public OptimizerApplicationService(
            OptimizerRepository repository,
            ExperimentGateway experimentGateway,
            Path testReportDirectory) {
        if (repository == null || experimentGateway == null || testReportDirectory == null) {
            throw new IllegalArgumentException(
                    "Repository, experiment gateway, and test-report path are required.");
        }
        this.repository = repository;
        this.experimentGateway = experimentGateway;
        this.testReportDirectory = testReportDirectory;
    }

    @Override
    public InitializationResult initializeData() throws Exception {
        repository.initialize();
        int requestCount = repository.countServiceRequests();
        int locationCount = repository.countLocations();
        if (requestCount <= 0 || locationCount <= 0) {
            throw new IllegalStateException(
                    "The database must contain service requests and graph locations.");
        }

        CustomHashTable<String, ServiceRequest> loadedLookup = new CustomHashTable<>();
        CircularQueue<ServiceRequest> loadedQueue = new CircularQueue<>(requestCount);
        CustomGraph loadedGraph = new CustomGraph(locationCount);
        repository.loadServiceRequests(loadedLookup, loadedQueue);
        repository.loadGraph(loadedGraph);

        if (loadedQueue.size() != requestCount || loadedGraph.getNumNodes() != locationCount) {
            throw new IllegalStateException("Database loader returned an incomplete data set.");
        }

        DynamicArray<ServiceRequest> loadedRequests = new DynamicArray<>(requestCount);
        CustomPriorityQueue loadedPriorityQueue = new CustomPriorityQueue();
        for (int index = 0; index < requestCount; index++) {
            ServiceRequest request = loadedQueue.dequeue();
            loadedQueue.enqueue(request);
            loadedRequests.insert(request);
            // CustomPriorityQueue is a min-priority queue; negate urgency so 5
            // is dispatched before 4, and so on.
            loadedPriorityQueue.insert(request.getId(), -request.getUrgency());
        }

        requestLookup = loadedLookup;
        pendingQueue = loadedQueue;
        priorityQueue = loadedPriorityQueue;
        requests = loadedRequests;
        graph = loadedGraph;
        initialized = true;

        return new InitializationResult(requestCount, loadedQueue.getCapacity(), locationCount);
    }

    @Override
    public RequestView findRequest(String requestId) {
        requireInitialized();
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be blank.");
        }
        return toView(requestLookup.get(requestId.trim()));
    }

    @Override
    public QueueView viewPendingQueue() {
        requireInitialized();
        RequestView next = pendingQueue.isEmpty() ? null : toView(pendingQueue.peek());
        return new QueueView(pendingQueue.size(), pendingQueue.getCapacity(), next);
    }

    @Override
    public RequestView dispatchNextPriority() {
        requireInitialized();
        String requestId;
        try {
            requestId = priorityQueue.extractHighestPriority();
        } catch (IllegalStateException emptyQueue) {
            return null;
        }
        RequestView dispatched = toView(requestLookup.get(requestId));
        removeFromPendingQueue(requestId);
        return dispatched;
    }

    @Override
    public TraversalResult traverse(String startLocation, TraversalMethod method) {
        requireInitialized();
        requireMethod(method);
        DynamicArray<String> order = method == TraversalMethod.BFS
                ? GraphTraversal.bfs(graph, startLocation)
                : GraphTraversal.dfs(graph, startLocation);
        String[] locations = new String[order.size()];
        for (int index = 0; index < order.size(); index++) {
            locations[index] = order.get(index);
        }
        return new TraversalResult(method, locations);
    }

    @Override
    public SearchResult searchRequestNumber(int requestNumber, SearchMethod method) {
        requireInitialized();
        requireMethod(method);
        if (requestNumber < 0) {
            throw new IllegalArgumentException("Request number cannot be negative.");
        }

        DynamicArray<Integer> numbers = requestNumbers();
        int index;
        if (method == SearchMethod.BINARY) {
            MergeSort.sort(numbers);
            index = CustomSearch.binarySearchPresorted(numbers, requestNumber);
        } else {
            index = CustomSearch.linearSearch(numbers, requestNumber);
        }
        return new SearchResult(method, requestNumber, index);
    }

    @Override
    public SortResult sortUrgencies(SortMethod method) {
        requireInitialized();
        requireMethod(method);
        DynamicArray<Integer> urgencies = new DynamicArray<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            urgencies.insert(requests.get(index).getUrgency());
        }

        switch (method) {
            case SELECTION -> SelectionSort.sort(urgencies);
            case INSERTION -> InsertionSort.sort(urgencies);
            case MERGE -> MergeSort.sort(urgencies);
            case QUICK -> QuickSort.sort(urgencies);
            default -> throw new IllegalArgumentException("Unsupported sort method: " + method);
        }

        int sampleSize = Math.min(SORT_SAMPLE_LIMIT, urgencies.size());
        int[] sample = new int[sampleSize];
        for (int index = 0; index < sampleSize; index++) {
            sample[index] = urgencies.get(index);
        }
        return new SortResult(method, urgencies.size(), sample);
    }

    @Override
    public PathResult shortestPath(String startLocation, String targetLocation) {
        requireInitialized();
        PathFinder.ShortestPathResult result =
                PathFinder.dijkstraWithRoute(graph, startLocation, targetLocation);
        return new PathResult(result.getDistance() >= 0, result.getDistance(), result.getRoute());
    }

    @Override
    public MstResult minimumSpanningTree(MstMethod method) {
        requireInitialized();
        requireMethod(method);
        PathFinder.MstResult result = method == MstMethod.PRIM
                ? PathFinder.primMST(graph)
                : PathFinder.kruskalMST(graph);
        PathFinder.MstEdge[] selectedEdges = result.getEdges();
        String[] edges = new String[selectedEdges.length];
        for (int index = 0; index < selectedEdges.length; index++) {
            PathFinder.MstEdge edge = selectedEdges[index];
            edges[index] = edge.getSource() + " -- " + edge.getDestination()
                    + " (weight " + edge.getWeight() + ")";
        }
        return new MstResult(method, result.isConnected(), result.getTotalCost(), edges);
    }

    @Override
    public OptimizationResult optimizeRequests(int capacity) {
        requireInitialized();
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative.");
        }

        int[] weights = new int[requests.size()];
        int[] values = new int[requests.size()];
        for (int index = 0; index < requests.size(); index++) {
            ServiceRequest request = requests.get(index);
            weights[index] = request.getWeight();
            values[index] = request.getValue();
        }

        int greedyValue = GreedyOptimizer.greedyKnapsack(weights, values, capacity);
        DPOptimizer.KnapsackResult optimal =
                DPOptimizer.solveKnapsack(weights, values, capacity);
        int[] selectedIndices = optimal.getSelectedIndices();
        String[] selectedIds = new String[selectedIndices.length];
        for (int index = 0; index < selectedIndices.length; index++) {
            selectedIds[index] = requests.get(selectedIndices[index]).getId();
        }
        return new OptimizationResult(
                greedyValue,
                optimal.getMaximumValue(),
                optimal.getTotalWeight(),
                selectedIds);
    }

    @Override
    public TestSummary latestTestSummary() {
        if (!Files.isDirectory(testReportDirectory)) {
            return unavailableSummary("No Maven test reports found. Run mvn test first.");
        }

        int tests = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;
        int reportCount = 0;
        try (DirectoryStream<Path> reports =
                Files.newDirectoryStream(testReportDirectory, "TEST-*.xml")) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            for (Path report : reports) {
                Element suite = factory.newDocumentBuilder().parse(report.toFile())
                        .getDocumentElement();
                tests += intAttribute(suite, "tests");
                failures += intAttribute(suite, "failures");
                errors += intAttribute(suite, "errors");
                skipped += intAttribute(suite, "skipped");
                reportCount++;
            }
        } catch (Exception exception) {
            return unavailableSummary("Unable to read Maven test reports: " + safeMessage(exception));
        }

        if (reportCount == 0) {
            return unavailableSummary("No Maven test reports found. Run mvn test first.");
        }
        return new TestSummary(true, tests, failures, errors, skipped, "Latest Maven test summary");
    }

    @Override
    public void runExperiments(int trials) throws Exception {
        requireInitialized();
        if (trials <= 0) {
            throw new IllegalArgumentException("Trial count must be greater than zero.");
        }
        experimentGateway.run(trials);
    }

    private DynamicArray<Integer> requestNumbers() {
        DynamicArray<Integer> numbers = new DynamicArray<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            String requestId = requests.get(index).getId();
            int separator = requestId.lastIndexOf('-');
            String numericPart = separator >= 0 ? requestId.substring(separator + 1) : requestId;
            try {
                numbers.insert(Integer.parseInt(numericPart));
            } catch (NumberFormatException exception) {
                throw new IllegalStateException(
                        "Request ID does not end in a numeric value: " + requestId, exception);
            }
        }
        return numbers;
    }

    private void removeFromPendingQueue(String requestId) {
        int pendingCount = pendingQueue.size();
        for (int index = 0; index < pendingCount; index++) {
            ServiceRequest request = pendingQueue.dequeue();
            if (!request.getId().equals(requestId)) {
                pendingQueue.enqueue(request);
            }
        }
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Initialize or reload the database first (menu option 5).");
        }
    }

    private static void requireMethod(Object method) {
        if (method == null) {
            throw new IllegalArgumentException("An algorithm choice is required.");
        }
    }

    private static RequestView toView(ServiceRequest request) {
        return request == null ? null : new RequestView(
                request.getId(), request.getUrgency(), request.getWeight(), request.getValue());
    }

    private static int intAttribute(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    private static TestSummary unavailableSummary(String message) {
        return new TestSummary(false, 0, 0, 0, 0, message);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
