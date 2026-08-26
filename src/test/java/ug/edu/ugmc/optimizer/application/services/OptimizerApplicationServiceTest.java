package ug.edu.ugmc.optimizer.application.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.DriverManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ug.edu.ugmc.optimizer.application.ports.OptimizerRepository;
import ug.edu.ugmc.optimizer.application.services.OptimizerService.CompletionOutcome;
import ug.edu.ugmc.optimizer.database.repository.SqliteOptimizerRepository;
import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;
import ug.edu.ugmc.optimizer.models.ServiceRequest.Status;

class OptimizerApplicationServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void uiLoadingFlowAllocatesEnoughQueueCapacityForAllThreeHundredRequests()
            throws Exception {
        Path database = temporaryDirectory.resolve("ui-integration.db");
        SqliteOptimizerRepository repository = new SqliteOptimizerRepository(
                database, Path.of("schema.sql"), Path.of("data"));
        OptimizerApplicationService service = new OptimizerApplicationService(
                repository, trials -> { }, temporaryDirectory.resolve("reports"));

        OptimizerService.InitializationResult result = service.initializeData();

        assertEquals(300, result.requestCount());
        assertEquals(300, result.queueCapacity());
        assertEquals(300, service.viewPendingQueue().size());
        assertEquals(300, service.viewPendingQueue().capacity());
        assertNotNull(service.findRequest("REQ-300"));
        assertEquals("REQ-001", service.viewPendingQueue().nextRequest().id());

        OptimizerService.RequestView dispatched = service.dispatchNextPriority();
        assertEquals(5, dispatched.urgency());
        assertEquals("In Progress", dispatched.status());
        assertEquals(299, service.viewPendingQueue().size());
        assertEquals("In Progress", storedStatus(database, dispatched.id()));
        String expectedFifoHead = dispatched.id().equals("REQ-001") ? "REQ-002" : "REQ-001";
        assertEquals(expectedFifoHead, service.viewPendingQueue().nextRequest().id());

        OptimizerService.InitializationResult reloaded = service.initializeData();
        assertEquals(299, reloaded.pendingCount());
        assertEquals(1, reloaded.inProgressCount());
        assertEquals(expectedFifoHead, service.viewPendingQueue().nextRequest().id());

        OptimizerService.RequestView secondDispatch = service.dispatchNextPriority();
        assertEquals(5, secondDispatch.urgency());
        assertNotEquals(dispatched.id(), secondDispatch.id());
        assertEquals(298, service.viewPendingQueue().size());
    }

    @Test
    void lookupReturnsTheCompletePersistentRecordAndNormalizesRequestId() throws Exception {
        OptimizerApplicationService service = createService("lookup.db");
        service.initializeData();

        OptimizerService.RequestView request = service.findRequest("  req-001  ");

        assertEquals("REQ-001", request.id());
        assertEquals("Patient_REQ-001", request.patientLabel());
        assertEquals("Ward Maintenance", request.category());
        assertEquals("LOC041", request.sourceLocation());
        assertEquals("LOC037", request.destinationLocation());
        assertEquals(2, request.urgency());
        assertEquals("Pending", request.status());
        assertEquals("05:36", request.submitted());
        assertEquals("06:56", request.deadline());
        assertEquals(42, request.weight());
        assertEquals(89, request.value());
    }

    @Test
    void dispatchAndCompletionPersistAcrossIndependentServiceReloads() throws Exception {
        OptimizerApplicationService firstService = createService("lifecycle.db");
        firstService.initializeData();

        OptimizerService.RequestView dispatched = firstService.dispatchNextPriority();
        assertEquals("In Progress", dispatched.status());
        assertEquals(299, firstService.viewPendingQueue().size());

        OptimizerApplicationService reloadedService = createService("lifecycle.db");
        OptimizerService.InitializationResult reloaded = reloadedService.initializeData();
        assertEquals(299, reloaded.pendingCount());
        assertEquals(1, reloaded.inProgressCount());
        assertEquals(0, reloaded.completedCount());
        assertEquals("In Progress", reloadedService.findRequest(dispatched.id()).status());

        String stillPendingId = dispatched.id().equals("REQ-001") ? "REQ-002" : "REQ-001";
        OptimizerService.CompletionResult rejected =
                reloadedService.completeRequest(stillPendingId);
        assertEquals(CompletionOutcome.MUST_DISPATCH_FIRST, rejected.outcome());

        OptimizerService.CompletionResult completed =
                reloadedService.completeRequest(dispatched.id());
        assertEquals(CompletionOutcome.COMPLETED, completed.outcome());
        assertEquals("Completed", completed.request().status());
        assertEquals("Completed", storedStatus(
                temporaryDirectory.resolve("lifecycle.db"), dispatched.id()));

        OptimizerService.CompletionResult repeated =
                reloadedService.completeRequest(dispatched.id());
        assertEquals(CompletionOutcome.ALREADY_COMPLETED, repeated.outcome());

        OptimizerApplicationService finalService = createService("lifecycle.db");
        OptimizerService.InitializationResult finalState = finalService.initializeData();
        assertEquals(299, finalState.pendingCount());
        assertEquals(0, finalState.inProgressCount());
        assertEquals(1, finalState.completedCount());
        assertEquals("Completed", finalService.findRequest(dispatched.id()).status());
        assertNotEquals(dispatched.id(), finalService.dispatchNextPriority().id());
    }

    @Test
    void allSortMethodsReportRealBeforeDataDistributionAndBothUrgencyExtremes()
            throws Exception {
        OptimizerApplicationService service = createService("sorts.db");
        service.initializeData();
        int[] expectedBefore = {2, 1, 2, 3, 1, 3, 4, 3, 4, 5};

        for (OptimizerService.SortMethod method : OptimizerService.SortMethod.values()) {
            OptimizerService.SortResult result = service.sortUrgencies(method);
            assertEquals(300, result.totalCount());
            assertArrayEquals(expectedBefore, result.beforeSample());
            assertEquals(1, result.lowestSample()[0]);
            assertEquals(5, result.highestSample()[result.highestSample().length - 1]);
            int distributionTotal = 0;
            for (int count : result.distribution()) {
                distributionTotal += count;
            }
            assertEquals(result.totalCount(), distributionTotal);
        }
    }

    @Test
    void optimizationSelectionsMatchDisplayedArithmeticAndCapacity() throws Exception {
        OptimizerApplicationService service = createService("optimization.db");
        service.initializeData();

        OptimizerService.OptimizationResult result = service.optimizeRequests(110);

        int greedyWeight = 0;
        int greedyValue = 0;
        for (String requestId : result.greedySelectedRequestIds()) {
            OptimizerService.RequestView request = service.findRequest(requestId);
            greedyWeight += request.weight();
            greedyValue += request.value();
            assertEquals("Pending", request.status());
        }
        assertEquals(greedyWeight, result.greedyWeight());
        assertEquals(greedyValue, result.greedyValue());
        assertEquals(110 - greedyWeight, result.greedyRemainingCapacity());

        int optimalWeight = 0;
        int optimalValue = 0;
        for (String requestId : result.selectedRequestIds()) {
            OptimizerService.RequestView request = service.findRequest(requestId);
            optimalWeight += request.weight();
            optimalValue += request.value();
            assertEquals("Pending", request.status());
        }
        assertEquals(optimalWeight, result.selectedWeight());
        assertEquals(optimalValue, result.optimalValue());
        assertEquals(110 - optimalWeight, result.optimalRemainingCapacity());
        assertTrue(result.greedyWeight() <= result.capacity());
        assertTrue(result.selectedWeight() <= result.capacity());
        assertTrue(result.optimalValue() >= result.greedyValue());
    }

    @Test
    void priorityDispatchUsesUrgencyRatherThanWeightOrBenefit() throws Exception {
        InMemoryRequestRepository repository = new InMemoryRequestRepository(new ServiceRequest[] {
                request("REQ-LOW", 1, 1, 10_000, Status.PENDING),
                request("REQ-HIGH", 5, 100, 1, Status.PENDING),
                request("REQ-MID", 3, 2, 5_000, Status.PENDING)
        });
        OptimizerApplicationService service = new OptimizerApplicationService(
                repository, trials -> { }, temporaryDirectory.resolve("reports"));
        service.initializeData();

        assertEquals("REQ-HIGH", service.dispatchNextPriority().id());
        assertEquals("REQ-MID", service.dispatchNextPriority().id());
    }

    @Test
    void interactiveOptimizationSelectsOnlyPendingRequests() throws Exception {
        InMemoryRequestRepository repository = new InMemoryRequestRepository(new ServiceRequest[] {
                request("REQ-COMPLETE", 5, 1, 10_000, Status.COMPLETED),
                request("REQ-PENDING", 1, 10, 10, Status.PENDING),
                request("REQ-ACTIVE", 5, 1, 9_000, Status.IN_PROGRESS)
        });
        OptimizerApplicationService service = new OptimizerApplicationService(
                repository, trials -> { }, temporaryDirectory.resolve("reports"));
        service.initializeData();

        OptimizerService.OptimizationResult result = service.optimizeRequests(100);

        assertArrayEquals(new String[] {"REQ-PENDING"}, result.greedySelectedRequestIds());
        assertArrayEquals(new String[] {"REQ-PENDING"}, result.selectedRequestIds());
        assertEquals(10, result.greedyValue());
        assertEquals(10, result.optimalValue());
    }

    private OptimizerApplicationService createService(String databaseName) {
        SqliteOptimizerRepository repository = new SqliteOptimizerRepository(
                temporaryDirectory.resolve(databaseName), Path.of("schema.sql"), Path.of("data"));
        return new OptimizerApplicationService(
                repository, trials -> { }, temporaryDirectory.resolve("reports"));
    }

    private static String storedStatus(Path database, String requestId) throws Exception {
        try (var connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + database.toAbsolutePath());
                var statement = connection.prepareStatement(
                    "SELECT status FROM service_requests WHERE request_id = ?")) {
            statement.setString(1, requestId);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    private static ServiceRequest request(
            String id, int urgency, int weight, int value, Status status) {
        return new ServiceRequest(
                id, "Patient_" + id, "LOC001", "LOC001", "Test",
                urgency, weight, value, "08:00", "09:00", status);
    }

    private static final class InMemoryRequestRepository implements OptimizerRepository {
        private final ServiceRequest[] requests;

        private InMemoryRequestRepository(ServiceRequest[] requests) {
            this.requests = requests;
        }

        @Override
        public void initialize() {
            // The test fixture is already initialized.
        }

        @Override
        public int countServiceRequests() {
            return requests.length;
        }

        @Override
        public int countServiceRequestsByStatus(Status status) {
            int count = 0;
            for (ServiceRequest request : requests) {
                if (request.getStatus() == status) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int countLocations() {
            return 1;
        }

        @Override
        public void loadServiceRequests(
                CustomHashTable<String, ServiceRequest> lookup,
                CircularQueue<ServiceRequest> pendingQueue) {
            loadServiceRequests(lookup, pendingQueue, new DynamicArray<>(requests.length));
        }

        @Override
        public void loadServiceRequests(
                CustomHashTable<String, ServiceRequest> lookup,
                CircularQueue<ServiceRequest> pendingQueue,
                DynamicArray<ServiceRequest> allRequests) {
            for (ServiceRequest request : requests) {
                lookup.put(request.getId(), request);
                allRequests.insert(request);
                if (request.getStatus() == Status.PENDING) {
                    pendingQueue.enqueue(request);
                }
            }
        }

        @Override
        public boolean updateRequestStatus(
                String requestId, Status expectedStatus, Status newStatus) {
            for (int index = 0; index < requests.length; index++) {
                ServiceRequest request = requests[index];
                if (request.getId().equals(requestId)
                        && request.getStatus() == expectedStatus) {
                    requests[index] = request.withStatus(newStatus);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void loadGraph(CustomGraph graph) {
            graph.addNode("LOC001");
        }
    }
}
