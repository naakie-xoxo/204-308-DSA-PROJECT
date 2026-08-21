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

class ConsoleUITest {

    private static final String MAIN_MENU_HEADING =
            "=== UGMC Smart Service Operations Optimizer ===";

    @Test
    void requestLookupStaysInsideFeatureUntilBackAndCanRunTwice() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "1\nREQ-001\n\nREQ-012\nB\n0\n");

        assertEquals(2, service.lookupCalls);
        assertTrue(output.contains("Record found - ID: REQ-001"));
        assertTrue(output.contains("Record found - ID: REQ-012"));
        assertTrue(output.contains("Press ENTER to look up another request"));
        assertEquals(2, countOccurrences(output, MAIN_MENU_HEADING));

        int firstResult = output.indexOf("Record found - ID: REQ-001");
        int secondResult = output.indexOf("Record found - ID: REQ-012");
        assertFalse(output.substring(firstResult, secondResult).contains(MAIN_MENU_HEADING));
        assertTrue(output.contains("Exiting system."));
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
        assertTrue(output.contains("Queue size: 1 / 300"));
        assertTrue(output.contains("Press ENTER to return to the main menu..."));
        assertEquals(1, countOccurrences(output, MAIN_MENU_HEADING));
    }

    @Test
    void enterReturnsFromOneShotQueueViewBeforeCleanExit() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "2\n\n0\n");

        assertEquals(1, service.queueViewCalls);
        assertTrue(output.contains("Queue size: 1 / 300"));
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
        private boolean failQueueView;

        @Override
        public InitializationResult initializeData() {
            return new InitializationResult(300, 300, 50);
        }

        @Override
        public RequestView findRequest(String requestId) {
            lookupCalls++;
            return new RequestView(requestId, 5, 2, 10);
        }

        @Override
        public QueueView viewPendingQueue() {
            queueViewCalls++;
            if (failQueueView) {
                throw new IllegalStateException("Queue unavailable");
            }
            return new QueueView(1, 300, new RequestView("REQ-001", 5, 2, 10));
        }

        @Override
        public RequestView dispatchNextPriority() {
            return new RequestView("REQ-001", 5, 2, 10);
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
            return new SortResult(method, 3, new int[] {1, 3, 5});
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
            return new OptimizationResult(9, 10, capacity, new String[] {"REQ-001"});
        }

        @Override
        public TestSummary latestTestSummary() {
            return new TestSummary(true, 10, 0, 0, 0, "Latest Maven test summary");
        }

        @Override
        public void runExperiments(int trials) {
            // No external work is required by the scripted UI test double.
        }
    }
}
