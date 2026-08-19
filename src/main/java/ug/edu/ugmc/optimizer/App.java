package ug.edu.ugmc.optimizer;

import java.nio.file.Path;

import ug.edu.ugmc.optimizer.application.ports.ExperimentGateway;
import ug.edu.ugmc.optimizer.application.ports.OptimizerRepository;
import ug.edu.ugmc.optimizer.application.services.OptimizerApplicationService;
import ug.edu.ugmc.optimizer.application.services.OptimizerService;
import ug.edu.ugmc.optimizer.database.repository.SqliteOptimizerRepository;
import ug.edu.ugmc.optimizer.experiments.PerformanceExperimentGateway;
import ug.edu.ugmc.optimizer.ui.console.ConsoleUI;

/** Composition root for the UGMC Smart Service Operations Optimizer. */
public final class App {
    private static final Path DATABASE_PATH = Path.of("hospital_system.db");
    private static final Path SCHEMA_PATH = Path.of("schema.sql");
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path RESULTS_DIRECTORY = Path.of("results");
    private static final Path ALGORITHM_RUNS_PATH =
            DATA_DIRECTORY.resolve("algorithm_runs.csv");
    private static final Path TEST_REPORT_DIRECTORY =
            Path.of("target", "surefire-reports");

    private App() {
    }

    public static void main(String[] args) {
        ConsoleUI consoleUI;
        try {
            OptimizerRepository repository = new SqliteOptimizerRepository(
                    DATABASE_PATH, SCHEMA_PATH, DATA_DIRECTORY);
            ExperimentGateway experimentGateway = new PerformanceExperimentGateway(
                    DATABASE_PATH, RESULTS_DIRECTORY, ALGORITHM_RUNS_PATH);
            OptimizerService service = new OptimizerApplicationService(
                    repository, experimentGateway, TEST_REPORT_DIRECTORY);

            service.initializeData();
            consoleUI = new ConsoleUI(service);
        } catch (Exception exception) {
            System.err.println("Application startup failed: " + safeMessage(exception));
            return;
        }

        consoleUI.run();
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
