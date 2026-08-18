package ug.edu.ugmc.optimizer.application.ports;

import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/**
 * Database operations required by the application service during startup and
 * reload. Concrete persistence details remain outside the UI and service layer.
 */
public interface OptimizerRepository {

    /** Creates or safely updates the persistent database from canonical data. */
    void initialize() throws Exception;

    /** @return the number of service requests available for loading */
    int countServiceRequests() throws Exception;

    /** @return the number of graph locations available for loading */
    int countLocations() throws Exception;

    /** Loads all requests into the supplied project-owned structures. */
    void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> lookup,
            CircularQueue<ServiceRequest> queue) throws Exception;

    /** Loads all locations and roads into the supplied project-owned graph. */
    void loadGraph(CustomGraph graph) throws Exception;
}
