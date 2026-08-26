package ug.edu.ugmc.optimizer.application.services;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import ug.edu.ugmc.optimizer.algorithms.graph.GraphTraversal;
import ug.edu.ugmc.optimizer.algorithms.graph.PathFinder;
import ug.edu.ugmc.optimizer.algorithms.optimization.DPOptimizer;
import ug.edu.ugmc.optimizer.algorithms.optimization.GreedyOptimizer;
import ug.edu.ugmc.optimizer.algorithms.optimization.GreedyOptimizer.GreedyResult;
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
import ug.edu.ugmc.optimizer.models.ServiceRequest.Status;

/**
 * Minimal application coordinator for examiner-facing use cases. Algorithms
 * and custom structures remain in their assessed packages; this class only
 * prepares inputs, invokes their public APIs, and maps results for the UI.
 */
public final class OptimizerApplicationService implements OptimizerService {

    private static final int SORT_SAMPLE_LIMIT = 10;

    private final OptimizerRepository repository;
    private final ExperimentGateway experimentGateway;
    private final Path testReportDirectory;

    private CustomHashTable<String, ServiceRequest> requestLookup;
    private CircularQueue<ServiceRequest> pendingQueue;
    private CustomPriorityQueue priorityQueue;
    private DynamicArray<ServiceRequest> requests;
    private DynamicArray<ServiceRequest> pendingRequests;
    private CustomGraph graph;
    private int requestCount;
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
        int loadedRequestCount = repository.countServiceRequests();
        int locationCount = repository.countLocations();
        if (loadedRequestCount <= 0 || locationCount <= 0) {
            throw new IllegalStateException(
                    "The database must contain service requests and graph locations.");
        }

        requestCount = loadedRequestCount;
        reloadRequestState();
        CustomGraph loadedGraph = new CustomGraph(locationCount);
        repository.loadGraph(loadedGraph);
        if (loadedGraph.getNumNodes() != locationCount) {
            throw new IllegalStateException("Database loader returned an incomplete data set.");
        }
        graph = loadedGraph;

        int pendingCount = repository.countServiceRequestsByStatus(Status.PENDING);
        int inProgressCount = repository.countServiceRequestsByStatus(Status.IN_PROGRESS);
        int completedCount = repository.countServiceRequestsByStatus(Status.COMPLETED);
        if (pendingCount + inProgressCount + completedCount != requestCount
                || pendingQueue.size() != pendingCount) {
            throw new IllegalStateException("Persistent request status counts are inconsistent.");
        }
        initialized = true;
        return new InitializationResult(
                requestCount, pendingCount, inProgressCount, completedCount,
                pendingQueue.getCapacity(), locationCount);
    }

    @Override
    public RequestView findRequest(String requestId) {
        requireInitialized();
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be blank.");
        }
        return toView(requestLookup.get(normalizeRequestId(requestId)));
    }

    @Override
    public QueueView viewPendingQueue() {
        requireInitialized();
        RequestView next = pendingQueue.isEmpty() ? null : toView(pendingQueue.peek());
        return new QueueView(pendingQueue.size(), pendingQueue.getCapacity(), next);
    }

    @Override
    public RequestView dispatchNextPriority() throws Exception {
        requireInitialized();
        String requestId;
        try {
            requestId = priorityQueue.extractHighestPriority();
        } catch (IllegalStateException emptyQueue) {
            return null;
        }
        boolean persisted = false;
        try {
            persisted = repository.updateRequestStatus(
                    requestId, Status.PENDING, Status.IN_PROGRESS);
            if (!persisted) {
                throw new IllegalStateException(
                        "Request is no longer pending and was not dispatched: " + requestId);
            }
            reloadRequestState();
            return toView(requestLookup.get(requestId));
        } catch (Exception exception) {
            if (persisted) {
                rollbackStatus(
                        exception, requestId, Status.IN_PROGRESS, Status.PENDING);
            }
            restoreRequestState(exception);
            throw exception;
        }
    }

    @Override
    public CompletionResult completeRequest(String requestId) throws Exception {
        requireInitialized();
        String normalizedId = normalizeRequestId(requestId);
        ServiceRequest current = requestLookup.get(normalizedId);
        if (current == null) {
            return new CompletionResult(CompletionOutcome.NOT_FOUND, null);
        }
        if (current.getStatus() == Status.PENDING) {
            return new CompletionResult(CompletionOutcome.MUST_DISPATCH_FIRST, toView(current));
        }
        if (current.getStatus() == Status.COMPLETED) {
            return new CompletionResult(CompletionOutcome.ALREADY_COMPLETED, toView(current));
        }

        boolean persisted = false;
        try {
            persisted = repository.updateRequestStatus(
                    normalizedId, Status.IN_PROGRESS, Status.COMPLETED);
            if (!persisted) {
                throw new IllegalStateException(
                        "Request status changed before completion: " + normalizedId);
            }
            reloadRequestState();
            return new CompletionResult(
                    CompletionOutcome.COMPLETED, toView(requestLookup.get(normalizedId)));
        } catch (Exception exception) {
            if (persisted) {
                rollbackStatus(
                        exception, normalizedId, Status.COMPLETED, Status.IN_PROGRESS);
            }
            restoreRequestState(exception);
            throw exception;
        }
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
        int[] distribution = new int[5];
        for (int index = 0; index < requests.size(); index++) {
            int urgency = requests.get(index).getUrgency();
            urgencies.insert(urgency);
            distribution[urgency - 1]++;
        }

        int sampleSize = Math.min(SORT_SAMPLE_LIMIT, urgencies.size());
        int[] beforeSample = new int[sampleSize];
        for (int index = 0; index < sampleSize; index++) {
            beforeSample[index] = urgencies.get(index);
        }

        switch (method) {
            case SELECTION -> SelectionSort.sort(urgencies);
            case INSERTION -> InsertionSort.sort(urgencies);
            case MERGE -> MergeSort.sort(urgencies);
            case QUICK -> QuickSort.sort(urgencies);
            default -> throw new IllegalArgumentException("Unsupported sort method: " + method);
        }

        int[] lowestSample = new int[sampleSize];
        int[] highestSample = new int[sampleSize];
        for (int index = 0; index < sampleSize; index++) {
            lowestSample[index] = urgencies.get(index);
            highestSample[index] = urgencies.get(urgencies.size() - sampleSize + index);
        }
        return new SortResult(
                method, urgencies.size(), beforeSample,
                lowestSample, highestSample, distribution);
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

        int[] weights = new int[pendingRequests.size()];
        int[] values = new int[pendingRequests.size()];
        for (int index = 0; index < pendingRequests.size(); index++) {
            ServiceRequest request = pendingRequests.get(index);
            weights[index] = request.getWeight();
            values[index] = request.getValue();
        }

        GreedyResult greedy = GreedyOptimizer.solveGreedyKnapsack(weights, values, capacity);
        DPOptimizer.KnapsackResult optimal =
                DPOptimizer.solveKnapsack(weights, values, capacity);
        int[] greedyIndices = greedy.getSelectedIndices();
        String[] greedyIds = new String[greedyIndices.length];
        for (int index = 0; index < greedyIndices.length; index++) {
            greedyIds[index] = pendingRequests.get(greedyIndices[index]).getId();
        }
        int[] selectedIndices = optimal.getSelectedIndices();
        String[] selectedIds = new String[selectedIndices.length];
        for (int index = 0; index < selectedIndices.length; index++) {
            selectedIds[index] = pendingRequests.get(selectedIndices[index]).getId();
        }
        return new OptimizationResult(
                capacity,
                greedy.getTotalValue(),
                greedy.getTotalWeight(),
                greedyIds,
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

    private void reloadRequestState() throws Exception {
        CustomHashTable<String, ServiceRequest> loadedLookup = new CustomHashTable<>();
        CircularQueue<ServiceRequest> loadedQueue = new CircularQueue<>(requestCount);
        DynamicArray<ServiceRequest> loadedRequests = new DynamicArray<>(requestCount);
        repository.loadServiceRequests(loadedLookup, loadedQueue, loadedRequests);
        if (loadedLookup.getSize() != requestCount || loadedRequests.size() != requestCount) {
            throw new IllegalStateException("Database loader returned incomplete request records.");
        }

        DynamicArray<ServiceRequest> loadedPending =
                new DynamicArray<>(Math.max(1, loadedQueue.size()));
        CustomPriorityQueue loadedPriorityQueue = new CustomPriorityQueue();
        int pendingCount = loadedQueue.size();
        for (int index = 0; index < pendingCount; index++) {
            ServiceRequest request = loadedQueue.dequeue();
            loadedQueue.enqueue(request);
            loadedPending.insert(request);
            // The custom queue is a min-heap; negating urgency dispatches 5 first.
            loadedPriorityQueue.insert(request.getId(), -request.getUrgency());
        }

        requestLookup = loadedLookup;
        pendingQueue = loadedQueue;
        priorityQueue = loadedPriorityQueue;
        requests = loadedRequests;
        pendingRequests = loadedPending;
    }

    private void restoreRequestState(Exception originalFailure) {
        try {
            reloadRequestState();
        } catch (Exception reloadFailure) {
            originalFailure.addSuppressed(reloadFailure);
        }
    }

    private void rollbackStatus(
            Exception originalFailure,
            String requestId,
            Status currentStatus,
            Status restoredStatus) {
        try {
            boolean rolledBack = repository.updateRequestStatus(
                    requestId, currentStatus, restoredStatus);
            if (!rolledBack) {
                originalFailure.addSuppressed(new IllegalStateException(
                        "Unable to roll back persistent status for " + requestId + "."));
            }
        } catch (Exception rollbackFailure) {
            originalFailure.addSuppressed(rollbackFailure);
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
                request.getId(),
                request.getPatientLabel(),
                request.getCategory(),
                request.getSourceLocation(),
                request.getDestinationLocation(),
                request.getUrgency(),
                request.getStatus().databaseValue(),
                request.getSubmitted(),
                request.getDeadline(),
                request.getWeight(),
                request.getValue());
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be blank.");
        }
        return requestId.trim().toUpperCase(Locale.ROOT);
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
