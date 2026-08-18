package ug.edu.ugmc.optimizer.experiments;

import java.nio.file.Path;

import ug.edu.ugmc.optimizer.application.ports.ExperimentGateway;

/** Adapter that exposes the existing performance runner to application services. */
public final class PerformanceExperimentGateway implements ExperimentGateway {

    private final String connectionString;
    private final Path resultsDirectory;
    private final Path algorithmRunsPath;

    public PerformanceExperimentGateway(
            Path databasePath,
            Path resultsDirectory,
            Path algorithmRunsPath) {
        if (databasePath == null || resultsDirectory == null || algorithmRunsPath == null) {
            throw new IllegalArgumentException("Experiment paths are required.");
        }
        this.connectionString = "jdbc:sqlite:" + databasePath.toAbsolutePath().normalize();
        this.resultsDirectory = resultsDirectory;
        this.algorithmRunsPath = algorithmRunsPath;
    }

    @Override
    public void run(int trials) throws Exception {
        PerformanceRunner.fromDatabase(connectionString, trials)
                .run(resultsDirectory, algorithmRunsPath);
    }
}
