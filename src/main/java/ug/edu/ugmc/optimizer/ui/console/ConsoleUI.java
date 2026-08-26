package ug.edu.ugmc.optimizer.ui.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import ug.edu.ugmc.optimizer.application.services.OptimizerService;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.CompletionOutcome;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.CompletionResult;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.InitializationResult;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.MstMethod;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.OptimizationResult;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.PathResult;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.QueueView;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.RequestView;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.SearchMethod;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.SearchResult;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.SortMethod;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.SortResult;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.TestSummary;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.TraversalMethod;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.TraversalResult;

/**
 * Examiner-facing console menu, input validation, and result formatting.
 * Concrete persistence, custom structures, and algorithms are coordinated by
 * {@link OptimizerService}, not by the UI.
 */
public final class ConsoleUI {

    // (22271087 % 5) + 3
    private static final int MAX_INVALID_ATTEMPTS = (22271087 % 5) + 3;
    private static final int MIN_MENU_CHOICE = 0;
    private static final int MAX_MENU_CHOICE = 13;

    private final OptimizerService service;
    private final Scanner scanner;
    private final PrintStream output;

    /** Creates an interactive UI over the process input and output streams. */
    public ConsoleUI(OptimizerService service) {
        this(service, System.in, System.out);
    }

    /** Creates a UI with injectable streams for scripted demonstrations/tests. */
    public ConsoleUI(OptimizerService service, InputStream input, PrintStream output) {
        if (service == null || input == null || output == null) {
            throw new IllegalArgumentException("Service, input, and output are required.");
        }
        this.service = service;
        this.scanner = new Scanner(input);
        this.output = output;
    }

    /** Runs until the examiner exits, input closes, or five invalid choices occur. */
    public void run() {
        int invalidAttempts = 0;
        boolean running = true;

        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                output.println();
                output.println("Input closed. Exiting system.");
                break;
            }

            String menuInput = scanner.nextLine().trim();
            if (menuInput.isEmpty()) {
                continue;
            }

            int choice;
            try {
                choice = Integer.parseInt(menuInput);
            } catch (NumberFormatException exception) {
                invalidAttempts = recordInvalidAttempt(
                        invalidAttempts, "Invalid non-numeric option: " + menuInput + ".");
                if (invalidAttempts >= MAX_INVALID_ATTEMPTS) {
                    break;
                }
                continue;
            }

            if (choice < MIN_MENU_CHOICE || choice > MAX_MENU_CHOICE) {
                invalidAttempts = recordInvalidAttempt(
                        invalidAttempts,
                        "Invalid numeric option: " + choice + ". Choose a number from the menu.");
                if (invalidAttempts >= MAX_INVALID_ATTEMPTS) {
                    break;
                }
                continue;
            }

            invalidAttempts = 0;
            if (choice == 0) {
                output.println("Exiting system.");
                running = false;
                continue;
            }

            try {
                dispatch(choice);
            } catch (InputClosedException exception) {
                output.println();
                output.println("Input closed. Exiting system.");
                running = false;
            } catch (Exception exception) {
                output.println("Operation failed: " + safeMessage(exception));
                try {
                    waitForMainMenu();
                } catch (InputClosedException inputClosed) {
                    output.println();
                    output.println("Input closed. Exiting system.");
                    running = false;
                }
            }
        }
    }

    private int recordInvalidAttempt(int currentAttempts, String message) {
        int nextAttempt = currentAttempts + 1;
        output.println(message);
        output.println("Invalid input. Attempt " + nextAttempt
                + " of " + MAX_INVALID_ATTEMPTS + ".");
        if (nextAttempt >= MAX_INVALID_ATTEMPTS) {
            output.println("Too many invalid attempts. Terminating system.");
        }
        return nextAttempt;
    }

    private void dispatch(int choice) {
        switch (choice) {
            case 1 -> runRequestLookupFeature();
            case 2 -> runOneShotFeature(
                    "PENDING FIFO TRIAGE QUEUE", this::handlePendingQueueView);
            case 3 -> runRepeatableFeature(
                    "GRAPH TRAVERSAL", "run another traversal", this::handleGraphTraversal);
            case 4 -> runRepeatableFeature(
                    "MINIMUM SPANNING TREE", "run another MST", this::handleMstCalculation);
            case 5 -> runOneShotFeature(
                    "DATABASE RELOAD", this::handleDatabaseInitialization);
            case 6 -> runOneShotFeature(
                    "PRIORITY DISPATCH", this::handlePriorityDispatch);
            case 7 -> runRepeatableFeature(
                    "REQUEST SEARCH", "search again", this::handleSearch);
            case 8 -> runRepeatableFeature(
                    "REQUEST URGENCY SORT", "run another sort", this::handleSort);
            case 9 -> runRepeatableFeature(
                    "SHORTEST PATH - DIJKSTRA", "find another route", this::handleShortestPath);
            case 10 -> runRepeatableFeature(
                    "GREEDY / DP OPTIMISATION", "try another capacity", this::handleOptimization);
            case 11 -> runOneShotFeature(
                    "LATEST MAVEN TEST SUMMARY", this::handleTestSummary);
            case 12 -> runOneShotFeature(
                    "PERFORMANCE EXPERIMENTS", this::handleExperiments);
            case 13 -> runCompletionFeature();
            default -> throw new IllegalArgumentException("Unsupported menu choice: " + choice);
        }
    }

    private void runRepeatableFeature(
            String heading, String repeatAction, FeatureAction operation) {
        printFeatureHeading(heading);
        boolean repeat = true;
        while (repeat) {
            try {
                operation.run();
            } catch (InputClosedException exception) {
                throw exception;
            } catch (Exception exception) {
                output.println("Operation failed: " + safeMessage(exception));
            }
            repeat = promptToRepeatOrReturn(repeatAction);
        }
    }

    private void runOneShotFeature(String heading, FeatureAction operation) {
        printFeatureHeading(heading);
        try {
            operation.run();
        } catch (InputClosedException exception) {
            throw exception;
        } catch (Exception exception) {
            output.println("Operation failed: " + safeMessage(exception));
        }
        waitForMainMenu();
    }

    private void runRequestLookupFeature() {
        printFeatureHeading("REQUEST ID LOOKUP");
        boolean firstPrompt = true;
        while (true) {
            output.print(firstPrompt
                    ? "Enter request ID or B to return: "
                    : "Enter another request ID or B to return: ");
            String requestId = readLine();
            if (requestId.equalsIgnoreCase("B")) {
                output.println();
                return;
            }
            if (requestId.isEmpty()) {
                output.println("Request ID cannot be blank.");
                firstPrompt = false;
                continue;
            }

            RequestView result = service.findRequest(requestId);
            if (result == null) {
                output.println("No record found for ID: " + requestId);
                output.println("Expected request-ID format is similar to REQ-001.");
            } else {
                printRequestDetails("Request found", result);
            }
            output.println();
            firstPrompt = false;
        }
    }

    private void runCompletionFeature() {
        printFeatureHeading("COMPLETE REQUEST");
        while (true) {
            output.print("Enter in-progress request ID or B to return: ");
            String requestId = readLine();
            if (requestId.equalsIgnoreCase("B")) {
                output.println();
                return;
            }
            if (requestId.isEmpty()) {
                output.println("Request ID cannot be blank.");
                continue;
            }

            try {
                CompletionResult result = service.completeRequest(requestId);
                if (result.outcome() == CompletionOutcome.NOT_FOUND) {
                    output.println("No record found for ID: " + requestId);
                    continue;
                }
                if (result.outcome() == CompletionOutcome.MUST_DISPATCH_FIRST) {
                    output.println("Request " + result.request().id()
                            + " is Pending and must be dispatched first.");
                    continue;
                }
                if (result.outcome() == CompletionOutcome.ALREADY_COMPLETED) {
                    output.println("Request " + result.request().id() + " is already Completed.");
                    continue;
                }
                output.println();
                output.println("Request " + result.request().id() + " marked Completed.");
                output.println("Status saved to SQLite.");
                waitForMainMenu();
                return;
            } catch (Exception exception) {
                output.println("Operation failed: " + safeMessage(exception));
            }
        }
    }

    private void printFeatureHeading(String heading) {
        output.println();
        output.println("=== " + heading + " ===");
        output.println();
    }

    private boolean promptToRepeatOrReturn(String repeatAction) {
        while (true) {
            output.println();
            output.println("Press ENTER to " + repeatAction);
            output.print("or B to return to the main menu: ");
            String response = readLine();
            if (response.isEmpty()) {
                output.println();
                return true;
            }
            if (response.equalsIgnoreCase("B")) {
                output.println();
                return false;
            }
            output.println();
            output.println("Choose ENTER to continue or B to return to the main menu.");
        }
    }

    private void waitForMainMenu() {
        while (true) {
            output.println();
            output.print("Press ENTER to return to the main menu...");
            String response = readLine();
            if (response.isEmpty()) {
                output.println();
                return;
            }
            output.println();
            output.println("Press ENTER without typing any other characters.");
        }
    }

    private void printMenu() {
        output.println();
        output.println("=== UGMC Smart Service Operations Optimizer ===");
        output.println("1. Request ID lookup");
        output.println("2. View pending FIFO triage queue");
        output.println("3. Run graph traversal (BFS/DFS)");
        output.println("4. Show MST edges and cost (Prim/Kruskal)");
        output.println("5. Reload persistent database state");
        output.println("6. Dispatch next priority request");
        output.println("7. Search request numbers (linear/binary)");
        output.println("8. Sort request urgency values");
        output.println("9. Find shortest path (Dijkstra)");
        output.println("10. Compare greedy and DP allocation");
        output.println("11. Display latest Maven test summary");
        output.println("12. Run/export performance experiments");
        output.println("13. Mark in-progress request completed");
        output.println("0. Exit");
        output.print("Choose an option: ");
    }

    private void handlePendingQueueView() {
        QueueView queue = service.viewPendingQueue();
        output.println("Pending requests: " + queue.size() + " / " + queue.capacity());
        if (queue.nextRequest() == null) {
            output.println("Pending FIFO triage queue is empty.");
        } else {
            printRequestDetails("Next request by FIFO arrival order", queue.nextRequest());
            output.println("FIFO means arrival-order processing, not highest urgency.");
        }
    }

    private void handleGraphTraversal() {
        String startNode = readToken("Enter starting location ID: ");
        String type = readToken("Traversal type - B for BFS, D for DFS: ");
        TraversalMethod method;
        if (type.equalsIgnoreCase("B")) {
            method = TraversalMethod.BFS;
        } else if (type.equalsIgnoreCase("D")) {
            method = TraversalMethod.DFS;
        } else {
            output.println("Unrecognized traversal type. Choose B or D.");
            return;
        }

        TraversalResult result = service.traverse(startNode, method);
        output.println(result.method() + " order from " + startNode + ":");
        printSequence(result.locations(), " -> ");
    }

    private void handleMstCalculation() {
        String algorithm = readToken("MST algorithm - P for Prim, K for Kruskal: ");
        MstMethod method;
        if (algorithm.equalsIgnoreCase("P")) {
            method = MstMethod.PRIM;
        } else if (algorithm.equalsIgnoreCase("K")) {
            method = MstMethod.KRUSKAL;
        } else {
            output.println("Unrecognized algorithm. Choose P or K.");
            return;
        }

        OptimizerService.MstResult result = service.minimumSpanningTree(method);
        if (!result.connected()) {
            output.println("Graph is disconnected - no spanning tree exists.");
            return;
        }
        output.println(result.method() + " selected edges:");
        for (String edge : result.edges()) {
            output.println("  " + edge);
        }
        output.println("Total MST cost: " + result.totalCost());
    }

    private void handleDatabaseInitialization() throws Exception {
        InitializationResult result = service.initializeData();
        output.println("Persistent database state reloaded successfully.");
        output.println();
        printField("Total requests:", result.requestCount());
        printField("Pending:", result.pendingCount());
        printField("In Progress:", result.inProgressCount());
        printField("Completed:", result.completedCount());
        printField("Pending FIFO entries:", result.pendingCount());
        printField("Loaded locations:", result.locationCount());
    }

    private void handlePriorityDispatch() throws Exception {
        RequestView request = service.dispatchNextPriority();
        if (request == null) {
            output.println("No Pending request is available for priority dispatch.");
        } else {
            printRequestDetails("Highest-priority pending request dispatched", request);
            output.println("Status saved to SQLite.");
        }
    }

    private void handleSearch() {
        Integer requestNumber = readInteger("Enter numeric request number (for REQ-nnn): ");
        if (requestNumber == null) {
            return;
        }
        String type = readToken("Search type - L for linear, B for binary: ");
        SearchMethod method;
        if (type.equalsIgnoreCase("L")) {
            method = SearchMethod.LINEAR;
        } else if (type.equalsIgnoreCase("B")) {
            method = SearchMethod.BINARY;
        } else {
            output.println("Unrecognized search type. Choose L or B.");
            return;
        }

        SearchResult result = service.searchRequestNumber(requestNumber, method);
        if (result.found()) {
            output.println(result.method() + " search found request number "
                    + result.requestNumber() + " at index " + result.index() + ".");
        } else {
            output.println(result.method() + " search did not find request number "
                    + result.requestNumber() + ".");
        }
    }

    private void handleSort() {
        String type = readToken("Sort - S selection, I insertion, M merge, Q quick: ");
        SortMethod method;
        if (type.equalsIgnoreCase("S")) {
            method = SortMethod.SELECTION;
        } else if (type.equalsIgnoreCase("I")) {
            method = SortMethod.INSERTION;
        } else if (type.equalsIgnoreCase("M")) {
            method = SortMethod.MERGE;
        } else if (type.equalsIgnoreCase("Q")) {
            method = SortMethod.QUICK;
        } else {
            output.println("Unrecognized sort type. Choose S, I, M, or Q.");
            return;
        }

        SortResult result = service.sortUrgencies(method);
        output.println("Algorithm: " + result.method());
        output.println("Requests sorted: " + result.totalCount());
        output.println("Order: ascending (1 = lowest urgency, 5 = highest)");
        output.println();
        output.println("Before sample:");
        printIntegers(result.beforeSample());
        output.println();
        output.println("Urgency distribution:");
        int[] distribution = result.distribution();
        for (int urgency = 1; urgency <= distribution.length; urgency++) {
            output.println(urgency + ": " + distribution[urgency - 1]);
        }
        output.println();
        output.println("Lowest " + result.lowestSample().length + " after sort:");
        printIntegers(result.lowestSample());
        output.println();
        output.println("Highest " + result.highestSample().length + " after sort:");
        printIntegers(result.highestSample());
    }

    private void handleShortestPath() {
        String start = readToken("Enter starting location ID: ");
        String target = readToken("Enter target location ID: ");
        PathResult result = service.shortestPath(start, target);
        if (!result.reachable()) {
            output.println("No route exists between " + start + " and " + target + ".");
            return;
        }
        output.println("Shortest-path distance: " + result.distance());
        output.print("Route: ");
        printSequence(result.route(), " -> ");
    }

    private void handleOptimization() {
        Integer capacity = readInteger("Enter resource capacity: ");
        if (capacity == null) {
            return;
        }
        OptimizationResult result = service.optimizeRequests(capacity);
        output.println("Resource capacity: " + result.capacity());
        output.println("Scores are unitless modelling values.");
        output.println();
        output.println("GREEDY");
        output.print("Selected requests: ");
        printSequence(result.greedySelectedRequestIds(), ", ");
        output.println("Resource used:      " + result.greedyWeight()
                + " / " + result.capacity());
        output.println("Remaining capacity: " + result.greedyRemainingCapacity());
        output.println("Total benefit:      " + result.greedyValue());
        output.println();
        output.println("DYNAMIC PROGRAMMING");
        output.print("Selected requests: ");
        printSequence(result.selectedRequestIds(), ", ");
        output.println("Resource used:      " + result.selectedWeight()
                + " / " + result.capacity());
        output.println("Remaining capacity: " + result.optimalRemainingCapacity());
        output.println("Total benefit:      " + result.optimalValue());
        output.println();
        output.println("DP improvement over Greedy: +"
                + (result.optimalValue() - result.greedyValue()) + " benefit points");
        output.println();
        output.println("Note:");
        output.println("This feature selects a set of Pending requests under limited resources.");
        output.println("It does not determine dispatch order.");
    }

    private void handleTestSummary() {
        TestSummary summary = service.latestTestSummary();
        if (!summary.available()) {
            output.println(summary.message());
            return;
        }
        output.println(summary.message() + ": tests=" + summary.tests()
                + ", failures=" + summary.failures()
                + ", errors=" + summary.errors()
                + ", skipped=" + summary.skipped());
    }

    private void handleExperiments() throws Exception {
        Integer trials = readInteger("Number of measured trials: ");
        if (trials == null) {
            return;
        }
        output.println("Running performance experiments...");
        service.runExperiments(trials);
        output.println("Performance results exported successfully.");
    }

    private String readToken(String prompt) {
        output.print(prompt);
        return readLine();
    }

    private Integer readInteger(String prompt) {
        output.print(prompt);
        String value = readLine();
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            String received = value.isEmpty() ? "(empty)" : value;
            output.println("Expected a whole number but received: " + received + ".");
            return null;
        }
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine().trim();
    }

    private void printRequestDetails(String label, RequestView request) {
        output.println(label + ":");
        output.println("----------------------------------------");
        printField("Request ID:", request.id());
        printField("Patient label (synthetic):", request.patientLabel());
        printField("Category:", request.category());
        printField("Source location:", request.sourceLocation());
        printField("Destination location:", request.destinationLocation());
        printField("Urgency level:", request.urgency());
        printField("Status:", request.status());
        printField("Submitted:", request.submitted());
        printField("Deadline:", request.deadline());
        printField("Resource cost score:", request.weight());
        printField("Optimisation benefit score:", request.value());
        output.println("----------------------------------------");
    }

    private void printField(String label, Object value) {
        String text = value == null || value.toString().isBlank()
                ? "(not recorded)"
                : value.toString();
        output.printf("  %-29s %s%n", label, text);
    }

    private void printSequence(String[] values, String separator) {
        if (values.length == 0) {
            output.println("(none)");
            return;
        }
        for (int index = 0; index < values.length; index++) {
            output.print(values[index]);
            if (index < values.length - 1) {
                output.print(separator);
            }
        }
        output.println();
    }

    private void printIntegers(int[] values) {
        for (int index = 0; index < values.length; index++) {
            output.print(values[index]);
            if (index < values.length - 1) {
                output.print(", ");
            }
        }
        output.println();
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    @FunctionalInterface
    private interface FeatureAction {
        void run() throws Exception;
    }

    private static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
