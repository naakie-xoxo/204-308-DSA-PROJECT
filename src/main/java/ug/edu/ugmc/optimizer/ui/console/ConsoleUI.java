package ug.edu.ugmc.optimizer.ui.console;

import java.util.InputMismatchException;
import java.util.Scanner;

import ug.edu.ugmc.optimizer.algorithms.graph.GraphTraversal;
import ug.edu.ugmc.optimizer.algorithms.graph.PathFinder;
import ug.edu.ugmc.optimizer.datastructures.hashing.CustomHashTable;
import ug.edu.ugmc.optimizer.datastructures.linear.DynamicArray;
import ug.edu.ugmc.optimizer.datastructures.queues.CircularQueue;
import ug.edu.ugmc.optimizer.graph.CustomGraph;
import ug.edu.ugmc.optimizer.models.ServiceRequest;

/**
 * Examiner-facing console menu, input validation, and result formatting.
 *
 * Sprint 3 - Papa Kwame (L200), Universal Parameter 22271087.
 * Invalid-input retry limit formula: (22271087 % 5) + 3 = 5 attempts.
 *
 * NOTE on architecture: the README's dependency flow is
 *   Console UI -> Application services -> Custom structures and algorithms
 * This class currently references Julyn's structures (CustomHashTable,
 * CircularQueue, CustomGraph) and Group B's algorithms (GraphTraversal,
 * PathFinder) directly, since the application/services/ layer is still
 * empty. Once that layer exists, swap these direct references for calls
 * into that service layer instead - the UI is not supposed to touch the
 * structures or the database directly per the architecture rule in the
 * README.
 */
public class ConsoleUI {

    // (22271087 % 5) + 3
    private static final int MAX_INVALID_ATTEMPTS = (22271087 % 5) + 3;

    private final Scanner scanner;

    private final CustomHashTable<String, ServiceRequest> patientLookup;
    private final CircularQueue<ServiceRequest> triageQueue;
    private final CustomGraph hospitalGraph;

    public ConsoleUI(CustomHashTable<String, ServiceRequest> patientLookup,
                      CircularQueue<ServiceRequest> triageQueue,
                      CustomGraph hospitalGraph) {
        this.scanner = new Scanner(System.in);
        this.patientLookup = patientLookup;
        this.triageQueue = triageQueue;
        this.hospitalGraph = hospitalGraph;
    }

    public void run() {
        int invalidAttempts = 0;
        boolean running = true;

        while (running) {
            printMenu();

            try {
                int choice = scanner.nextInt();
                invalidAttempts = 0; // reset on any successfully parsed input

                switch (choice) {
                    case 1:
                        handlePatientLookup();
                        break;
                    case 2:
                        handleTriageQueueView();
                        break;
                    case 3:
                        handleGraphTraversal();
                        break;
                    case 4:
                        handleMstCalculation();
                        break;
                    case 0:
                        running = false;
                        System.out.println("Exiting system.");
                        break;
                    default:
                        System.out.println("Invalid option. Please choose a number from the menu.");
                }
            } catch (InputMismatchException e) {
                scanner.next(); // discard the bad token so we don't loop forever
                invalidAttempts++;
                System.out.println("Invalid input. Attempt " + invalidAttempts
                        + " of " + MAX_INVALID_ATTEMPTS + ".");

                if (invalidAttempts >= MAX_INVALID_ATTEMPTS) {
                    System.out.println("Too many invalid attempts. Terminating system.");
                    running = false;
                }
            }
        }

        scanner.close();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== UGMC Smart Service Operations Optimizer ===");
        System.out.println("1. Patient ID Lookup (CustomHashTable)");
        System.out.println("2. View Pending Triage Queue (CircularQueue)");
        System.out.println("3. Run Graph Traversal (BFS/DFS)");
        System.out.println("4. Run MST Calculation (Prim/Kruskal)");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    /**
     * Uses Julyn's CustomHashTable for O(1) patient ID lookups.
     * Signature confirmed from main:
     *   ServiceRequest get(String key)
     */
    private void handlePatientLookup() {
        System.out.print("Enter patient/request ID: ");
        String requestId = scanner.next();

        ServiceRequest result = patientLookup.get(requestId);
        if (result == null) {
            System.out.println("No record found for ID: " + requestId);
        } else {
            System.out.println("Record found - ID: " + result.getId()
                    + ", Urgency: " + result.getUrgency()
                    + ", Weight: " + result.getWeight()
                    + ", Value: " + result.getValue());
        }
    }

    /**
     * Uses Julyn's CircularQueue for pending triage requests.
     * Signature confirmed from main:
     *   ServiceRequest peek(), boolean isEmpty(), int size(), int getCapacity()
     *
     * NOTE: peek() only, not dequeue() - the console menu should not be
     * silently consuming pending triage requests just to display them.
     */
    private void handleTriageQueueView() {
        if (triageQueue.isEmpty()) {
            System.out.println("Triage queue is empty.");
            return;
        }

        ServiceRequest next = triageQueue.peek();
        System.out.println("Queue size: " + triageQueue.size()
                + " / " + triageQueue.getCapacity());
        System.out.println("Next in line - ID: " + next.getId()
                + ", Urgency: " + next.getUrgency());
    }

    /**
     * Uses GraphTraversal.bfs/dfs (merged to main via PR #31).
     * Both return a DynamicArray<String> of location IDs in discovery order,
     * bounded by GraphTraversal's internal MAX_TRAVERSAL_DEPTH.
     */
    private void handleGraphTraversal() {
        System.out.print("Enter starting location ID: ");
        String startNode = scanner.next();

        System.out.print("Traversal type - B for BFS, D for DFS: ");
        String type = scanner.next().trim();

        try {
            DynamicArray<String> result;
            if (type.equalsIgnoreCase("B")) {
                result = GraphTraversal.bfs(hospitalGraph, startNode);
                System.out.println("BFS order from " + startNode + ":");
            } else if (type.equalsIgnoreCase("D")) {
                result = GraphTraversal.dfs(hospitalGraph, startNode);
                System.out.println("DFS order from " + startNode + ":");
            } else {
                System.out.println("Unrecognized traversal type. Choose B or D.");
                return;
            }

            for (int i = 0; i < result.size(); i++) {
                System.out.print(result.get(i));
                if (i < result.size() - 1) {
                    System.out.print(" -> ");
                }
            }
            System.out.println();
        } catch (IllegalArgumentException e) {
            System.out.println("Traversal failed: " + e.getMessage());
        }
    }

    /**
     * Uses PathFinder.primMSTCost/kruskalMSTCost (merged to main via PR #21).
     * Both return the total MST cost as an int, or -1 if the graph is
     * disconnected.
     */
    private void handleMstCalculation() {
        System.out.print("MST algorithm - P for Prim, K for Kruskal: ");
        String algo = scanner.next().trim();

        try {
            int cost;
            if (algo.equalsIgnoreCase("P")) {
                cost = PathFinder.primMSTCost(hospitalGraph);
                System.out.print("Prim's MST cost: ");
            } else if (algo.equalsIgnoreCase("K")) {
                cost = PathFinder.kruskalMSTCost(hospitalGraph);
                System.out.print("Kruskal's MST cost: ");
            } else {
                System.out.println("Unrecognized algorithm. Choose P or K.");
                return;
            }

            if (cost == -1) {
                System.out.println("Graph is disconnected - no spanning tree exists.");
            } else {
                System.out.println(cost);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("MST calculation failed: " + e.getMessage());
        }
    }
}
