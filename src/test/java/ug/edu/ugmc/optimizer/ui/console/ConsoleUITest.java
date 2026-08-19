package ug.edu.ugmc.optimizer.ui.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ug.edu.ugmc.optimizer.application.services.OptimizerService;

class ConsoleUITest {

    @Test
    void validChoiceRoutesARequestLookupAndThenExitsCleanly() {
        FakeOptimizerService service = new FakeOptimizerService();

        String output = runScript(service, "1\nREQ-001\n0\n");

        assertEquals(1, service.lookupCalls);
        assertTrue(output.contains("Record found - ID: REQ-001"));
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
    void fiveMixedInvalidChoicesTerminateTheMenu() {
        String output = runScript(
                new FakeOptimizerService(), "bad\n99\nbad\n99\nbad\n");

        assertTrue(output.contains("Invalid input. Attempt 5 of 5."));
        assertTrue(output.contains("Too many invalid attempts. Terminating system."));
    }

    @Test
    void mstOperationPrintsEverySelectedEdgeAndTheTotalCost() {
        String output = runScript(new FakeOptimizerService(), "4\nP\n0\n");

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

    private static String runScript(FakeOptimizerService service, String script) {
        ByteArrayInputStream input = new ByteArrayInputStream(
                script.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);

        new ConsoleUI(service, input, output).run();

        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static final class FakeOptimizerService implements OptimizerService {
        private int lookupCalls;

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
            return new QueueView(1, 300, new RequestView("REQ-001", 5, 2, 10));
        }

        @Override
        public RequestView dispatchNextPriority() {
            return new RequestView("REQ-001", 5, 2, 10);
        }

        @Override
        public TraversalResult traverse(String startLocation, TraversalMethod method) {
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
