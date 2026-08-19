package ug.edu.ugmc.optimizer.application.ports;

/** Application-facing gateway to the existing empirical experiment runner. */
@FunctionalInterface
public interface ExperimentGateway {

    /** Runs and exports the existing performance experiments. */
    void run(int trials) throws Exception;
}
