package ug.edu.ugmc.optimizer.ui.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.application.services.OptimizerService;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.CompletionOutcome;

class ConsoleUITest {

    private static final String MAIN_MENU_HEADING =
            "=== UGMC Smart Service Operations Optimizer ===";

    @Test
    void requestLookupStaysInsideFeatureUntilBackAndCanRunTwice() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "1\nREQ-001\nREQ-012\nB\n0\n");

        assertEquals(2, service.lookupCalls);
        assertTrue(output.contains("Request ID:                   REQ-001"));
        assertTrue(output.contains("Request ID:                   REQ-012"));
        assertTrue(output.contains("Patient label (synthetic):"));
        assertTrue(output.contains("Resource cost score:"));
        assertTrue(output.contains("Optimisation benefit score:"));
        assertTrue(output.contains("Enter another request ID or B to return:"));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));

        int firstResult = output.indexOf("Request ID:                   REQ-001");
        int secondResult = output.indexOf("Request ID:                   REQ-012");
        assertFalse(output.substring(firstResult, secondResult).contains(MAIN_MENU_HEADING));
        assertTrue(output.contains("Exiting system."));
    }

    @Test
    void malformedLookupShowsFormatGuidanceAndAcceptsTheNextIdDirectly() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "1\nREQ001\nreq-001\nB\n0\n");

        assertEquals(2, service.lookupCalls);
        assertTrue(output.contains("No record found for ID: REQ001"));
        assertTrue(output.contains("Expected request-ID format is similar to REQ-001."));
        assertTrue(output.contains("Request found:"));
        assertFalse(output.contains("Press ENTER to look up another request"));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));
    }

    @Test
    void malformedAndOutOfRangeNumericChoicesBothCountAsInvalidAttempts() {
        String output = runScript(new FakeOptimizerService(), "not-a-number\n99\n0\n");

        assertTrue(output.contains("Invalid non-numeric option: not-a-number."));
        assertTrue(output.contains("Invalid input. Attempt 1 of 5."));
        assertTrue(output.contains("Invalid numeric option: 99."));
        assertTrue(output.contains("Invalid input. Attempt 2 of 5."));
    }

    @Test
    void blankMainMenuInputIsIgnoredWithoutCountingAsInvalid() {
        String output = runScript(new FakeOptimizerService(), "\n\n\n0\n");

        assertTrue(output.contains("Exiting system."));
        assertFalse(output.contains("Invalid non-numeric option: (empty)."));
        assertFalse(output.contains("Invalid input. Attempt 1 of 5."));
        assertFalse(output.contains("Too many invalid attempts. Terminating system."));
    }

    @Test
    void blankMainMenuInputDoesNotAdvanceAnExistingInvalidCount() {
        String output = runScript(new FakeOptimizerService(), "bad\n\n\n99\n0\n");

        assertTrue(output.contains("Invalid non-numeric option: bad."));
        assertTrue(output.contains("Invalid input. Attempt 1 of 5."));
        assertTrue(output.contains("Invalid numeric option: 99."));
        assertTrue(output.contains("Invalid input. Attempt 2 of 5."));
        assertFalse(output.contains("Invalid input. Attempt 3 of 5."));
        assertFalse(output.contains("Too many invalid attempts. Terminating system."));
        assertTrue(output.contains("Exiting system."));
    }

    @Test
    void fiveMixedInvalidChoicesTerminateTheMenu() {
        String output = runScript(
                new FakeOptimizerService(), "bad\n99\nbad\n99\nbad\n");

        assertTrue(output.contains("Invalid input. Attempt 5 of 5."));
        assertTrue(output.contains("Too many invalid attempts. Terminating system."));
    }

    @Test
    void dijkstraStaysInsideFeatureAndRunsTwoRoutesBeforeBack() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(
                service, "9\nLOC002\nLOC012\n\nLOC003\nLOC004\nB\n0\n");

        assertEquals(2, service.shortestPathCalls);
        assertTrue(output.contains("=== SHORTEST PATH - DIJKSTRA ==="));
        assertTrue(output.contains("Route: LOC002 -> LOC012"));
        assertTrue(output.contains("Route: LOC003 -> LOC004"));
        assertTrue(output.contains("Press ENTER to find another route"));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));

        int firstRoute = output.indexOf("Route: LOC002 -> LOC012");
        int secondRoute = output.indexOf("Route: LOC003 -> LOC004");
        assertFalse(output.substring(firstRoute, secondRoute).contains(MAIN_MENU_HEADING));
    }

    @Test
    void oneShotQueueResultWaitsWhenInputClosesAfterTheChoice() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "2\n");

        assertEquals(1, service.queueViewCalls);
        assertTrue(output.contains("Pending requests: 1 / 300"));
        assertTrue(output.contains("Press ENTER to return to the main menu..."));
        assertEquals(1, countOccurrences(output, MAIN_MENU_HEADING));
    }

    @Test
    void enterReturnsFromOneShotQueueViewBeforeCleanExit() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "2\n\n0\n");

        assertEquals(1, service.queueViewCalls);
        assertTrue(output.contains("Pending requests: 1 / 300"));
        assertTrue(output.contains("Press ENTER to return to the main menu..."));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));
        assertTrue(output.contains("Exiting system."));
    }

    @Test
    void invalidTraversalSubtypeStaysInsideFeatureAndAllowsRetry() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "3\nLOC002\nX\n\nLOC003\nD\nB\n0\n");

        assertTrue(output.contains("Unrecognized traversal type. Choose B or D."));
        assertTrue(output.contains("DFS order from LOC003:"));
        assertEquals(1, service.traversalCalls);
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));

        int error = output.indexOf("Unrecognized traversal type. Choose B or D.");
        int retryResult = output.indexOf("DFS order from LOC003:");
        assertFalse(output.substring(error, retryResult).contains(MAIN_MENU_HEADING));
    }

    @Test
    void repeatableOperationFailureAllowsAnotherDijkstraAttempt() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(
                service, "9\nINVALID\nLOC012\n\nLOC002\nLOC012\nB\n0\n");

        assertEquals(2, service.shortestPathCalls);
        assertTrue(output.contains("Operation failed: Unknown location: INVALID"));
        assertTrue(output.contains("Route: LOC002 -> LOC012"));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));
    }

    @Test
    void oneShotOperationFailureStillWaitsBeforeReturning() {
        FakeOptimizerService service = new FakeOptimizerService();
        service.failQueueView = true;

        String output = runScript(service, "2\n");

        assertTrue(output.contains("Operation failed: Queue unavailable"));
        assertTrue(output.contains("Press ENTER to return to the main menu..."));
        assertEquals(1, countOccurrences(output, MAIN_MENU_HEADING));
    }

    @Test
    void mstOperationPrintsEverySelectedEdgeAndTheTotalCost() {
        String output = runScript(new FakeOptimizerService(), "4\nP\nB\n0\n");

        assertTrue(output.contains("PRIM selected edges:"));
        assertTrue(output.contains("LOC001 -- LOC002 (4)"));
        assertTrue(output.contains("LOC002 -- LOC003 (6)"));
        assertTrue(output.contains("Total MST cost: 10"));
    }

    @Test
    void databaseReloadShowsPersistentLifecycleCounts() {
        String output = runScript(new FakeOptimizerService(), "5\n\n0\n");

        assertTrue(output.contains("Total requests:               300"));
        assertTrue(output.contains("Pending:                      298"));
        assertTrue(output.contains("In Progress:                  1"));
        assertTrue(output.contains("Completed:                    1"));
        assertTrue(output.contains("Loaded locations:             50"));
    }

    @Test
    void dispatchPrintsFullRecordAndConfirmsPersistentStatus() {
        String output = runScript(new FakeOptimizerService(), "6\n\n0\n");

        assertTrue(output.contains("Highest-priority pending request dispatched:"));
        assertTrue(output.contains("Status:                       In Progress"));
        assertTrue(output.contains("Status saved to SQLite."));
    }

    @Test
    void completionRejectsPendingThenCompletesInProgressRequest() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "13\nREQ-002\nREQ-001\n\n0\n");

        assertEquals(2, service.completionCalls);
        assertTrue(output.contains("REQ-002 is Pending and must be dispatched first."));
        assertTrue(output.contains("Request REQ-001 marked Completed."));
        assertTrue(output.contains("Status saved to SQLite."));
    }

    @Test
    void sortOutputShowsBeforeDistributionAndBothEndsOfSortedData() {
        String output = runScript(new FakeOptimizerService(), "8\nM\nB\n0\n");

        assertTrue(output.contains("Before sample:"));
        assertTrue(output.contains("Urgency distribution:"));
        assertTrue(output.contains("Lowest 3 after sort:"));
        assertTrue(output.contains("Highest 3 after sort:"));
    }

    @Test
    void allFourSortChoicesRemainAvailableInOneFeatureSession() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "8\nS\n\nI\n\nM\n\nQ\nB\n0\n");

        assertEquals(4, service.sortCalls);
        assertTrue(output.contains("Algorithm: SELECTION"));
        assertTrue(output.contains("Algorithm: INSERTION"));
        assertTrue(output.contains("Algorithm: MERGE"));
        assertTrue(output.contains("Algorithm: QUICK"));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));
    }

    @Test
    void optimizationOutputExplainsBothSelectionsAndCapacityArithmetic() {
        String output = runScript(new FakeOptimizerService(), "10\n10\nB\n0\n");

        assertTrue(output.contains("Scores are unitless modelling values."));
        assertTrue(output.contains("GREEDY"));
        assertTrue(output.contains("DYNAMIC PROGRAMMING"));
        assertTrue(output.contains("DP improvement over Greedy:"));
        assertTrue(output.contains("It does not determine dispatch order."));
    }

    @Test
    void zeroIsAValidCleanExitWithoutCallingAnOperation() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "0\n");

        assertEquals(0, service.lookupCalls);
        assertTrue(output.contains("Exiting system."));
    }

    @Test
    void validChoiceResetsAccumulatedMainMenuInvalidAttempts() {
        String output = runScript(
                new FakeOptimizerService(), "bad\n2\n\nbad\nbad\nbad\nbad\n0\n");

        assertEquals(2, countOccurrences(output, "Invalid input. Attempt 1 of 5."));
        assertTrue(output.contains("Invalid input. Attempt 4 of 5."));
        assertFalse(output.contains("Too many invalid attempts. Terminating system."));
        assertTrue(output.contains("Exiting system."));
    }

    private static String runScript(FakeOptimizerService service, String script) {
        ByteArrayInputStream input = new ByteArrayInputStream(
                script.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);

        new ConsoleUI(service, input, output).run();

        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static int countOccurrences(String value, String target) {
        int count = 0;
        int fromIndex = 0;
        while (true) {
            int match = value.indexOf(target, fromIndex);
            if (match < 0) {
                return count;
            }
            count++;
            fromIndex = match + target.length();
        }
    }

    private static final class FakeOptimizerService implements OptimizerService {
        private int lookupCalls;
        private int queueViewCalls;
        private int traversalCalls;
        private int shortestPathCalls;
        private int completionCalls;
        private int sortCalls;
        private boolean failQueueView;

        @Override
        public InitializationResult initializeData() {
            return new InitializationResult(300, 298, 1, 1, 300, 50);
        }

        @Override
        public RequestView findRequest(String requestId) {
            lookupCalls++;
            if (requestId.equalsIgnoreCase("REQ001")) {
                return null;
            }
            return request(requestId, "Pending");
        }

        @Override
        public QueueView viewPendingQueue() {
            queueViewCalls++;
            if (failQueueView) {
                throw new IllegalStateException("Queue unavailable");
            }
            return new QueueView(1, 300, request("REQ-001", "Pending"));
        }

        @Override
        public RequestView dispatchNextPriority() {
            return request("REQ-001", "In Progress");
        }

        @Override
        public CompletionResult completeRequest(String requestId) {
            completionCalls++;
            if (requestId.equalsIgnoreCase("REQ-002")) {
                return new CompletionResult(
                        CompletionOutcome.MUST_DISPATCH_FIRST, request("REQ-002", "Pending"));
            }
            return new CompletionResult(
                    CompletionOutcome.COMPLETED, request(requestId, "Completed"));
        }

        @Override
        public TraversalResult traverse(String startLocation, TraversalMethod method) {
            traversalCalls++;
            return new TraversalResult(method, new String[] {startLocation});
        }

        @Override
        public SearchResult searchRequestNumber(int requestNumber, SearchMethod method) {
            return new SearchResult(method, requestNumber, 0);
        }

        @Override
        public SortResult sortUrgencies(SortMethod method) {
            sortCalls++;
            return new SortResult(
                    method, 5,
                    new int[] {5, 1, 3},
                    new int[] {1, 2, 3},
                    new int[] {3, 4, 5},
                    new int[] {1, 1, 1, 1, 1});
        }

        @Override
        public PathResult shortestPath(String startLocation, String targetLocation) {
            shortestPathCalls++;
            if (startLocation.equals("INVALID")) {
                throw new IllegalArgumentException("Unknown location: " + startLocation);
            }
            return new PathResult(true, 4, new String[] {startLocation, targetLocation});
        }

        @Override
        public MstResult minimumSpanningTree(MstMethod method) {
            return new MstResult(method, true, 10, new String[] {
                    "LOC001 -- LOC002 (4)",
                    "LOC002 -- LOC003 (6)"
            });
        }

        @Override
        public OptimizationResult optimizeRequests(int capacity) {
            return new OptimizationResult(
                    capacity, 9, 6, new String[] {"REQ-001"},
                    10, 8, new String[] {"REQ-002"});
        }

        @Override
        public TestSummary latestTestSummary() {
            return new TestSummary(true, 10, 0, 0, 0, "Latest Maven test summary");
        }

        @Override
        public void runExperiments(int trials) {
            // No external work is required by the scripted UI test double.
        }

        private static RequestView request(String requestId, String status) {
            return new RequestView(
                    requestId,
                    "Patient_" + requestId,
                    "Ward Maintenance",
                    "LOC001",
                    "LOC002",
                    5,
                    status,
                    "08:00",
                    "09:00",
                    2,
                    10);
        }
    }
}
