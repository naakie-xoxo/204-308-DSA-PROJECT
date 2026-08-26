package ug.edu.ugmc.optimizer.application.ports;

import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;
import ug.edu.ugmc.optimizer.models.ServiceRequest.Status;

/**
 * Database operations required by the application service during startup and
 * reload. Concrete persistence details remain outside the UI and service layer.
 */
public interface OptimizerRepository {

    /** Creates or safely updates the persistent database from canonical data. */
    void initialize() throws Exception;

    /** @return the number of service requests available for loading */
    int countServiceRequests() throws Exception;

    /** @return the number of requests currently in the supplied lifecycle state */
    default int countServiceRequestsByStatus(Status status) throws Exception {
        return status == Status.PENDING ? countServiceRequests() : 0;
    }

    /** @return the number of graph locations available for loading */
    int countLocations() throws Exception;

    /** Compatibility loading contract retained for existing repository adapters. */
    void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> lookup,
            CircularQueue<ServiceRequest> pendingQueue) throws Exception;

    /** Loads all records for lookup and only pending records for processing. */
    default void loadServiceRequests(
            CustomHashTable<String, ServiceRequest> lookup,
            CircularQueue<ServiceRequest> pendingQueue,
            DynamicArray<ServiceRequest> allRequests) throws Exception {
        loadServiceRequests(lookup, pendingQueue);
        int pendingCount = pendingQueue.size();
        for (int index = 0; index < pendingCount; index++) {
            ServiceRequest request = pendingQueue.dequeue();
            pendingQueue.enqueue(request);
            allRequests.insert(request);
        }
    }

    /**
     * Atomically applies one valid request-state transition.
     *
     * @return true only when the stored row matched the expected state
     */
    default boolean updateRequestStatus(
            String requestId, Status expectedStatus, Status newStatus) throws Exception {
        throw new UnsupportedOperationException(
                "This repository adapter does not support persistent request status updates.");
    }

    /** Loads all locations and roads into the supplied project-owned graph. */
    void loadGraph(CustomGraph graph) throws Exception;
}
