package ug.edu.ugmc.optimizer.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ug.edu.ugmc.optimizer.database.repository.SqliteOptimizerRepository;

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

        OptimizerService.RequestView dispatched = service.dispatchNextPriority();
        assertEquals(5, dispatched.urgency());
        assertEquals(299, service.viewPendingQueue().size());
    }
}
