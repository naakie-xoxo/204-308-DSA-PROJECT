package ug.edu.ugmc.optimizer.ui.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import ug.edu.ugmc.optimizer.application.services.OptimizerService;
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
    private static final int MAX_MENU_CHOICE = 12;

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
            int choice;
            try {
                choice = Integer.parseInt(menuInput);
            } catch (NumberFormatException exception) {
                String invalidToken = menuInput.isEmpty() ? "(empty)" : menuInput;
                invalidAttempts = recordInvalidAttempt(
                        invalidAttempts, "Invalid non-numeric option: " + invalidToken + ".");
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
            case 1 -> runRepeatableFeature(
                    "REQUEST ID LOOKUP", "look up another request", this::handleRequestLookup);
            case 2 -> runOneShotFeature(
                    "PENDING FIFO TRIAGE QUEUE", this::handlePendingQueueView);
            case 3 -> runRepeatableFeature(
                    "GRAPH TRAVERSAL", "run another traversal", this::handleGraphTraversal);
            case 4 -> runRepeatableFeature(
                    "MINIMUM SPANNING TREE", "run another MST", this::handleMstCalculation);
            case 5 -> runOneShotFeature(
                    "DATABASE INITIALIZATION", this::handleDatabaseInitialization);
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
        output.println("5. Initialize/reload database");
        output.println("6. Dispatch next priority request");
        output.println("7. Search request numbers (linear/binary)");
        output.println("8. Sort request urgency values");
        output.println("9. Find shortest path (Dijkstra)");
        output.println("10. Compare greedy and DP allocation");
        output.println("11. Display latest Maven test summary");
        output.println("12. Run/export performance experiments");
        output.println("0. Exit");
        output.print("Choose an option: ");
    }

    private void handleRequestLookup() {
        String requestId = readToken("Enter request ID: ");
        RequestView result = service.findRequest(requestId);
        if (result == null) {
            output.println("No record found for ID: " + requestId);
        } else {
            printRequest("Record found", result);
        }
    }

    private void handlePendingQueueView() {
        QueueView queue = service.viewPendingQueue();
        output.println("Queue size: " + queue.size() + " / " + queue.capacity());
        if (queue.nextRequest() == null) {
            output.println("Triage queue is empty.");
        } else {
            printRequest("Next FIFO request", queue.nextRequest());
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
        output.println("Database and in-memory structures ready.");
        output.println("Loaded requests: " + result.requestCount());
        output.println("Triage queue capacity: " + result.queueCapacity());
        output.println("Loaded locations: " + result.locationCount());
    }

    private void handlePriorityDispatch() {
        RequestView request = service.dispatchNextPriority();
        if (request == null) {
            output.println("Priority queue is empty.");
        } else {
            printRequest("Priority dispatch", request);
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
        int[] sample = result.sample();
        output.println(result.method() + " sorted " + result.totalCount()
                + " urgency values. First " + sample.length + ":");
        printIntegers(sample);
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
        output.println("Greedy value: " + result.greedyValue());
        output.println("DP optimal value: " + result.optimalValue());
        output.println("DP selected weight: " + result.selectedWeight());
        output.print("DP selected requests: ");
        printSequence(result.selectedRequestIds(), ", ");
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

    private void printRequest(String label, RequestView request) {
        output.println(label + " - ID: " + request.id()
                + ", Urgency: " + request.urgency()
                + ", Weight: " + request.weight()
                + ", Value: " + request.value());
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
